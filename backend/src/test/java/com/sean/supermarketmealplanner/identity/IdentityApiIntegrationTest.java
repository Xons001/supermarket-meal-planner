package com.sean.supermarketmealplanner.identity;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.supermarketmealplanner.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class IdentityApiIntegrationTest extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void csrfRegistrationCookiesSessionRefreshRotationLogoutAndNoStore() throws Exception {
        mvc.perform(post("/api/v1/auth/register").with(anonymous()).with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "email","session@example.test","password","contraseña segura 123",
                                "displayName","Sesión"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));

        var registered = register("session@example.test");
        org.assertj.core.api.Assertions.assertThat(registered.access().isHttpOnly()).isTrue();
        org.assertj.core.api.Assertions.assertThat(registered.refresh().isHttpOnly()).isTrue();
        org.assertj.core.api.Assertions.assertThat(registered.raw().getResponse().getHeader("Cache-Control"))
                .contains("no-store");
        var access = registered.access();
        var refresh = registered.refresh();

        mvc.perform(get("/api/v1/auth/me").with(anonymous()).cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("session@example.test"));

        var rotated = mvc.perform(post("/api/v1/auth/refresh").with(anonymous()).with(csrf())
                        .cookie(refresh))
                .andExpect(status().isOk()).andReturn();
        var nextRefresh = rotated.getResponse().getCookie("SMP_REFRESH");
        org.assertj.core.api.Assertions.assertThat(nextRefresh).isNotNull();
        org.assertj.core.api.Assertions.assertThat(nextRefresh.getValue()).isNotEqualTo(refresh.getValue());

        mvc.perform(post("/api/v1/auth/refresh").with(anonymous()).with(csrf()).cookie(refresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));

        mvc.perform(post("/api/v1/auth/logout").with(anonymous()).with(csrf()).cookie(nextRefresh))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    void preferencesFillGenerationAndForeignResourcesAreAlwaysNotFound() throws Exception {
        var a = register("owner-a@example.test");
        var b = register("owner-b@example.test");
        var generation = mvc.perform(post("/api/v1/meal-plans/generate").with(anonymous()).with(csrf())
                        .cookie(a.access()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of(
                                "supermarketCode","MERCADONA","name","Plan privado",
                                "startDate", LocalDate.now().plusDays(1).toString(),
                                "servings",1,"deterministicSeed",77,"persist",true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numberOfDays").value(7))
                .andExpect(jsonPath("$.mealsPerDay").value(4))
                .andReturn();
        var planId = json.readTree(generation.getResponse().getContentAsByteArray())
                .path("mealPlanId").asText();
        var plannedMealId = json.readTree(generation.getResponse().getContentAsByteArray())
                .path("days").get(0).path("meals").get(0).path("plannedMealId").asText();
        mvc.perform(get("/api/v1/meal-plans/{id}",planId).with(anonymous()).cookie(a.access()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/meal-plans/{id}",planId).with(anonymous()).cookie(b.access()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/meal-plans/{id}/changes",planId)
                        .with(anonymous()).cookie(b.access()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/meal-plans/{id}/meals/{mealId}/alternatives",planId,plannedMealId)
                        .with(anonymous()).cookie(b.access()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/meal-plans/{id}/shopping-list",planId)
                        .with(anonymous()).with(csrf()).cookie(b.access()))
                .andExpect(status().isNotFound());
    }

    @Test
    void aRegularUserCannotMutateTemplates() throws Exception {
        var user = register("ordinary@example.test");
        mvc.perform(post("/api/v1/meal-templates").with(anonymous()).with(csrf())
                        .cookie(user.access()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void emailIsCaseInsensitiveAndPasswordChangeRevokesTheCurrentSession() throws Exception {
        var user=register("CaseSensitive@example.test");
        mvc.perform(post("/api/v1/auth/register").with(anonymous()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email","casesensitive@EXAMPLE.TEST",
                                "password","otra contraseña 456","displayName","Duplicada"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
        mvc.perform(post("/api/v1/users/me/change-password").with(anonymous()).with(csrf())
                        .cookie(user.access()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("currentPassword","contraseña segura 123",
                                "newPassword","contraseña nueva 456"))))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie",containsString("Max-Age=0")));
        mvc.perform(get("/api/v1/auth/me").with(anonymous()).cookie(user.access()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").with(anonymous()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email","casesensitive@example.test",
                                "password","contraseña nueva 456"))))
                .andExpect(status().isOk());
    }

    private Registration register(String email) throws Exception {
        var result = mvc.perform(post("/api/v1/auth/register").with(anonymous()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email",email,
                                "password","contraseña segura 123","displayName","Persona"))))
                .andExpect(status().isOk()).andReturn();
        return new Registration(result,result.getResponse().getCookie("SMP_ACCESS"),
                result.getResponse().getCookie("SMP_REFRESH"));
    }
    private record Registration(MvcResult raw,Cookie access,Cookie refresh) {}
}
