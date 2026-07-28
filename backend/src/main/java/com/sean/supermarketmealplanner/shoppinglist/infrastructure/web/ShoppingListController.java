package com.sean.supermarketmealplanner.shoppinglist.infrastructure.web;

import com.sean.supermarketmealplanner.shared.application.PageResponse;
import com.sean.supermarketmealplanner.shoppinglist.application.ShoppingListCsvExporter;
import com.sean.supermarketmealplanner.shoppinglist.application.ShoppingListResponse;
import com.sean.supermarketmealplanner.shoppinglist.application.ShoppingListSearchRequestParser;
import com.sean.supermarketmealplanner.shoppinglist.application.ShoppingListService;
import com.sean.supermarketmealplanner.shoppinglist.application.ShoppingListSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shopping-lists")
@Tag(name = "Shopping lists")
public class ShoppingListController {

    private final ShoppingListService service;
    private final ShoppingListSearchRequestParser parser;
    private final ShoppingListCsvExporter exporter;

    public ShoppingListController(
            ShoppingListService service,
            ShoppingListSearchRequestParser parser,
            ShoppingListCsvExporter exporter
    ) {
        this.service = service;
        this.parser = parser;
        this.exporter = exporter;
    }

    @GetMapping
    @Operation(summary = "List shopping lists with filters and pagination")
    public PageResponse<ShoppingListSummaryResponse> findAll(
            @RequestParam(required = false) String supermarketCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime generatedFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime generatedTo,
            @RequestParam(required = false) Boolean calculationComplete,
            @RequestParam(required = false) Boolean budgetExceeded,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "generatedAt,desc") String sort
    ) {
        return service.findAll(parser.parse(
                supermarketCode,
                status,
                generatedFrom,
                generatedTo,
                calculationComplete,
                budgetExceeded,
                page,
                size,
                sort
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a shopping list snapshot")
    public ShoppingListResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping(value = "/{id}/export", produces = "text/csv")
    @Operation(summary = "Export a shopping list as UTF-8 CSV")
    public ResponseEntity<byte[]> export(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "csv") String format
    ) {
        if (!"csv".equalsIgnoreCase(format)) {
            throw new com.sean.supermarketmealplanner.shoppinglist.application.ShoppingListException(
                    "Only CSV export is supported",
                    "INVALID_EXPORT_FORMAT",
                    400
            );
        }
        var body = service.exportCsv(id, exporter);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("shopping-list-" + id + ".csv")
                .build());
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
