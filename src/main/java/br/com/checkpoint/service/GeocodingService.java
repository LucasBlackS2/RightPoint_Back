package br.com.checkpoint.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/** Converte um endereço em latitude/longitude usando a Geocoding API do Google. */
@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private static final String URL = "https://maps.googleapis.com/maps/api/geocode/json";

    public record Coordenadas(double latitude, double longitude) {
    }

    private final String apiKey;
    private final RestClient restClient;

    public GeocodingService(@Value("${checkpoint.google-maps-api-key:}") String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(5_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public boolean isConfigurado() {
        return !apiKey.isEmpty();
    }

    /** Devolve vazio se a chave não estiver configurada, o endereço não for encontrado ou o Google falhar. */
    public Optional<Coordenadas> buscarCoordenadas(String endereco) {
        if (!isConfigurado()) {
            return Optional.empty();
        }
        try {
            JsonNode resposta = restClient.get()
                    .uri(URL + "?address={endereco}&key={chave}", endereco, apiKey)
                    .retrieve()
                    .body(JsonNode.class);

            if (resposta != null && "OK".equals(resposta.path("status").asText())) {
                JsonNode local = resposta.path("results").path(0).path("geometry").path("location");
                return Optional.of(new Coordenadas(local.path("lat").asDouble(), local.path("lng").asDouble()));
            }
            log.warn("Google não encontrou coordenadas (status {}) para: {}",
                    resposta == null ? "sem resposta" : resposta.path("status").asText(), endereco);
        } catch (RestClientException e) {
            log.warn("Falha ao consultar o Google Maps: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
