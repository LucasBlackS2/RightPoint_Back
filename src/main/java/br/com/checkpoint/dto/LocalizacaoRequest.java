package br.com.checkpoint.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** Localização atual do celular do aluno. JSON: {"latitude": -8.28, "longitude": -35.97} */
public record LocalizacaoRequest(
        @NotNull(message = "Informe a latitude.") @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull(message = "Informe a longitude.") @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude) {
}
