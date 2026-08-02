package com.sean.supermarketmealplanner.identity.infrastructure.web;

import static com.sean.supermarketmealplanner.identity.application.IdentityDtos.*;

import com.sean.supermarketmealplanner.identity.application.AuthProperties;
import com.sean.supermarketmealplanner.identity.application.CurrentUserProvider;
import com.sean.supermarketmealplanner.identity.application.IdentityService;
import com.sean.supermarketmealplanner.identity.application.InMemoryRateLimiter;
import com.sean.supermarketmealplanner.identity.application.UserDataExportService;
import com.sean.supermarketmealplanner.identity.infrastructure.security.AuthCookieService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final IdentityService service;private final CurrentUserProvider current;private final AuthCookieService cookies;
    private final InMemoryRateLimiter limits;private final AuthProperties properties;
    private final UserDataExportService exportService;
    public UserController(IdentityService service,CurrentUserProvider current,AuthCookieService cookies,
                          InMemoryRateLimiter limits,AuthProperties properties,UserDataExportService exportService){
        this.service=service;this.current=current;this.cookies=cookies;this.limits=limits;this.properties=properties;
        this.exportService=exportService;
    }
    @PatchMapping public ResponseEntity<UserResponse> update(@Valid @RequestBody UpdateProfileRequest request){
        return noStore(service.update(current.userId(),request));}
    @GetMapping("/preferences") public ResponseEntity<PreferencesResponse> preferences(){
        return noStore(service.me(current.userId()).preferences());}
    @PutMapping("/preferences") public ResponseEntity<PreferencesResponse> preferences(
            @Valid @RequestBody PreferencesRequest request){
        return noStore(service.updatePreferences(current.userId(),request).preferences());}
    @PostMapping("/change-password") public ResponseEntity<Void> password(
            @Valid @RequestBody ChangePasswordRequest request,HttpServletResponse response){
        limits.check("password",current.userId().toString(),properties.rateLimits().passwordChangePerHour(),
                Duration.ofHours(1));service.changePassword(current.userId(),request);cookies.clear(response);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();}
    @DeleteMapping public ResponseEntity<Void> disable(@Valid @RequestBody DisableAccountRequest request,
                                                       HttpServletResponse response){
        service.disable(current.userId(),request);cookies.clear(response);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();}
    @GetMapping(value="/export",produces="application/json")
    public ResponseEntity<StreamingResponseBody> export(){
        var userId=current.userId();limits.check("user-export",userId.toString(),2,Duration.ofHours(1));
        StreamingResponseBody body=output->exportService.write(userId,output);
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.noStore())
                .header("Content-Disposition","attachment; filename=supermarket-meal-planner-export.json")
                .body(body);
    }
    private static <T> ResponseEntity<T> noStore(T body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
}
