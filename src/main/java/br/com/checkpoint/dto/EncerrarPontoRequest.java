package br.com.checkpoint.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Usado pelo professor/coordenação para fechar um ponto que o aluno esqueceu aberto. */
public record EncerrarPontoRequest(
        @NotNull(message = "Informe quantos minutos devem ser considerados.")
        @Min(value = 0, message = "Os minutos não podem ser negativos.") Integer tempoTrabalhadoMinutos) {
}
