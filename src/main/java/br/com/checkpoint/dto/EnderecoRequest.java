package br.com.checkpoint.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * latitude/longitude são opcionais: se não vierem, a API tenta buscá-las no
 * Google Maps (quando GOOGLE_MAPS_API_KEY estiver configurada).
 */
public record EnderecoRequest(
        @Size(max = 15) String cep,
        @NotBlank(message = "Informe o logradouro.") @Size(max = 255) String logradouro,
        @NotBlank(message = "Informe a cidade.") @Size(max = 120) String cidade,
        @NotBlank(message = "Informe o estado.") @Size(max = 10) String estado,
        @Size(max = 30) String numero,
        @Size(max = 120) String bairro,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Size(max = 64) String wifiSsid,
        @Size(max = 32) String wifiBssid,
        @Size(max = 255) String ipPublico,
        @Min(value = 20, message = "O raio mínimo é 20 m.") @Max(value = 5000, message = "O raio máximo é 5000 m.") Integer raioMetros,
        Boolean aceitaQrImpresso) {
}
