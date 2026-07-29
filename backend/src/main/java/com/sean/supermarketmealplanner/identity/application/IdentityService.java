package com.sean.supermarketmealplanner.identity.application;

import static com.sean.supermarketmealplanner.identity.application.IdentityDtos.*;

import com.sean.supermarketmealplanner.identity.domain.UserRole;
import com.sean.supermarketmealplanner.identity.domain.UserStatus;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.UserAccountEntity;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.UserAccountRepository;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.UserPreferencesEntity;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.UserPreferencesRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {
    private final UserAccountRepository users; private final UserPreferencesRepository preferences;
    private final PasswordEncoder passwords; private final PasswordPolicy policy; private final SessionService sessions;
    private final Clock clock;
    private final IdentityAuditLogger audit;
    public IdentityService(UserAccountRepository users, UserPreferencesRepository preferences,
                           PasswordEncoder passwords, PasswordPolicy policy, SessionService sessions, Clock clock,
                           IdentityAuditLogger audit) {
        this.users=users; this.preferences=preferences; this.passwords=passwords; this.policy=policy;
        this.sessions=sessions; this.clock=clock;
        this.audit=audit;
    }
    @Transactional
    public AuthResult register(RegisterRequest request, String userAgent) {
        policy.validate(request.password());
        var email = UserAccountEntity.normalizeEmail(request.email());
        if (users.existsByNormalizedEmail(email)) throw new IdentityException(HttpStatus.CONFLICT,
                "EMAIL_ALREADY_REGISTERED", "Ya existe una cuenta con ese correo");
        var now=OffsetDateTime.now(clock);
        var user=users.save(new UserAccountEntity(email,passwords.encode(request.password()),
                request.displayName(), UserRole.USER, now));
        var pref=preferences.save(new UserPreferencesEntity(user,now));
        audit.success("registration_succeeded",user.getId());
        return new AuthResult(toResponse(user,pref),sessions.create(user,userAgent));
    }
    @Transactional
    public AuthResult login(LoginRequest request,String userAgent) {
        var normalized=UserAccountEntity.normalizeEmail(request.email());
        var user=users.findByNormalizedEmail(normalized).orElseThrow(()->{
            audit.failure("login_failed",normalized);return badCredentials();});
        if (user.getPasswordHash()==null || !passwords.matches(request.password(),user.getPasswordHash())) {
            audit.failure("login_failed",normalized);
            throw badCredentials();
        }
        if(user.getStatus()!=UserStatus.ACTIVE) throw new IdentityException(HttpStatus.FORBIDDEN,
                "ACCOUNT_DISABLED","La cuenta está desactivada");
        user.recordLogin(OffsetDateTime.now(clock)); users.save(user);
        audit.success("login_succeeded",user.getId());
        return new AuthResult(toResponse(user,preference(user.getId())),sessions.create(user,userAgent));
    }
    @Transactional(readOnly=true)
    public UserResponse me(UUID id) { var user=active(id); return toResponse(user,preference(id)); }
    @Transactional
    public UserResponse update(UUID id, UpdateProfileRequest request) {
        var user=active(id); user.updateProfile(request.displayName(),OffsetDateTime.now(clock));
        return toResponse(users.save(user),preference(id));
    }
    @Transactional
    public UserResponse updatePreferences(UUID id, PreferencesRequest request) {
        var user=active(id); var p=preference(id); p.update(request.dailyCaloriesTarget(),
                request.dailyProteinTarget(),request.weeklyBudget(),request.numberOfDays(),request.mealsPerDay(),
                request.strategy(),request.optimizationPreset(),request.dietaryRestrictions(),request.allergens(),
                OffsetDateTime.now(clock)); return toResponse(user,preferences.save(p));
    }
    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request) {
        var user=active(id);
        if(!passwords.matches(request.currentPassword(),user.getPasswordHash())) throw badCredentials();
        policy.validate(request.newPassword()); user.changePassword(passwords.encode(request.newPassword()),
                OffsetDateTime.now(clock)); users.save(user); sessions.revokeAll(id);
        audit.success("password_changed",id);
    }
    @Transactional
    public void disable(UUID id, DisableAccountRequest request) {
        var user=active(id);
        if(!passwords.matches(request.currentPassword(),user.getPasswordHash())) throw badCredentials();
        user.disable(OffsetDateTime.now(clock)); users.save(user); sessions.revokeAll(id);
        audit.success("account_disabled",id);
    }
    private UserAccountEntity active(UUID id) {
        var user=users.findById(id).orElseThrow(() -> new IdentityException(HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND","Recurso no encontrado"));
        if(user.getStatus()!=UserStatus.ACTIVE) throw new IdentityException(HttpStatus.FORBIDDEN,
                "ACCOUNT_DISABLED","La cuenta está desactivada"); return user;
    }
    private UserPreferencesEntity preference(UUID id) { return preferences.findById(id).orElseThrow(); }
    private IdentityException badCredentials(){return new IdentityException(HttpStatus.UNAUTHORIZED,
            "INVALID_CREDENTIALS","Credenciales no válidas");}
    private UserResponse toResponse(UserAccountEntity u,UserPreferencesEntity p){
        return new UserResponse(u.getId(),u.getNormalizedEmail(),u.getDisplayName(),u.getStatus().name(),
                u.getRole().name(),u.getCreatedAt(),new PreferencesResponse(p.getDailyCaloriesTarget(),
                p.getDailyProteinTarget(),p.getWeeklyBudget(),p.getNumberOfDays(),p.getMealsPerDay(),
                p.getStrategy(),p.getPreset(),p.getDietaryRestrictions(),p.getAllergens()));
    }
    public record AuthResult(UserResponse user,SessionTokens tokens){}
}
