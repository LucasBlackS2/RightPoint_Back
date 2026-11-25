package br.com.checkpoint.dto;

/** JSON: {"access_token": "...", "token_type": "bearer", "expires_in": 3600, "tipo_acesso": "aluno"} */
public record TokenResponse(String accessToken, String tokenType, long expiresIn, String tipoAcesso) {
}
