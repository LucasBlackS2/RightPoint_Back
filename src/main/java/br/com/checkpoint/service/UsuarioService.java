package br.com.checkpoint.service;

import br.com.checkpoint.domain.TipoUsuario;
import br.com.checkpoint.domain.Usuario;
import br.com.checkpoint.dto.UsuarioCreateRequest;
import br.com.checkpoint.dto.UsuarioResponse;
import br.com.checkpoint.exception.ApiException;
import br.com.checkpoint.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse criar(UsuarioCreateRequest req, Usuario criador) {
        TipoUsuario tipo = req.tipoAcesso() == null ? TipoUsuario.ALUNO : req.tipoAcesso();
        validarPermissaoDeCriar(criador.getTipoAcesso(), tipo);

        String matricula = req.matricula().trim();
        String email = req.email().trim().toLowerCase();
        String contato = vazioParaNull(req.contato());

        if (usuarioRepository.existsByMatricula(matricula)) {
            throw ApiException.conflict("Matrícula já cadastrada.");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("E-mail já cadastrado.");
        }
        if (contato != null && usuarioRepository.existsByContato(contato)) {
            throw ApiException.conflict("Número de telefone já cadastrado.");
        }

        Usuario novo = new Usuario();
        novo.setNome(req.nome().trim());
        novo.setMatricula(matricula);
        novo.setEmail(email);
        novo.setContato(contato);
        novo.setTurma(vazioParaNull(req.turma()));
        novo.setTipoAcesso(tipo);
        novo.setSenhaHash(passwordEncoder.encode(req.senha()));
        // Usuários criados por contas de demonstração também são de demonstração.
        novo.setDemonstracao(criador.isDemonstracao());

        return UsuarioResponse.from(usuarioRepository.save(novo));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(String tipo, Usuario solicitante) {
        // Contas de demonstração só enxergam dados de demonstração (e vice-versa).
        boolean demo = solicitante.isDemonstracao();
        List<Usuario> usuarios = (tipo == null || tipo.isBlank())
                ? usuarioRepository.findByDemonstracaoOrderByNomeAsc(demo)
                : usuarioRepository.findByTipoAcessoAndDemonstracaoOrderByNomeAsc(TipoUsuario.of(tipo), demo);
        return usuarios.stream().map(UsuarioResponse::from).toList();
    }

    /**
     * Admin cria qualquer perfil; coordenador cria todos menos admin;
     * professor só cria aluno; aluno não cria ninguém.
     */
    private void validarPermissaoDeCriar(TipoUsuario criador, TipoUsuario novo) {
        boolean permitido = switch (criador) {
            case ADMIN -> true;
            case COORDENADOR -> novo != TipoUsuario.ADMIN;
            case PROFESSOR -> novo == TipoUsuario.ALUNO;
            case ALUNO -> false;
        };
        if (!permitido) {
            throw ApiException.forbidden("Seu perfil não pode criar usuários do tipo '" + novo.getValor() + "'.");
        }
    }

    private static String vazioParaNull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
