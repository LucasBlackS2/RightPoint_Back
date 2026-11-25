package br.com.checkpoint.dto;

/** Token de uma conta de demonstração + o id do contrato de exemplo (usado pelo tour do app). */
public record DemoResponse(String accessToken, String tokenType, long expiresIn, String tipoAcesso,
                           Long idContratoDemo, Long idLocalDemo) {
}
