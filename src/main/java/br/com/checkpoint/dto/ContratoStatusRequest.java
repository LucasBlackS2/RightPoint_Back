package br.com.checkpoint.dto;

import jakarta.validation.constraints.NotNull;

public record ContratoStatusRequest(@NotNull(message = "Informe o status (true/false).") Boolean status) {
}
