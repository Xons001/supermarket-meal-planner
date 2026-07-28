package com.sean.supermarketmealplanner.supermarket.infrastructure.web;

import com.sean.supermarketmealplanner.supermarket.application.SupermarketQueryService;
import com.sean.supermarketmealplanner.supermarket.application.SupermarketResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/supermarkets")
@Tag(name = "Supermarkets")
public class SupermarketController {

    private final SupermarketQueryService supermarketQueryService;

    public SupermarketController(SupermarketQueryService supermarketQueryService) {
        this.supermarketQueryService = supermarketQueryService;
    }

    @GetMapping
    @Operation(summary = "List the supermarkets known by the platform")
    public List<SupermarketResponse> findAll() {
        return supermarketQueryService.findAll();
    }
}
