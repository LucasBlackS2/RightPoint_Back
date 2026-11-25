package br.com.checkpoint.security;

import br.com.checkpoint.repository.UsuarioRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lê o cabeçalho "Authorization: Bearer <token>", valida o JWT e coloca o
 * usuário no contexto de segurança. O perfil vira a role ROLE_ALUNO,
 * ROLE_PROFESSOR, ROLE_COORDENADOR ou ROLE_ADMIN.
 *
 * Não é um @Component de propósito: assim o Spring Boot não registra o filtro
 * duas vezes (uma como filtro do servlet e outra na cadeia de segurança).
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(PREFIXO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long idUsuario = jwtService.extrairIdUsuario(header.substring(PREFIXO.length()).trim());
                usuarioRepository.findById(idUsuario).ifPresent(usuario -> {
                    var autenticacao = new UsernamePasswordAuthenticationToken(
                            usuario, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getTipoAcesso().name())));
                    autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacao);
                });
            } catch (JwtException | IllegalArgumentException e) {
                // Token inválido/expirado: segue sem autenticação e a rota protegida devolve 401.
            }
        }

        chain.doFilter(request, response);
    }
}
