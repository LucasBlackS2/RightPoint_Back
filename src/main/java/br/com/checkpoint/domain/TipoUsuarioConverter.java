package br.com.checkpoint.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Grava o enum no banco como texto minúsculo ("aluno", "professor"...). */
@Converter
public class TipoUsuarioConverter implements AttributeConverter<TipoUsuario, String> {

    @Override
    public String convertToDatabaseColumn(TipoUsuario tipo) {
        return tipo == null ? null : tipo.getValor();
    }

    @Override
    public TipoUsuario convertToEntityAttribute(String valor) {
        return valor == null ? null : TipoUsuario.of(valor);
    }
}
