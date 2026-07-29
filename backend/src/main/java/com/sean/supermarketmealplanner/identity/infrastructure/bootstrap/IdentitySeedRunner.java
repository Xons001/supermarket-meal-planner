package com.sean.supermarketmealplanner.identity.infrastructure.bootstrap;

import com.sean.supermarketmealplanner.identity.application.PasswordPolicy;
import com.sean.supermarketmealplanner.identity.domain.UserRole;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.UserAccountEntity;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.UserAccountRepository;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.UserPreferencesEntity;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.UserPreferencesRepository;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IdentitySeedRunner implements ApplicationRunner {
    private static final java.util.UUID TECHNICAL_OWNER =
            java.util.UUID.fromString("00000000-0000-4000-8000-000000000007");
    private final boolean demoEnabled; private final String demoEmail; private final String demoPassword;
    private final boolean adminEnabled; private final String adminEmail; private final String adminPassword;
    private final UserAccountRepository users; private final UserPreferencesRepository preferences;
    private final PasswordEncoder encoder; private final PasswordPolicy policy; private final JdbcTemplate jdbc;
    public IdentitySeedRunner(@Value("${app.demo.enabled:false}") boolean demoEnabled,
            @Value("${app.demo.email:}") String demoEmail,@Value("${app.demo.password:}") String demoPassword,
            @Value("${app.admin.enabled:false}") boolean adminEnabled,
            @Value("${app.admin.email:}") String adminEmail,@Value("${app.admin.password:}") String adminPassword,
            UserAccountRepository users,UserPreferencesRepository preferences,PasswordEncoder encoder,
            PasswordPolicy policy,JdbcTemplate jdbc){
        this.demoEnabled=demoEnabled;this.demoEmail=demoEmail;this.demoPassword=demoPassword;
        this.adminEnabled=adminEnabled;this.adminEmail=adminEmail;this.adminPassword=adminPassword;
        this.users=users;this.preferences=preferences;this.encoder=encoder;this.policy=policy;this.jdbc=jdbc;
    }
    @Override @Transactional public void run(ApplicationArguments args){
        if(demoEnabled){var demo=seed(demoEmail,demoPassword,"Cuenta demo",UserRole.USER);
            jdbc.execute("SET CONSTRAINTS fk_shopping_list_plan_owner DEFERRED");
            jdbc.update("update meal_plans set owner_id=? where owner_id=?",demo.getId(),TECHNICAL_OWNER);
            jdbc.update("update shopping_lists set owner_id=? where owner_id=?",demo.getId(),TECHNICAL_OWNER);}
        if(adminEnabled)seed(adminEmail,adminPassword,"Administrador",UserRole.ADMIN);
    }
    private UserAccountEntity seed(String email,String password,String name,UserRole role){
        if(email==null||email.isBlank()||password==null||password.isBlank())
            throw new IllegalStateException(role+" seed requires external email and password");
        var normalized=UserAccountEntity.normalizeEmail(email);
        return users.findByNormalizedEmail(normalized).orElseGet(()->{
            policy.validate(password);var now=OffsetDateTime.now();
            var user=users.save(new UserAccountEntity(normalized,encoder.encode(password),name,role,now));
            preferences.save(new UserPreferencesEntity(user,now));return user;
        });
    }
}
