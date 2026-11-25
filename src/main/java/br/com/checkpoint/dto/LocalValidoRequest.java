package br.com.checkpoint.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Professor define onde o QR Code vale: a posição (normalmente a atual, estando
 * no local), o raio e se o QR impresso continua aceito. Todos os campos são
 * opcionais; só os enviados são alterados.
 */
public record LocalValidoRequest(
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Min(value = 20, message = "O raio mínimo é 20 m.") @Max(value = 5000, message = "O raio máximo é 5000 m.") Integer raioMetros,
        Boolean aceitaQrImpresso) {
}
