package br.com.checkpoint.security;

import br.com.checkpoint.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtService(@Value("${checkpoint.jwt.secret}") String segredo,
                      @Value("${checkpoint.jwt.expiration-minutes}") long expiracaoMinutos) {
        // Sem valor padrão de propósito: com uma chave conhecida qualquer um poderia forjar tokens.
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET não configurado ou muito curto. Defina a variável de ambiente JWT_SECRET "
                            + "com pelo menos 32 caracteres aleatórios (veja o arquivo .env.example).");
        }
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("matricula", usuario.getMatricula())
                .claim("tipo", usuario.getTipoAcesso().getValor())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES)))
                .signWith(chave)
                .compact();
    }

    /**
     * Valida assinatura e validade do token e devolve o id do usuário.
     *
     * @throws JwtException se o token for inválido ou estiver expirado
     */
    public Long extrairIdUsuario(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }

    public long getExpiracaoSegundos() {
        return expiracaoMinutos * 60;
    }
}
