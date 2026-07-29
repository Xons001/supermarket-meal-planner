package com.sean.supermarketmealplanner.identity.infrastructure.web;

import static com.sean.supermarketmealplanner.identity.application.IdentityDtos.*;

import com.sean.supermarketmealplanner.identity.application.AuthProperties;
import com.sean.supermarketmealplanner.identity.application.CurrentUserProvider;
import com.sean.supermarketmealplanner.identity.application.IdentityService;
import com.sean.supermarketmealplanner.identity.application.InMemoryRateLimiter;
import com.sean.supermarketmealplanner.identity.application.SessionService;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.UserAccountEntity;
import com.sean.supermarketmealplanner.identity.infrastructure.security.AccessTokenAuthenticationFilter;
import com.sean.supermarketmealplanner.identity.infrastructure.security.AuthCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final IdentityService identity; private final SessionService sessions; private final AuthCookieService cookies;
    private final CurrentUserProvider current; private final InMemoryRateLimiter limits; private final AuthProperties properties;
    private final com.sean.supermarketmealplanner.identity.application.IdentityAuditLogger audit;
    public AuthController(IdentityService identity,SessionService sessions,AuthCookieService cookies,
                          CurrentUserProvider current,InMemoryRateLimiter limits,AuthProperties properties,
                          com.sean.supermarketmealplanner.identity.application.IdentityAuditLogger audit){
        this.identity=identity;this.sessions=sessions;this.cookies=cookies;this.current=current;
        this.limits=limits;this.properties=properties;
        this.audit=audit;
    }
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token){token.getToken();return noStore().build();}
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest body,
                                                  HttpServletRequest request,HttpServletResponse response){
        limits.check("register",ip(request),properties.rateLimits().registerPerHour(),Duration.ofHours(1));
        var result=identity.register(body,request.getHeader("User-Agent"));cookies.write(response,result.tokens());
        return noStore(result.user());
    }
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest body,
                                               HttpServletRequest request,HttpServletResponse response){
        limits.check("login",ip(request)+"|"+UserAccountEntity.normalizeEmail(body.email()),
                properties.rateLimits().loginPerMinute(),Duration.ofMinutes(1));
        var result=identity.login(body,request.getHeader("User-Agent"));cookies.write(response,result.tokens());
        return noStore(result.user());
    }
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request,HttpServletResponse response){
        var raw=AccessTokenAuthenticationFilter.cookie(request,AuthCookieService.REFRESH);
        limits.check("refresh",ip(request)+"|"+(raw==null?"missing":sessions.hash(raw)),
                properties.rateLimits().refreshPerMinute(),Duration.ofMinutes(1));
        cookies.write(response,sessions.rotate(raw,request.getHeader("User-Agent")));return noStore().build();
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,HttpServletResponse response){
        sessions.revoke(AccessTokenAuthenticationFilter.cookie(request,AuthCookieService.REFRESH));
        var authentication=org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication();
        if(authentication!=null && authentication.getPrincipal()
                instanceof com.sean.supermarketmealplanner.identity.application.AuthPrincipal principal)
            audit.success("logout",principal.userId());
        cookies.clear(response);return noStore().build();
    }
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(HttpServletResponse response){
        var userId=current.userId();sessions.revokeAll(userId);audit.success("logout_all",userId);
        cookies.clear(response);return noStore().build();
    }
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(){return noStore(identity.me(current.userId()));}
    private String ip(HttpServletRequest request){return request.getRemoteAddr();}
    private static ResponseEntity.BodyBuilder noStore(){return ResponseEntity.ok().cacheControl(CacheControl.noStore());}
    private static <T> ResponseEntity<T> noStore(T body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
}
