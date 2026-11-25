package br.com.checkpoint.dto;

import jakarta.validation.constraints.NotBlank;

/** {"perfil": "aluno"} ou {"perfil": "professor"} */
public record DemoRequest(@NotBlank(message = "Escolha o perfil da demonstração.") String perfil) {
}
