package com.sean.supermarketmealplanner.identity.infrastructure.security;

import com.sean.supermarketmealplanner.identity.application.AuthPrincipal;
import com.sean.supermarketmealplanner.identity.application.AuthProperties;
import com.sean.supermarketmealplanner.identity.domain.UserStatus;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.RefreshTokenSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AccessTokenAuthenticationFilter extends OncePerRequestFilter {
    private final RefreshTokenSessionRepository sessions; private final Clock clock; private final JwtDecoder decoder;
    public AccessTokenAuthenticationFilter(AuthProperties properties,RefreshTokenSessionRepository sessions,Clock clock){
        this.sessions=sessions;this.clock=clock;
        var key=new SecretKeySpec(properties.accessTokenSecret().getBytes(StandardCharsets.UTF_8),"HmacSHA256");
        var nimbus=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        nimbus.setJwtValidator(org.springframework.security.oauth2.jwt.JwtValidators
                .createDefaultWithIssuer(properties.issuer()));
        this.decoder=nimbus;
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,
                                              FilterChain chain)throws ServletException,IOException{
        var raw=cookie(request,AuthCookieService.ACCESS);
        if(raw!=null){
            try{
                var jwt=decoder.decode(raw);var userId=UUID.fromString(jwt.getSubject());
                var sessionId=UUID.fromString(jwt.getClaimAsString("sid"));
                sessions.findWithUserById(sessionId).filter(s->s.getUser().getId().equals(userId))
                        .filter(s->s.isUsableAt(OffsetDateTime.now(clock)))
                        .filter(s->s.getUser().getStatus()==UserStatus.ACTIVE).ifPresent(s->{
                            var principal=new AuthPrincipal(userId,sessionId,s.getUser().getRole());
                            SecurityContextHolder.getContext().setAuthentication(
                                    new UsernamePasswordAuthenticationToken(principal,null,
                                            List.of(new SimpleGrantedAuthority("ROLE_"+s.getUser().getRole().name()))));
                        });
            }catch(RuntimeException ignored){ request.setAttribute("expired-or-invalid-session",Boolean.TRUE); }
        }
        chain.doFilter(request,response);
    }
    public static String cookie(HttpServletRequest request,String name){
        if(request.getCookies()==null)return null;
        return Arrays.stream(request.getCookies()).filter(c->name.equals(c.getName()))
                .map(Cookie::getValue).findFirst().orElse(null);
    }
}
