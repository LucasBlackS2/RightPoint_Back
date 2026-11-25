package br.com.checkpoint.dto;

import br.com.checkpoint.domain.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioCreateRequest(
        @NotBlank(message = "Informe o nome.") @Size(max = 255) String nome,
        @NotBlank(message = "Informe a matrícula.") @Size(max = 50) String matricula,
        @NotBlank @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres.") String senha,
        @Size(max = 50) String contato,
        @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email,
        @Size(max = 50) String turma,
        TipoUsuario tipoAcesso) {
}
