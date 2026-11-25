package br.com.checkpoint.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Perfis de acesso. No banco e no JSON o valor é gravado em minúsculo
 * ("aluno", "professor"...), igual à versão anterior em Python.
 */
public enum TipoUsuario {
    ALUNO("aluno"),
    PROFESSOR("professor"),
    COORDENADOR("coordenador"),
    ADMIN("admin");

    private final String valor;

    TipoUsuario(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static TipoUsuario of(String valor) {
        if (valor == null) {
            return null;
        }
        for (TipoUsuario tipo : values()) {
            if (tipo.valor.equalsIgnoreCase(valor.trim()) || tipo.name().equalsIgnoreCase(valor.trim())) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de usuário inválido: " + valor
                + ". Use aluno, professor, coordenador ou admin.");
    }
}
