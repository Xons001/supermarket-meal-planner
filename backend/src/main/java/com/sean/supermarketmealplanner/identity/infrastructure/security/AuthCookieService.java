package com.sean.supermarketmealplanner.identity.infrastructure.security;

import com.sean.supermarketmealplanner.identity.application.AuthProperties;
import com.sean.supermarketmealplanner.identity.application.SessionTokens;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieService {
    public static final String ACCESS="SMP_ACCESS", REFRESH="SMP_REFRESH";
    private final AuthProperties properties;
    public AuthCookieService(AuthProperties properties){this.properties=properties;}
    public void write(HttpServletResponse response, SessionTokens tokens){
        add(response,ACCESS,tokens.accessToken(),"/",properties.accessTtl(),true);
        add(response,REFRESH,tokens.refreshToken(),"/api/v1/auth",properties.refreshTtl(),true);
    }
    public void clear(HttpServletResponse response){
        add(response,ACCESS,"","/",Duration.ZERO,true);
        add(response,REFRESH,"","/api/v1/auth",Duration.ZERO,true);
    }
    private void add(HttpServletResponse response,String name,String value,String path,Duration age,boolean httpOnly){
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from(name,value).httpOnly(httpOnly)
                .secure(properties.cookie().secure()).sameSite(properties.cookie().sameSite())
                .path(path).maxAge(age).build().toString());
    }
}
