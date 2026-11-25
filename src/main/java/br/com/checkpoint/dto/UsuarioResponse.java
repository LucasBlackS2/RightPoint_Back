package br.com.checkpoint.dto;

import br.com.checkpoint.domain.TipoUsuario;
import br.com.checkpoint.domain.Usuario;

public record UsuarioResponse(Long id, String nome, String matricula, String email, String contato,
                              String turma, TipoUsuario tipoAcesso, boolean demonstracao) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getMatricula(), u.getEmail(), u.getContato(),
                u.getTurma(), u.getTipoAcesso(), u.isDemonstracao());
    }
}
