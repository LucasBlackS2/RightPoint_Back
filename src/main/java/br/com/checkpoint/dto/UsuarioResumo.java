package br.com.checkpoint.dto;

import br.com.checkpoint.domain.Usuario;

public record UsuarioResumo(Long id, String nome, String matricula) {

    public static UsuarioResumo from(Usuario u) {
        return new UsuarioResumo(u.getId(), u.getNome(), u.getMatricula());
    }
}
