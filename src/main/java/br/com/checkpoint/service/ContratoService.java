package br.com.checkpoint.service;

import br.com.checkpoint.domain.Contrato;
import br.com.checkpoint.domain.Endereco;
import br.com.checkpoint.domain.TipoUsuario;
import br.com.checkpoint.domain.Usuario;
import br.com.checkpoint.dto.ContratoRequest;
import br.com.checkpoint.dto.ContratoResponse;
import br.com.checkpoint.exception.ApiException;
import br.com.checkpoint.repository.ContratoRepository;
import br.com.checkpoint.repository.EnderecoRepository;
import br.com.checkpoint.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EnderecoRepository enderecoRepository;

    public ContratoService(ContratoRepository contratoRepository, UsuarioRepository usuarioRepository,
                           EnderecoRepository enderecoRepository) {
        this.contratoRepository = contratoRepository;
        this.usuarioRepository = usuarioRepository;
        this.enderecoRepository = enderecoRepository;
    }

    @Transactional
    public ContratoResponse criar(ContratoRequest req, Usuario criador) {
        Usuario aluno = usuarioRepository.findById(req.idAluno())
                .orElseThrow(() -> ApiException.notFound("Aluno não encontrado."));
        if (aluno.getTipoAcesso() != TipoUsuario.ALUNO) {
            throw ApiException.badRequest("O usuário informado em id_aluno não é um aluno.");
        }

        Usuario professor = usuarioRepository.findById(req.idProfessor())
                .orElseThrow(() -> ApiException.notFound("Professor não encontrado."));
        if (professor.getTipoAcesso() != TipoUsuario.PROFESSOR) {
            throw ApiException.badRequest("O usuário informado em id_professor não é um professor.");
        }
        if (criador.getTipoAcesso() == TipoUsuario.PROFESSOR && !criador.getId().equals(professor.getId())) {
            throw ApiException.forbidden("Professor só pode criar contratos em que ele mesmo é o orientador.");
        }

        Endereco endereco = enderecoRepository.findById(req.idEndereco())
                .orElseThrow(() -> ApiException.notFound("Endereço não encontrado."));

        boolean demo = criador.isDemonstracao();
        if (aluno.isDemonstracao() != demo || professor.isDemonstracao() != demo || endereco.isDemonstracao() != demo) {
            throw ApiException.badRequest("Misture apenas dados de demonstração com dados de demonstração.");
        }

        if (req.dataInicio() != null && req.dataFinal() != null && req.dataFinal().isBefore(req.dataInicio())) {
            throw ApiException.badRequest("A data final não pode ser antes da data de início.");
        }
        if (contratoRepository.existsByAlunoIdAndStatusTrue(aluno.getId())) {
            throw ApiException.conflict("Este aluno já tem um contrato ativo. Encerre-o antes de criar outro.");
        }

        Contrato contrato = new Contrato();
        contrato.setAluno(aluno);
        contrato.setProfessor(professor);
        contrato.setEndereco(endereco);
        contrato.setDataInicio(req.dataInicio());
        contrato.setDataFinal(req.dataFinal());
        contrato.setCargaHorariaTotal(req.cargaHorariaTotal());
        contrato.setStatus(true);

        return ContratoResponse.from(contratoRepository.save(contrato));
    }

    /** Cada perfil só enxerga o que é dele. */
    @Transactional(readOnly = true)
    public List<ContratoResponse> listar(Usuario usuario) {
        List<Contrato> contratos = switch (usuario.getTipoAcesso()) {
            case ALUNO -> contratoRepository.findByAlunoIdOrderByIdDesc(usuario.getId());
            case PROFESSOR -> contratoRepository.findByProfessorIdOrderByIdDesc(usuario.getId());
            case COORDENADOR, ADMIN -> contratoRepository.findByAlunoDemonstracaoOrderByIdDesc(usuario.isDemonstracao());
        };
        return contratos.stream().map(ContratoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ContratoResponse buscar(Long id, Usuario usuario) {
        return ContratoResponse.from(buscarComPermissao(id, usuario));
    }

    @Transactional
    public ContratoResponse alterarStatus(Long id, boolean ativo, Usuario usuario) {
        Contrato contrato = buscarComPermissao(id, usuario);
        if (ativo && contratoRepository.existsByAlunoIdAndStatusTrueAndIdNot(contrato.getAluno().getId(), id)) {
            throw ApiException.conflict("O aluno já tem outro contrato ativo.");
        }
        contrato.setStatus(ativo);
        return ContratoResponse.from(contrato);
    }

    /** Busca o contrato e garante que o usuário pode vê-lo (404 se não existir, 403 se não for dele). */
    @Transactional(readOnly = true)
    public Contrato buscarComPermissao(Long id, Usuario usuario) {
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Contrato não encontrado."));
        if (!podeAcessar(contrato, usuario)) {
            throw ApiException.forbidden("Você não tem acesso a este contrato.");
        }
        return contrato;
    }

    static boolean podeAcessar(Contrato contrato, Usuario usuario) {
        return switch (usuario.getTipoAcesso()) {
            case ADMIN, COORDENADOR -> true;
            case PROFESSOR -> contrato.getProfessor().getId().equals(usuario.getId());
            case ALUNO -> contrato.getAluno().getId().equals(usuario.getId());
        };
    }
}
