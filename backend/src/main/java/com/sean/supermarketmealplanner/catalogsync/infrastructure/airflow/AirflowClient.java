package com.sean.supermarketmealplanner.catalogsync.infrastructure.airflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sean.supermarketmealplanner.catalogsync.application.*;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class AirflowClient {
    private final RestClient client;
    private final CatalogSyncProperties properties;
    public AirflowClient(RestClient.Builder builder, CatalogSyncProperties properties) {
        this.properties=properties; this.client=builder
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory())
                .baseUrl(properties.airflowBaseUrl()).build();
    }
    public void trigger(String dagId, String dagRunId, Map<String,Object> conf) {
        try {
            var token=client.post().uri("/auth/token")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Map.of("username",properties.airflowUsername(),"password",properties.airflowPassword()))
                .retrieve().body(TokenResponse.class);
            if(token==null || token.accessToken()==null || token.accessToken().isBlank())
                throw new CatalogSyncException(HttpStatus.BAD_GATEWAY,"AIRFLOW_RESPONSE_INVALID","Airflow no devolvió un token válido");
            var request=new java.util.LinkedHashMap<String,Object>();
            request.put("dag_run_id",dagRunId); request.put("logical_date",null); request.put("conf",conf);
            client.post().uri("/api/v2/dags/{dagId}/dagRuns",dagId)
                .header("Authorization","Bearer "+token.accessToken())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve().toBodilessEntity();
        } catch (CatalogSyncException exception) { throw exception;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            throw new CatalogSyncException(HttpStatus.SERVICE_UNAVAILABLE,"AIRFLOW_AUTHENTICATION_FAILED","Airflow rechazó las credenciales configuradas");
        } catch (RestClientResponseException exception) {
            throw new CatalogSyncException(HttpStatus.BAD_GATEWAY,"AIRFLOW_RESPONSE_INVALID","Airflow rechazó la solicitud de sincronización");
        } catch (ResourceAccessException exception) {
            throw new CatalogSyncException(HttpStatus.SERVICE_UNAVAILABLE,"AIRFLOW_UNAVAILABLE","Airflow no está disponible");
        }
    }
    public boolean healthy() {
        try { client.get().uri("/api/v2/monitor/health").retrieve().toBodilessEntity(); return true; }
        catch (RestClientException exception) { return false; }
    }
    private record TokenResponse(@JsonProperty("access_token") String accessToken) {}
}
