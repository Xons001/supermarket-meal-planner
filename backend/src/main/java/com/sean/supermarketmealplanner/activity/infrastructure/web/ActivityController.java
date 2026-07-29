package com.sean.supermarketmealplanner.activity.infrastructure.web;

import com.sean.supermarketmealplanner.activity.application.ActivityResponse;
import com.sean.supermarketmealplanner.activity.application.ActivityService;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activity")
@Tag(name = "Activity")
public class ActivityController {
    private final ActivityService service;

    public ActivityController(ActivityService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Get the authenticated user's combined activity feed")
    public PageResponse<ActivityResponse> findAll(@RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.findAll(type == null || type.isBlank() ? null : type, page, size);
    }
}
