package br.com.checkpoint.service;

import br.com.checkpoint.domain.Contrato;
import br.com.checkpoint.domain.Endereco;
import br.com.checkpoint.domain.FotoPonto;
import br.com.checkpoint.domain.Matriculas;
import br.com.checkpoint.domain.Ponto;
import br.com.checkpoint.domain.TipoUsuario;
import br.com.checkpoint.domain.Usuario;
import br.com.checkpoint.dto.DemoResponse;
import br.com.checkpoint.exception.ApiException;
import br.com.checkpoint.repository.ContratoRepository;
import br.com.checkpoint.repository.EnderecoRepository;
import br.com.checkpoint.repository.FotoPontoRepository;
import br.com.checkpoint.repository.PontoRepository;
import br.com.checkpoint.repository.UsuarioRepository;
import br.com.checkpoint.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

/**
 * Modo demonstração: contas e dados de exemplo para mostrar o app funcionando
 * (um aluno, um professor, um local, um contrato e vários registros de ponto).
 * Essas contas só enxergam dados de demonstração.
 */
@Service
public class DemoService {

    private static final Logger log = LoggerFactory.getLogger(DemoService.class);

    public static final String MATRICULA_ALUNO = "90000001";
    public static final String MATRICULA_PROFESSOR = "90000002";

    private static final Map<String, String> PERFIS = Map.of(
            "aluno", MATRICULA_ALUNO,
            "professor", MATRICULA_PROFESSOR);

    private final boolean habilitado;
    private final UsuarioRepository usuarioRepository;
    private final EnderecoRepository enderecoRepository;
    private final ContratoRepository contratoRepository;
    private final PontoRepository pontoRepository;
    private final FotoPontoRepository fotoPontoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final QrCodeService qrCodeService;
    private final ZoneId fusoHorario;

    public DemoService(@Value("${checkpoint.demo.habilitado:true}") boolean habilitado,
                       UsuarioRepository usuarioRepository, EnderecoRepository enderecoRepository,
                       ContratoRepository contratoRepository, PontoRepository pontoRepository,
                       FotoPontoRepository fotoPontoRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, QrCodeService qrCodeService,
                       @Value("${checkpoint.fuso-horario}") String fusoHorario) {
        this.habilitado = habilitado;
        this.usuarioRepository = usuarioRepository;
        this.enderecoRepository = enderecoRepository;
        this.contratoRepository = contratoRepository;
        this.pontoRepository = pontoRepository;
        this.fotoPontoRepository = fotoPontoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.qrCodeService = qrCodeService;
        this.fusoHorario = ZoneId.of(fusoHorario);
    }

    public boolean isHabilitado() {
        return habilitado;
    }

    /** Entra numa conta de demonstração sem senha. */
    @Transactional
    public DemoResponse entrar(String perfil) {
        if (!habilitado) {
            throw ApiException.notFound("O modo demonstração está desligado neste servidor.");
        }
        String matricula = PERFIS.get(perfil == null ? "" : perfil.trim().toLowerCase());
        if (matricula == null) {
            throw ApiException.badRequest("Perfil de demonstração inválido. Use 'aluno' ou 'professor'.");
        }
        Contrato contrato = garantirDados();
        Usuario usuario = usuarioRepository.findByMatricula(matricula).orElseThrow();
        return new DemoResponse(jwtService.gerarToken(usuario), "bearer", jwtService.getExpiracaoSegundos(),
                usuario.getTipoAcesso().getValor(), contrato.getId(), contrato.getEndereco().getId());
    }

    /**
     * Na demonstração o aluno não está perto de um QR Code de verdade:
     * o app usa este conteúdo para "simular" a leitura.
     */
    @Transactional(readOnly = true)
    public String qrCodeSimulado(Usuario usuario) {
        if (!usuario.isDemonstracao()) {
            throw ApiException.forbidden("A simulação de QR Code só existe no modo demonstração.");
        }
        Endereco endereco = contratoRepository.findFirstByAlunoIdAndStatusTrueOrderByIdDesc(usuario.getId())
                .map(Contrato::getEndereco)
                .orElseThrow(() -> ApiException.badRequest("A conta de demonstração não tem contrato ativo."));
        return qrCodeService.conteudo(endereco.getQrToken());
    }

    /** Cria (uma única vez) todos os dados de demonstração e devolve o contrato de exemplo. */
    @Transactional
    public Contrato garantirDados() {
        // Matrículas antigas das contas de demonstração (antes da regra "só números").
        Matriculas.migrar(usuarioRepository, "demo.aluno", MATRICULA_ALUNO, TipoUsuario.ALUNO, log);
        Matriculas.migrar(usuarioRepository, "demo.professor", MATRICULA_PROFESSOR, TipoUsuario.PROFESSOR, log);
        Usuario professor = usuarioRepository.findByMatricula(MATRICULA_PROFESSOR)
                .orElseGet(() -> criarUsuario(MATRICULA_PROFESSOR, "Prof.ª Ana Demonstração",
                        "professor.demo@checkpoint.local", TipoUsuario.PROFESSOR, null));
        Usuario aluno = usuarioRepository.findByMatricula(MATRICULA_ALUNO)
                .orElseGet(() -> criarUsuario(MATRICULA_ALUNO, "Lucas Estagiário (demo)",
                        "aluno.demo@checkpoint.local", TipoUsuario.ALUNO, "ADS - Demonstração"));

        var existente = contratoRepository.findFirstByAlunoIdAndStatusTrueOrderByIdDesc(aluno.getId());
        if (existente.isPresent()) {
            return existente.get();
        }

        Endereco local = enderecoRepository.findFirstByDemonstracaoTrueOrderByIdAsc().orElseGet(() -> {
            Endereco e = new Endereco();
            e.setLogradouro("Av. Agamenon Magalhães (Empresa Demonstração)");
            e.setNumero("100");
            e.setBairro("Maurício de Nassau");
            e.setCidade("Caruaru");
            e.setEstado("PE");
            e.setCep("55012-000");
            e.setLatitude(-8.2835);
            e.setLongitude(-35.9761);
            e.setWifiSsid("CheckPoint-Demo");
            e.setQrToken(qrCodeService.novoToken());
            e.setDemonstracao(true);
            return enderecoRepository.save(e);
        });

        LocalDate hoje = LocalDate.now(fusoHorario);
        Contrato contrato = new Contrato();
        contrato.setAluno(aluno);
        contrato.setProfessor(professor);
        contrato.setEndereco(local);
        contrato.setDataInicio(hoje.minusDays(40));
        contrato.setDataFinal(hoje.plusDays(120));
        contrato.setCargaHorariaTotal(300);
        contrato.setStatus(true);
        contrato = contratoRepository.save(contrato);

        criarHistorico(contrato, hoje);
        log.info("Dados de demonstração criados (aluno '{}', professor '{}').", MATRICULA_ALUNO, MATRICULA_PROFESSOR);
        return contrato;
    }

    private Usuario criarUsuario(String matricula, String nome, String email, TipoUsuario tipo, String turma) {
        Usuario u = new Usuario();
        u.setMatricula(matricula);
        u.setNome(nome);
        u.setEmail(email);
        u.setTurma(turma);
        u.setTipoAcesso(tipo);
        u.setDemonstracao(true);
        // Senha aleatória: contas de demonstração só entram pelo botão "Conhecer o app".
        u.setSenhaHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        return usuarioRepository.save(u);
    }

    /** Registros dos últimos dias úteis, com uma "selfie" ilustrativa em cada entrada. */
    private void criarHistorico(Contrato contrato, LocalDate hoje) {
        int[] minutosPorDia = {300, 360, 280, 330, 360, 240, 315, 345, 300, 360, 290, 330};
        byte[] avatar = gerarAvatar();
        LocalDate dia = hoje.minusDays(1);
        int criados = 0;
        while (criados < minutosPorDia.length) {
            if (dia.getDayOfWeek() != DayOfWeek.SATURDAY && dia.getDayOfWeek() != DayOfWeek.SUNDAY) {
                int minutos = minutosPorDia[criados];
                // 08:00 em Recife = 11:00 UTC
                LocalDateTime entrada = dia.atTime(11, 0).plusMinutes((criados * 7) % 25);
                Ponto p = new Ponto();
                p.setContrato(contrato);
                p.setData(dia);
                p.setHoraEntrada(entrada);
                p.setHoraSaida(entrada.plusMinutes(minutos));
                p.setTempoTrabalhadoMinutos(minutos);
                p.setAtivo(false);
                p.setLatitudeEntrada(contrato.getEndereco().getLatitude());
                p.setLongitudeEntrada(contrato.getEndereco().getLongitude());
                p.setDistanciaEntradaMetros(12.0 + criados % 5 * 9);
                p.setDistanciaSaidaMetros(8.0 + criados % 4 * 11);
                p.setWifiEntrada("CheckPoint-Demo");
                p.setWifiSaida("CheckPoint-Demo");
                p.setFotoEntrada(true);
                p = pontoRepository.save(p);

                FotoPonto foto = new FotoPonto();
                foto.setPonto(p);
                foto.setMomento(FotoPonto.ENTRADA);
                foto.setContentType("image/png");
                foto.setDados(avatar);
                foto.setCriadoEm(entrada);
                fotoPontoRepository.save(foto);
                criados++;
            }
            dia = dia.minusDays(1);
        }
    }

    /** Desenho simples de uma pessoa, usado como selfie de exemplo (sem texto, para não depender de fontes). */
    private static byte[] gerarAvatar() {
        BufferedImage img = new BufferedImage(320, 320, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xE8EEFF));
        g.fillRect(0, 0, 320, 320);
        g.setColor(new Color(0x1E5EFF));
        g.fillOval(100, 60, 120, 120);
        g.fillRoundRect(60, 190, 200, 160, 100, 100);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(6));
        g.drawArc(130, 105, 60, 45, 200, 140);
        g.fillOval(130, 100, 14, 14);
        g.fillOval(176, 100, 14, 14);
        g.dispose();
        try {
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            ImageIO.write(img, "png", saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
