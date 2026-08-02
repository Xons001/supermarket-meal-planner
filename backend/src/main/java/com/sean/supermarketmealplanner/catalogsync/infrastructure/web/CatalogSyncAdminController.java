package com.sean.supermarketmealplanner.catalogsync.infrastructure.web;

import com.sean.supermarketmealplanner.catalogsync.application.*;
import com.sean.supermarketmealplanner.catalogsync.domain.*;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import io.swagger.v3.oas.annotations.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/catalog-syncs") @PreAuthorize("hasRole('ADMIN')")
public class CatalogSyncAdminController {
    private final CatalogSyncService service; public CatalogSyncAdminController(CatalogSyncService service){this.service=service;}
    @GetMapping("/overview") public CatalogSyncDtos.Overview overview(){return service.overview();}
    @GetMapping public PageResponse<CatalogSyncDtos.Run> list(@RequestParam(required=false)String supermarketCode,
        @RequestParam(required=false)CatalogSyncStatus status,@RequestParam(required=false)CatalogSyncType syncType,
        @RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return service.list(supermarketCode,status,syncType,page,size);}
    @GetMapping("/{id}") public CatalogSyncDtos.Run get(@PathVariable UUID id){return service.get(id);}
    @GetMapping("/{id}/errors") public PageResponse<CatalogSyncDtos.Error> errors(@PathVariable UUID id,
        @RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="50")int size){return service.errors(id,page,size);}
    @PostMapping public ResponseEntity<CatalogSyncDtos.Accepted> trigger(@Valid @RequestBody CatalogSyncDtos.TriggerRequest request){
        return ResponseEntity.accepted().body(service.trigger(request));}
    @PostMapping("/{id}/retry") public ResponseEntity<CatalogSyncDtos.Accepted> retry(@PathVariable UUID id){
        return ResponseEntity.accepted().body(service.retry(id));}
}
