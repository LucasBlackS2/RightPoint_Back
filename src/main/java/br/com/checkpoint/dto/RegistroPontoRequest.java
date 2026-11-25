package br.com.checkpoint.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Versão JSON do registro de ponto (sem selfie). O app usa a versão
 * multipart, que envia os mesmos campos + o arquivo "foto".
 * JSON: {"latitude", "longitude", "qr_code", "wifi_ssid", "wifi_bssid"}
 */
public record RegistroPontoRequest(
        @NotNull(message = "Informe a latitude.") @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull(message = "Informe a longitude.") @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Size(max = 200) String qrCode,
        @Size(max = 64) String wifiSsid,
        @Size(max = 32) String wifiBssid) {
}
