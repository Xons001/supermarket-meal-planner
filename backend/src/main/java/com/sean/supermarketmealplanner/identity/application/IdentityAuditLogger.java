package com.sean.supermarketmealplanner.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IdentityAuditLogger {
    private static final Logger LOG=LoggerFactory.getLogger("identity-audit");
    private final io.micrometer.core.instrument.MeterRegistry metrics;
    public IdentityAuditLogger(io.micrometer.core.instrument.MeterRegistry metrics){this.metrics=metrics;}
    public void success(String event,UUID userId){
        LOG.info("event={} subject={}",event,fingerprint(userId.toString()));
        metrics.counter("authentication.events","event",safeEvent(event),"result","success").increment();
    }
    public void failure(String event,String identifier){
        LOG.warn("event={} subject={}",event,fingerprint(identifier));
        metrics.counter("authentication.events","event",safeEvent(event),"result","failure").increment();
    }
    public void denied(String path){LOG.warn("event=access_denied path={}",path);}
    private String fingerprint(String value){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)),0,12);}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private String safeEvent(String event){
        return switch(event){
            case "registration_succeeded","login_succeeded","login_failed","password_changed",
                    "account_disabled","logout","logout_all","refresh_succeeded","refresh_token_reused",
                    "data_exported" -> event;
            default -> "other";
        };
    }
}
