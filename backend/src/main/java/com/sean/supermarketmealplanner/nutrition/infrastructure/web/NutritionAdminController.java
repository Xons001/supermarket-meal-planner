package com.sean.supermarketmealplanner.nutrition.infrastructure.web;

import com.sean.supermarketmealplanner.nutrition.application.*;
import com.sean.supermarketmealplanner.nutrition.domain.NutritionEnums.CandidateStatus;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @PreAuthorize("hasRole('ADMIN')")
public class NutritionAdminController {
    private final NutritionEnrichmentService service;
    public NutritionAdminController(NutritionEnrichmentService service){this.service=service;}
    @GetMapping("/api/v1/admin/nutrition-enrichment/overview") public NutritionAdminDtos.Overview overview(){return service.overview();}
    @GetMapping("/api/v1/admin/nutrition-enrichment/runs") public PageResponse<NutritionAdminDtos.Run> runs(
        @RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return service.runs(page,size);}
    @GetMapping("/api/v1/admin/nutrition-enrichment/runs/{id}") public NutritionAdminDtos.Run run(@PathVariable UUID id){return service.run(id);}
    @PostMapping("/api/v1/admin/nutrition-enrichment/runs") public ResponseEntity<NutritionAdminDtos.Accepted> trigger(
        @RequestBody(required=false) NutritionAdminDtos.RunRequest request){return ResponseEntity.accepted().body(service.trigger(request==null?new NutritionAdminDtos.RunRequest(null):request));}
    @GetMapping("/api/v1/admin/nutrition-candidates") public PageResponse<NutritionAdminDtos.Candidate> candidates(
        @RequestParam(required=false)CandidateStatus status,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return service.candidates(status,page,size);}
    @GetMapping("/api/v1/admin/nutrition-candidates/{id}") public NutritionAdminDtos.Candidate candidate(@PathVariable UUID id){return service.candidate(id);}
    @PostMapping("/api/v1/admin/nutrition-candidates/{id}/accept") public NutritionAdminDtos.NutritionSnapshot accept(
        @PathVariable UUID id,@Valid @RequestBody NutritionAdminDtos.AcceptRequest request){return service.accept(id,request);}
    @PostMapping("/api/v1/admin/nutrition-candidates/{id}/reject") public ResponseEntity<Void> reject(
        @PathVariable UUID id,@Valid @RequestBody NutritionAdminDtos.RejectRequest request){service.reject(id,request);return ResponseEntity.noContent().build();}
    @PostMapping("/api/v1/admin/products/{productId}/nutrition") public ResponseEntity<NutritionAdminDtos.NutritionSnapshot> create(
        @PathVariable UUID productId,@Valid @RequestBody NutritionAdminDtos.ManualRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.manual(productId,request,true));}
    @PutMapping("/api/v1/admin/products/{productId}/nutrition") public NutritionAdminDtos.NutritionSnapshot update(
        @PathVariable UUID productId,@Valid @RequestBody NutritionAdminDtos.ManualRequest request){return service.manual(productId,request,false);}
    @GetMapping("/api/v1/admin/products/{productId}/nutrition-history") public List<NutritionAdminDtos.History> history(@PathVariable UUID productId){return service.history(productId);}
}
