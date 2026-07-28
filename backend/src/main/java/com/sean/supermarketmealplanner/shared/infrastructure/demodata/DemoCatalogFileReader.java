package com.sean.supermarketmealplanner.shared.infrastructure.demodata;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class DemoCatalogFileReader {

    private final ObjectMapper objectMapper;
    private final Resource resource;
    private volatile DemoCatalogDocument cachedDocument;

    public DemoCatalogFileReader(
            ObjectMapper objectMapper,
            @Value("${app.catalog.demo-resource}") Resource resource
    ) {
        this.objectMapper = objectMapper;
        this.resource = resource;
    }

    public DemoCatalogDocument read() {
        var current = cachedDocument;
        if (current == null) {
            synchronized (this) {
                current = cachedDocument;
                if (current == null) {
                    current = loadDocument();
                    cachedDocument = current;
                }
            }
        }
        return current;
    }

    private DemoCatalogDocument loadDocument() {
        try (var inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, DemoCatalogDocument.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the controlled demo catalog", exception);
        }
    }
}
