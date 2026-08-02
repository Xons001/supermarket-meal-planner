package com.sean.supermarketmealplanner.catalogsync.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.supermarketmealplanner.catalogsync.domain.*;
import com.sean.supermarketmealplanner.catalogsync.infrastructure.airflow.AirflowClient;
import com.sean.supermarketmealplanner.catalogsync.infrastructure.persistence.*;
import com.sean.supermarketmealplanner.identity.application.*;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.*;
import java.util.*;
import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CatalogSyncService {
    private static final Logger log=LoggerFactory.getLogger(CatalogSyncService.class);
    private static final List<CatalogSyncStatus> ACTIVE=List.of(CatalogSyncStatus.PENDING,CatalogSyncStatus.RUNNING);
    private final CatalogSyncRunRepository runs; private final CatalogSyncErrorRepository errors;
    private final SupermarketRepository supermarkets; private final CurrentUserProvider users; private final InMemoryRateLimiter limiter;
    private final CatalogSyncProperties properties; private final AirflowClient airflow; private final ObjectMapper json;
    private final Clock clock; private final MeterRegistry metrics;
    public CatalogSyncService(CatalogSyncRunRepository runs,CatalogSyncErrorRepository errors,SupermarketRepository supermarkets,
        CurrentUserProvider users,InMemoryRateLimiter limiter,CatalogSyncProperties properties,AirflowClient airflow,
        ObjectMapper json,Clock clock,MeterRegistry metrics){this.runs=runs;this.errors=errors;this.supermarkets=supermarkets;
        this.users=users;this.limiter=limiter;this.properties=properties;this.airflow=airflow;this.json=json;this.clock=clock;this.metrics=metrics;}

    public CatalogSyncDtos.Accepted trigger(CatalogSyncDtos.TriggerRequest request){
        ensureEnabled(); var user=users.require(); limiter.check("catalog-sync",user.userId().toString(),properties.manualRatePerHour(),Duration.ofHours(1));
        var code=parseCode(request.supermarketCode()); var supermarket=supermarkets.findByCode(code).orElseThrow(()->unsupported(code.name()));
        if(!supermarket.isEnabled()) throw unsupported(code.name());
        return createAndTrigger(supermarket,request.syncType(),CatalogSyncTrigger.MANUAL,user.userId(),null);
    }
    public CatalogSyncDtos.Accepted retry(UUID id){
        ensureEnabled(); var user=users.require(); limiter.check("catalog-sync",user.userId().toString(),properties.manualRatePerHour(),Duration.ofHours(1));
        var previous=find(id); if(!previous.getStatus().terminal() || previous.getStatus()==CatalogSyncStatus.SUCCESS)
            throw new CatalogSyncException(HttpStatus.CONFLICT,"CATALOG_SYNC_NOT_RETRYABLE","La ejecución todavía no se puede reintentar");
        return createAndTrigger(previous.getSupermarket(),previous.getSyncType(),CatalogSyncTrigger.RETRY,user.userId(),previous);
    }
    private CatalogSyncDtos.Accepted createAndTrigger(com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketEntity supermarket,
        CatalogSyncType type,CatalogSyncTrigger trigger,UUID userId,CatalogSyncRunEntity retryOf){
        if(runs.existsBySupermarketIdAndStatusIn(supermarket.getId(),ACTIVE)) throw alreadyRunning();
        var now=OffsetDateTime.now(clock); var configuration=json.createObjectNode().put("provider",properties.provider());
        var run=new CatalogSyncRunEntity(supermarket,type,trigger,properties.provider(),userId,retryOf,configuration,now);
        try { runs.saveAndFlush(run); } catch(DataIntegrityViolationException exception){ throw alreadyRunning(); }
        var dag=type==CatalogSyncType.FULL_CATALOG?"catalog_full_sync":"catalog_price_sync";
        var dagRunId="manual__"+run.getId();
        try {
            airflow.trigger(dag,dagRunId,Map.of("syncRunId",run.getId().toString(),"supermarketCode",supermarket.getCode().name(),
                "provider",properties.provider(),"triggeredBy",trigger.name()));
            run.accepted(dag,dagRunId,OffsetDateTime.now(clock)); runs.save(run);
            metrics.counter("catalog.sync.requests","type",type.name(),"supermarket",supermarket.getCode().name()).increment();
            log.info("catalog_sync_requested run={} supermarket={} type={} actor={}",run.getId(),supermarket.getCode(),type,userId);
            return new CatalogSyncDtos.Accepted(run.getId(),dag,dagRunId,run.getStatus(),run.getRequestedAt());
        } catch(CatalogSyncException exception){
            run.fail(json.createObjectNode().put("errorCode",exception.code()).put("message",exception.getMessage()),OffsetDateTime.now(clock));
            runs.save(run); log.warn("catalog_sync_rejected run={} code={}",run.getId(),exception.code()); throw exception;
        }
    }
    public PageResponse<CatalogSyncDtos.Run> list(String supermarketCode,CatalogSyncStatus status,CatalogSyncType type,int page,int size){
        var code=supermarketCode==null||supermarketCode.isBlank()?null:parseCode(supermarketCode);
        var pageable=PageRequest.of(Math.max(0,page),Math.min(Math.max(size,1),100),Sort.by(Sort.Direction.DESC,"requestedAt"));
        var source=runs.search(code,status,type,pageable).map(CatalogSyncDtos.Run::from); return PageResponse.from(source);
    }
    public CatalogSyncDtos.Run get(UUID id){return CatalogSyncDtos.Run.from(find(id));}
    public PageResponse<CatalogSyncDtos.Error> errors(UUID id,int page,int size){find(id);return PageResponse.from(errors
        .findBySyncRunIdOrderByCreatedAtAscIdAsc(id,PageRequest.of(Math.max(page,0),Math.min(Math.max(size,1),100))).map(CatalogSyncDtos.Error::from));}
    public CatalogSyncDtos.Overview overview(){var latest=runs.findFirstByOrderByRequestedAtDesc().map(CatalogSyncDtos.Run::from).orElse(null);
        return new CatalogSyncDtos.Overview(properties.enabled(),properties.enabled()&&airflow.healthy(),properties.provider(),
            blankToNull(properties.airflowPublicUrl()),properties.fullSchedule(),properties.priceSchedule(),latest);}
    private CatalogSyncRunEntity find(UUID id){return runs.findById(id).orElseThrow(()->new CatalogSyncException(HttpStatus.NOT_FOUND,
        "CATALOG_SYNC_NOT_FOUND","No existe la ejecución de sincronización"));}
    private void ensureEnabled(){if(!properties.enabled())throw new CatalogSyncException(HttpStatus.SERVICE_UNAVAILABLE,
        "CATALOG_SYNC_DISABLED","La sincronización de catálogo está deshabilitada");}
    private SupermarketCode parseCode(String value){try{return SupermarketCode.valueOf(value.trim().toUpperCase(Locale.ROOT));}
        catch(Exception e){throw unsupported(value);}}
    private CatalogSyncException unsupported(String code){return new CatalogSyncException(HttpStatus.UNPROCESSABLE_ENTITY,
        "CATALOG_SYNC_PROVIDER_UNSUPPORTED","No existe un proveedor habilitado para "+code);}
    private CatalogSyncException alreadyRunning(){return new CatalogSyncException(HttpStatus.CONFLICT,
        "CATALOG_SYNC_ALREADY_RUNNING","Ya existe una sincronización activa para este supermercado");}
    private String blankToNull(String value){return value==null||value.isBlank()?null:value;}
}
