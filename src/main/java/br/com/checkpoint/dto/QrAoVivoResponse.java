package br.com.checkpoint.dto;

import java.time.OffsetDateTime;

/**
 * QR Code ao vivo para o professor mostrar na tela.
 * png_base64 pode ir direto num <Image source={{ uri: "data:image/png;base64," + png_base64 }} />.
 */
public record QrAoVivoResponse(String conteudo, String pngBase64, OffsetDateTime expiraEm, int validoPorSegundos,
                               double distanciaMetros, double raioMetros) {
}
