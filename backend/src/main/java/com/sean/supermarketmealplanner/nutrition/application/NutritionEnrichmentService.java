package com.sean.supermarketmealplanner.nutrition.application;

import com.fasterxml.jackson.databind.*;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.*;
import com.sean.supermarketmealplanner.catalogsync.application.CatalogSyncException;
import com.sean.supermarketmealplanner.catalogsync.infrastructure.airflow.AirflowClient;
import com.sean.supermarketmealplanner.identity.application.*;
import com.sean.supermarketmealplanner.nutrition.domain.NutritionEnums.*;
import com.sean.supermarketmealplanner.nutrition.infrastructure.persistence.*;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NutritionEnrichmentService {
    private static final Logger log=LoggerFactory.getLogger(NutritionEnrichmentService.class);
    private static final List<EnrichmentStatus> ACTIVE=List.of(EnrichmentStatus.PENDING,EnrichmentStatus.RUNNING);
    private final NutritionEnrichmentRunRepository runs;private final NutritionMatchCandidateRepository candidates;
    private final NutritionRepository nutrition;private final ProductRepository products;private final CurrentUserProvider users;
    private final NutritionEnrichmentProperties properties;private final AirflowClient airflow;private final ObjectMapper json;
    private final JdbcTemplate jdbc;private final NutritionDataValidator validator;private final Clock clock;private final MeterRegistry metrics;private final InMemoryRateLimiter limiter;
    public NutritionEnrichmentService(NutritionEnrichmentRunRepository runs,NutritionMatchCandidateRepository candidates,
        NutritionRepository nutrition,ProductRepository products,CurrentUserProvider users,NutritionEnrichmentProperties properties,
        AirflowClient airflow,ObjectMapper json,JdbcTemplate jdbc,NutritionDataValidator validator,Clock clock,MeterRegistry metrics,InMemoryRateLimiter limiter){
        this.runs=runs;this.candidates=candidates;this.nutrition=nutrition;this.products=products;this.users=users;this.properties=properties;
        this.airflow=airflow;this.json=json;this.jdbc=jdbc;this.validator=validator;this.clock=clock;this.metrics=metrics;this.limiter=limiter;}

    @Transactional public NutritionAdminDtos.Accepted trigger(NutritionAdminDtos.RunRequest request){ensureEnabled();
        if(runs.existsByStatusIn(ACTIVE))throw problem(HttpStatus.CONFLICT,"NUTRITION_ENRICHMENT_ALREADY_RUNNING","Ya existe un enriquecimiento activo");
        String provider=request.provider()==null||request.provider().isBlank()?properties.provider():request.provider().toUpperCase(Locale.ROOT);
        if(!Set.of("LOCAL_JSON","OPEN_FOOD_FACTS").contains(provider))throw problem(HttpStatus.SERVICE_UNAVAILABLE,"NUTRITION_PROVIDER_UNAVAILABLE","Proveedor no soportado");
        if(provider.equals("OPEN_FOOD_FACTS")&&!properties.openFoodFacts().enabled())throw problem(HttpStatus.SERVICE_UNAVAILABLE,"NUTRITION_PROVIDER_UNAVAILABLE","Open Food Facts está deshabilitado");
        var actor=users.require();limiter.check("nutrition-enrichment",actor.userId().toString(),properties.manualRatePerHour(),Duration.ofHours(1));var now=OffsetDateTime.now(clock);var run=new NutritionEnrichmentRunEntity(provider,TriggeredBy.MANUAL,actor.userId(),json.createObjectNode(),now);
        try{runs.saveAndFlush(run);}catch(DataIntegrityViolationException exception){throw problem(HttpStatus.CONFLICT,"NUTRITION_ENRICHMENT_ALREADY_RUNNING","Ya existe un enriquecimiento activo");}
        String dagRunId="manual__nutrition__"+run.getId();
        try{airflow.trigger("nutrition_enrichment",dagRunId,Map.of("runId",run.getId().toString(),"provider",provider,
            "batchSize",properties.batchSize(),"autoAcceptThreshold",properties.autoAcceptThreshold(),
            "manualReviewThreshold",properties.manualReviewThreshold()));run.accepted(dagRunId,now);runs.save(run);
            metrics.counter("nutrition.enrichment.requests","provider",provider).increment();
            log.info("nutrition_enrichment_requested run={} provider={} actor={}",run.getId(),provider,actor.userId());
            return new NutritionAdminDtos.Accepted(run.getId(),dagRunId,run.getStatus(),run.getCreatedAt());
        }catch(CatalogSyncException exception){run.failed(json.createObjectNode().put("code",exception.code()).put("message",exception.getMessage()),OffsetDateTime.now(clock));runs.save(run);
            throw problem(HttpStatus.SERVICE_UNAVAILABLE,"NUTRITION_ENRICHMENT_FAILED","Airflow no aceptó el enriquecimiento");}
    }
    public PageResponse<NutritionAdminDtos.Run> runs(int page,int size){return PageResponse.from(runs.findAllByOrderByCreatedAtDesc(
        PageRequest.of(Math.max(0,page),limit(size))).map(NutritionAdminDtos.Run::from));}
    public NutritionAdminDtos.Run run(UUID id){return NutritionAdminDtos.Run.from(findRun(id));}
    public PageResponse<NutritionAdminDtos.Candidate> candidates(CandidateStatus status,int page,int size){var resolved=status==null?CandidateStatus.PENDING:status;
        return PageResponse.from(candidates.findByStatusOrderByConfidenceScoreDescCreatedAtAsc(resolved,PageRequest.of(Math.max(0,page),limit(size))).map(NutritionAdminDtos.Candidate::from));}
    public NutritionAdminDtos.Candidate candidate(UUID id){return NutritionAdminDtos.Candidate.from(findCandidate(id));}

    @Transactional public NutritionAdminDtos.NutritionSnapshot accept(UUID id,NutritionAdminDtos.AcceptRequest request){var actor=users.require();var candidate=findCandidate(id);
        ensureVersion(candidate,request.expectedVersion());ensureFresh(candidate);var existing=candidate.getProduct().getNutrition();
        if(existing!=null&&"MANUAL_OVERRIDE".equals(existing.getVerificationStatus()))throw problem(HttpStatus.CONFLICT,"NUTRITION_MANUAL_OVERRIDE_PROTECTED","La nutrición manual está protegida");
        var input=request.nutrition()!=null?request.nutrition():payload(candidate.getCandidatePayload());
        apply(candidate.getProduct(),input,candidate.getProvider(),"VERIFIED",candidate.getConfidenceScore(),candidate.getExternalReference(),
            actor.userId(),request.reason()==null?"Candidato aceptado manualmente":request.reason(),"CANDIDATE_ACCEPTED");
        try{candidate.accept(actor.userId(),OffsetDateTime.now(clock));candidates.save(candidate);}catch(IllegalStateException exception){throw reviewed();}
        metrics.counter("nutrition.candidates.reviewed","decision","accepted").increment();
        log.info("nutrition_candidate_accepted candidate={} product={} actor={}",id,candidate.getProduct().getId(),actor.userId());
        return NutritionAdminDtos.NutritionSnapshot.from(candidate.getProduct().getNutrition());}
    @Transactional public void reject(UUID id,NutritionAdminDtos.RejectRequest request){var actor=users.require();var candidate=findCandidate(id);
        ensureVersion(candidate,request.expectedVersion());ensureFresh(candidate);try{candidate.reject(actor.userId(),request.reason(),OffsetDateTime.now(clock));candidates.save(candidate);}
        catch(IllegalStateException exception){throw reviewed();}metrics.counter("nutrition.candidates.reviewed","decision","rejected").increment();
        log.info("nutrition_candidate_rejected candidate={} product={} actor={}",id,candidate.getProduct().getId(),actor.userId());}
    @Transactional public NutritionAdminDtos.NutritionSnapshot manual(UUID productId,NutritionAdminDtos.ManualRequest request,boolean create){
        var actor=users.require();var product=products.findById(productId).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","Producto no encontrado"));
        if(create&&product.getNutrition()!=null)throw problem(HttpStatus.CONFLICT,"NUTRITION_MATCH_CONFLICT","El producto ya tiene nutrición");
        apply(product,request.nutrition(),"MANUAL","MANUAL_OVERRIDE",BigDecimal.valueOf(100),null,actor.userId(),request.reason(),"MANUAL");
        metrics.counter("nutrition.manual.overrides").increment();log.info("nutrition_manual_override product={} actor={}",productId,actor.userId());
        return NutritionAdminDtos.NutritionSnapshot.from(product.getNutrition());}
    public List<NutritionAdminDtos.History> history(UUID productId){products.findById(productId).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","Producto no encontrado"));
        return jdbc.query("select id,previous_snapshot_json,new_snapshot_json,change_source,provider,confidence_score,changed_at,reason from product_nutrition_history where product_id=? order by changed_at desc",
            (rs,row)->new NutritionAdminDtos.History(rs.getObject(1,UUID.class),read(rs.getString(2)),read(rs.getString(3)),rs.getString(4),rs.getString(5),rs.getBigDecimal(6),rs.getObject(7,OffsetDateTime.class),rs.getString(8)),productId);}
    public NutritionAdminDtos.Overview overview(){long missing=jdbc.queryForObject("select count(*) from products p left join nutrition n on n.product_id=p.id where n.id is null",Long.class);
        long partial=jdbc.queryForObject("select count(*) from nutrition where completeness in ('PARTIAL','MINIMAL','EMPTY')",Long.class);
        long verified=jdbc.queryForObject("select count(*) from nutrition where verification_status in ('VERIFIED','MANUAL_OVERRIDE')",Long.class);
        long pending=jdbc.queryForObject("select count(*) from nutrition_match_candidates where status='PENDING' and expires_at>now()",Long.class);
        var latest=runs.findAllByOrderByCreatedAtDesc(PageRequest.of(0,1)).stream().findFirst().map(NutritionAdminDtos.Run::from).orElse(null);
        return new NutritionAdminDtos.Overview(properties.enabled(),properties.provider(),properties.cron(),missing,partial,verified,pending,latest);}

    private void apply(ProductEntity product,NutritionAdminDtos.NutritionInput input,String source,String verification,BigDecimal confidence,
        String reference,UUID actor,String reason,String changeSource){var values=new NutritionEntity.NutritionValues(input.basis(),input.calories(),input.protein(),input.carbohydrates(),input.fat(),input.fiber(),input.sugars(),input.salt(),input.saturatedFat());
        validator.validate(values);var before=NutritionAdminDtos.NutritionSnapshot.from(product.getNutrition());var now=OffsetDateTime.now(clock);
        var entity=product.getNutrition();if(entity==null){entity=new NutritionEntity(product);product.setNutrition(entity);}
        entity.apply(values,source,verification,confidence,reference,now,actor,now,now);nutrition.save(entity);
        var after=NutritionAdminDtos.NutritionSnapshot.from(entity);JsonNode newJson=json.valueToTree(after);String hash=hash(newJson.toString());
        jdbc.update("insert into product_nutrition_history(id,product_id,previous_snapshot_json,new_snapshot_json,change_source,provider,confidence_score,changed_by,changed_at,reason,snapshot_hash) values (?,?,?::jsonb,?::jsonb,?,?,?,?,?,?,?) on conflict(product_id,snapshot_hash) do nothing",
            UUID.randomUUID(),product.getId(),before==null?null:json.valueToTree(before).toString(),newJson.toString(),changeSource,source,confidence,actor,now,reason,hash);}
    private NutritionAdminDtos.NutritionInput payload(JsonNode p){try{return json.treeToValue(p,NutritionAdminDtos.NutritionInput.class);}catch(Exception e){throw problem(HttpStatus.UNPROCESSABLE_ENTITY,"NUTRITION_DATA_INVALID","El candidato no contiene nutrición utilizable");}}
    private NutritionEnrichmentRunEntity findRun(UUID id){return runs.findById(id).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"NUTRITION_ENRICHMENT_RUN_NOT_FOUND","Ejecución no encontrada"));}
    private NutritionMatchCandidateEntity findCandidate(UUID id){return candidates.findById(id).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"NUTRITION_CANDIDATE_NOT_FOUND","Candidato no encontrado"));}
    private void ensureFresh(NutritionMatchCandidateEntity c){if(c.getStatus()!=CandidateStatus.PENDING)throw reviewed();if(!c.getExpiresAt().isAfter(OffsetDateTime.now(clock)))throw problem(HttpStatus.CONFLICT,"NUTRITION_CANDIDATE_STALE","El candidato ha caducado");}
    private void ensureVersion(NutritionMatchCandidateEntity c,long expected){if(c.getRowVersion()!=expected)throw problem(HttpStatus.CONFLICT,"NUTRITION_MATCH_CONFLICT","El candidato cambió durante la revisión");}
    private NutritionException reviewed(){return problem(HttpStatus.CONFLICT,"NUTRITION_CANDIDATE_ALREADY_REVIEWED","El candidato ya fue revisado");}
    private void ensureEnabled(){if(!properties.enabled())throw problem(HttpStatus.SERVICE_UNAVAILABLE,"NUTRITION_PROVIDER_UNAVAILABLE","El enriquecimiento está deshabilitado");}
    private int limit(int size){return Math.min(Math.max(size,1),100);}private JsonNode read(String raw){try{return raw==null?null:json.readTree(raw);}catch(Exception e){return null;}}
    private String hash(String value){try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private NutritionException problem(HttpStatus status,String code,String detail){return new NutritionException(status,code,detail);}
}
