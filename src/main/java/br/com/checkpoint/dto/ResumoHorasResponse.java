package br.com.checkpoint.dto;

/**
 * Progresso do estágio. percentual é null quando o contrato não tem
 * carga_horaria_total definida.
 */
public record ResumoHorasResponse(Long idContrato, long minutosCumpridos, double horasCumpridas,
                                  Integer cargaHorariaTotal, Double horasRestantes, Double percentual,
                                  boolean pontoAberto) {
}
