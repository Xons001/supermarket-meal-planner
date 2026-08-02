package com.sean.supermarketmealplanner.catalogsync;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sean.supermarketmealplanner.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class CatalogSyncApiIntegrationTest extends AbstractIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void adminCanReadEmptyPaginatedHistory() throws Exception {
        mockMvc.perform(get("/api/v1/admin/catalog-syncs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0));
    }
}
