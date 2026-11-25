package br.com.checkpoint.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Informe a matrícula.") String matricula,
        @NotBlank(message = "Informe a senha.") String senha) {
}
