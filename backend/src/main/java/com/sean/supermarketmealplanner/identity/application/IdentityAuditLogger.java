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
    public void success(String event,UUID userId){LOG.info("event={} userId={}",event,userId);}
    public void failure(String event,String identifier){LOG.warn("event={} subject={}",event,fingerprint(identifier));}
    public void denied(String path){LOG.warn("event=access_denied path={}",path);}
    private String fingerprint(String value){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)),0,12);}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
}
