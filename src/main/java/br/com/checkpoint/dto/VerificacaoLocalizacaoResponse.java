package br.com.checkpoint.dto;

public record VerificacaoLocalizacaoResponse(boolean dentroDoRaio, double distanciaMetros, double raioMetros) {
}
