package br.com.checkpoint.dto;

import br.com.checkpoint.domain.Ponto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Horários devolvidos em UTC com fuso explícito (ex.: 2026-11-11T12:00:00Z). */
public record PontoResponse(Long id, Long idContrato, LocalDate data, OffsetDateTime horaEntrada,
                            OffsetDateTime horaSaida, Integer tempoTrabalhadoMinutos, boolean ativo,
                            Double distanciaEntradaMetros, Double distanciaSaidaMetros, boolean encerradoManualmente,
                            boolean fotoEntrada, boolean fotoSaida, String wifiEntrada, String wifiSaida) {

    public static PontoResponse from(Ponto p) {
        return new PontoResponse(p.getId(), p.getContrato().getId(), p.getData(), utc(p.getHoraEntrada()),
                utc(p.getHoraSaida()), p.getTempoTrabalhadoMinutos(), p.isAtivo(), p.getDistanciaEntradaMetros(),
                p.getDistanciaSaidaMetros(), p.getEncerradoPor() != null, p.isFotoEntrada(), p.isFotoSaida(),
                p.getWifiEntrada(), p.getWifiSaida());
    }

    private static OffsetDateTime utc(LocalDateTime dt) {
        return dt == null ? null : dt.atOffset(ZoneOffset.UTC);
    }
}
