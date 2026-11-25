package br.com.checkpoint.service;

import br.com.checkpoint.domain.Usuario;
import br.com.checkpoint.dto.TokenResponse;
import br.com.checkpoint.exception.ApiException;
import br.com.checkpoint.repository.UsuarioRepository;
import br.com.checkpoint.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse login(String matricula, String senha) {
        Usuario usuario = usuarioRepository.findByMatricula(matricula.trim())
                .orElseThrow(() -> ApiException.unauthorized("Matrícula ou senha inválida."));

        if (!senhaConfere(usuario, senha)) {
            throw ApiException.unauthorized("Matrícula ou senha inválida.");
        }

        return new TokenResponse(jwtService.gerarToken(usuario), "bearer", jwtService.getExpiracaoSegundos(),
                usuario.getTipoAcesso().getValor());
    }

    @Transactional
    public void alterarSenha(Long idUsuario, String senhaAtual, String novaSenha) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
        if (usuario.isDemonstracao()) {
            throw ApiException.forbidden("Contas de demonstração não podem trocar a senha.");
        }
        if (!senhaConfere(usuario, senhaAtual)) {
            throw ApiException.badRequest("Senha atual incorreta.");
        }
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
    }

    /**
     * A versão em Python salvava a senha em texto puro. Se encontrarmos uma
     * senha assim, conferimos e já convertemos para BCrypt no mesmo login.
     */
    private boolean senhaConfere(Usuario usuario, String senha) {
        String salva = usuario.getSenhaHash();
        if (salva == null || senha == null) {
            return false;
        }
        if (salva.startsWith("$2")) {
            return passwordEncoder.matches(senha, salva);
        }
        boolean igual = MessageDigest.isEqual(salva.getBytes(StandardCharsets.UTF_8),
                senha.getBytes(StandardCharsets.UTF_8));
        if (igual) {
            usuario.setSenhaHash(passwordEncoder.encode(senha));
            usuarioRepository.save(usuario);
        }
        return igual;
    }
}
