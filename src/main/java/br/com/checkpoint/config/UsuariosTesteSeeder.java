package br.com.checkpoint.config;

import br.com.checkpoint.domain.Contrato;
import br.com.checkpoint.domain.Endereco;
import br.com.checkpoint.domain.TipoUsuario;
import br.com.checkpoint.domain.Usuario;
import br.com.checkpoint.repository.ContratoRepository;
import br.com.checkpoint.repository.EnderecoRepository;
import br.com.checkpoint.repository.UsuarioRepository;
import br.com.checkpoint.service.QrCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Cria, ao iniciar, um professor e um aluno de teste (com senha) definidos no
 * .env, e um contrato de 300 h ligando os dois. Útil para testar o app com
 * contas reais. Se as variáveis estiverem vazias, não faz nada.
 */
@Component
@Order(2)
public class UsuariosTesteSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuariosTesteSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final EnderecoRepository enderecoRepository;
    private final ContratoRepository contratoRepository;
    private final PasswordEncoder passwordEncoder;
    private final QrCodeService qrCodeService;
    private final ZoneId fusoHorario;

    private final String profMatricula;
    private final String profSenha;
    private final String profNome;
    private final String alunoMatricula;
    private final String alunoSenha;
    private final String alunoNome;

    public UsuariosTesteSeeder(UsuarioRepository usuarioRepository, EnderecoRepository enderecoRepository,
                               ContratoRepository contratoRepository, PasswordEncoder passwordEncoder,
                               QrCodeService qrCodeService,
                               @Value("${checkpoint.fuso-horario}") String fusoHorario,
                               @Value("${checkpoint.teste.professor.matricula:}") String profMatricula,
                               @Value("${checkpoint.teste.professor.senha:}") String profSenha,
                               @Value("${checkpoint.teste.professor.nome:Professor Teste}") String profNome,
                               @Value("${checkpoint.teste.aluno.matricula:}") String alunoMatricula,
                               @Value("${checkpoint.teste.aluno.senha:}") String alunoSenha,
                               @Value("${checkpoint.teste.aluno.nome:Aluno Teste}") String alunoNome) {
        this.usuarioRepository = usuarioRepository;
        this.enderecoRepository = enderecoRepository;
        this.contratoRepository = contratoRepository;
        this.passwordEncoder = passwordEncoder;
        this.qrCodeService = qrCodeService;
        this.fusoHorario = ZoneId.of(fusoHorario);
        this.profMatricula = profMatricula.trim();
        this.profSenha = profSenha;
        this.profNome = profNome;
        this.alunoMatricula = alunoMatricula.trim();
        this.alunoSenha = alunoSenha;
        this.alunoNome = alunoNome;
    }

    @Override
    public void run(ApplicationArguments args) {
        Usuario professor = garantir(profMatricula, profSenha, profNome, TipoUsuario.PROFESSOR);
        Usuario aluno = garantir(alunoMatricula, alunoSenha, alunoNome, TipoUsuario.ALUNO);
        if (professor == null || aluno == null) {
            return;
        }
        if (contratoRepository.existsByAlunoIdAndStatusTrue(aluno.getId())) {
            return;
        }

        // Local de teste sem posição: o professor define com "Definir aqui como local válido".
        Endereco local = new Endereco();
        local.setLogradouro("Local de teste (defina o local válido no app)");
        local.setCidade("Caruaru");
        local.setEstado("PE");
        local.setQrToken(qrCodeService.novoToken());
        local = enderecoRepository.save(local);

        Contrato contrato = new Contrato();
        contrato.setAluno(aluno);
        contrato.setProfessor(professor);
        contrato.setEndereco(local);
        contrato.setDataInicio(LocalDate.now(fusoHorario));
        contrato.setDataFinal(LocalDate.now(fusoHorario).plusMonths(6));
        contrato.setCargaHorariaTotal(300);
        contratoRepository.save(contrato);
        log.info("Contrato de teste criado entre '{}' e '{}'.", professor.getMatricula(), aluno.getMatricula());
    }

    /** Cria o usuário se ainda não existir; se já existir, só devolve (a senha não é trocada). */
    private Usuario garantir(String matricula, String senha, String nome, TipoUsuario tipo) {
        if (matricula.isEmpty() || senha == null || senha.isBlank()) {
            return null;
        }
        var existente = usuarioRepository.findByMatricula(matricula);
        if (existente.isPresent()) {
            if (existente.get().getTipoAcesso() != tipo) {
                log.warn("A matrícula '{}' já existe com outro perfil; conta de teste não criada.", matricula);
                return null;
            }
            return existente.get();
        }
        Usuario u = new Usuario();
        u.setMatricula(matricula);
        u.setNome(nome);
        u.setEmail(matricula + "@checkpoint.local");
        u.setTipoAcesso(tipo);
        u.setTurma(tipo == TipoUsuario.ALUNO ? "Teste" : null);
        u.setSenhaHash(passwordEncoder.encode(senha));
        u = usuarioRepository.save(u);
        log.info("Conta de teste criada: {} '{}'.", tipo.getValor(), matricula);
        return u;
    }
}
