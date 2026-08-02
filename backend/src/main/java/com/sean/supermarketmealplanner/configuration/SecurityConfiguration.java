package com.sean.supermarketmealplanner.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.supermarketmealplanner.identity.application.AuthProperties;
import com.sean.supermarketmealplanner.identity.infrastructure.security.AccessTokenAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder(AuthProperties p){
        java.security.Security.addProvider(new BouncyCastleProvider());
        return new Argon2PasswordEncoder(p.argon2().saltLength(),p.argon2().hashLength(),
                p.argon2().parallelism(),p.argon2().memoryKb(),p.argon2().iterations());
    }
    @Bean SecurityFilterChain security(HttpSecurity http,AuthProperties p,
                                       AccessTokenAuthenticationFilter filter,ObjectMapper json)throws Exception{
        var csrf=CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookiePath("/");
        csrf.setCookieCustomizer(cookie->cookie.secure(p.cookie().secure()).sameSite(p.cookie().sameSite()));
        http.sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(c->{}).csrf(c->c
                        .csrfTokenRepository(csrf)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .authorizeHttpRequests(a->{
                    a.requestMatchers("/api/v1/auth/csrf","/api/v1/auth/register","/api/v1/auth/login",
                            "/api/v1/auth/refresh","/api/v1/auth/logout","/actuator/health",
                            "/api/v1/supermarkets/**","/api/v1/products/**","/api/v1/catalog/**",
                            "/api/v1/categories/**","/api/v1/dietary-tags/**","/api/v1/allergens/**").permitAll();
                    a.requestMatchers(org.springframework.http.HttpMethod.GET,"/api/v1/meal-templates/**").permitAll();
                    a.requestMatchers("/v3/api-docs/**","/swagger-ui/**").access((auth,ctx)->
                            new org.springframework.security.authorization.AuthorizationDecision(p.swaggerEnabled()));
                    a.requestMatchers("/api/v1/meal-templates/**").hasRole("ADMIN");
                    a.requestMatchers("/api/v1/admin/**").hasRole("ADMIN");
                    a.anyRequest().authenticated();
                })
                .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->
                        problem(json,res,401,"AUTHENTICATION_REQUIRED","Debes iniciar sesión"))
                    .accessDeniedHandler((req,res,ex)->problem(json,res,403,
                            ex instanceof CsrfException ? "CSRF_TOKEN_INVALID":"ACCESS_DENIED",
                            ex instanceof CsrfException ? "Token CSRF ausente o inválido":"Acceso denegado")))
                .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean CorsConfigurationSource corsConfigurationSource(AuthProperties p){
        var c=new CorsConfiguration();c.setAllowedOrigins(p.allowedOrigins());
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Content-Type","X-XSRF-TOKEN"));c.setAllowCredentials(true);
        var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",c);return source;
    }
    private static void problem(ObjectMapper json,HttpServletResponse response,int status,String code,String detail)
            throws java.io.IOException{
        response.setStatus(status);response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control","no-store");
        json.writeValue(response.getOutputStream(),java.util.Map.of("type","about:blank","title",detail,
                "status",status,"detail",detail,"code",code));
    }
}
