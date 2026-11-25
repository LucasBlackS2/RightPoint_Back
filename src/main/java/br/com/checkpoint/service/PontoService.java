package br.com.checkpoint.service;

import br.com.checkpoint.domain.Contrato;
import br.com.checkpoint.domain.Endereco;
import br.com.checkpoint.domain.FotoPonto;
import br.com.checkpoint.domain.Ponto;
import br.com.checkpoint.domain.Usuario;
import br.com.checkpoint.dto.EncerrarPontoRequest;
import br.com.checkpoint.dto.LocalizacaoRequest;
import br.com.checkpoint.dto.PontoResponse;
import br.com.checkpoint.dto.ResumoHorasResponse;
import br.com.checkpoint.dto.VerificacaoLocalizacaoResponse;
import br.com.checkpoint.exception.ApiException;
import br.com.checkpoint.repository.ContratoRepository;
import br.com.checkpoint.repository.FotoPontoRepository;
import br.com.checkpoint.repository.PontoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
public class PontoService {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int TAMANHO_MAXIMO_FOTO = 5 * 1024 * 1024;

    private final PontoRepository pontoRepository;
    private final ContratoRepository contratoRepository;
    private final ContratoService contratoService;
    private final FotoPontoRepository fotoPontoRepository;
    private final QrCodeService qrCodeService;
    private final EnderecoService enderecoService;
    private final boolean exigirQrCode;
    private final boolean exigirSelfie;
    private final int maxMinutosPorRegistro;
    private final ZoneId fusoHorario;

    public PontoService(PontoRepository pontoRepository, ContratoRepository contratoRepository,
                        ContratoService contratoService, FotoPontoRepository fotoPontoRepository,
                        QrCodeService qrCodeService, EnderecoService enderecoService,
                        @Value("${checkpoint.ponto.exigir-qrcode:true}") boolean exigirQrCode,
                        @Value("${checkpoint.ponto.exigir-selfie:true}") boolean exigirSelfie,
                        @Value("${checkpoint.ponto.max-horas-por-registro}") int maxHorasPorRegistro,
                        @Value("${checkpoint.fuso-horario}") String fusoHorario) {
        this.pontoRepository = pontoRepository;
        this.contratoRepository = contratoRepository;
        this.contratoService = contratoService;
        this.fotoPontoRepository = fotoPontoRepository;
        this.qrCodeService = qrCodeService;
        this.enderecoService = enderecoService;
        this.exigirQrCode = exigirQrCode;
        this.exigirSelfie = exigirSelfie;
        this.maxMinutosPorRegistro = maxHorasPorRegistro * 60;
        this.fusoHorario = ZoneId.of(fusoHorario);
    }

    // ------------------------------------------------------------------ aluno

    @Transactional
    public PontoResponse registrarEntrada(Usuario aluno, ProvaPresenca prova) {
        if (pontoRepository.findFirstByContratoAlunoIdAndAtivoTrue(aluno.getId()).isPresent()) {
            throw ApiException.conflict("Você já tem um ponto aberto. Registre a saída antes de uma nova entrada.");
        }

        Contrato contrato = contratoAtivo(aluno);
        LocalDate hoje = LocalDate.now(fusoHorario);
        validarVigencia(contrato, hoje);

        double distancia = validarPresenca(contrato.getEndereco(), prova);

        Ponto ponto = new Ponto();
        ponto.setContrato(contrato);
        ponto.setData(hoje);
        ponto.setHoraEntrada(agoraUtc());
        ponto.setAtivo(true);
        ponto.setLatitudeEntrada(prova.latitude());
        ponto.setLongitudeEntrada(prova.longitude());
        ponto.setDistanciaEntradaMetros(arredondar(distancia, 1));
        ponto.setWifiEntrada(limparSsid(prova.wifiSsid()));
        ponto.setIpEntrada(prova.ip());
        ponto = pontoRepository.save(ponto);

        if (prova.selfie() != null) {
            salvarFoto(ponto, FotoPonto.ENTRADA, prova.selfie());
            ponto.setFotoEntrada(true);
        }
        return PontoResponse.from(ponto);
    }

    @Transactional
    public PontoResponse registrarSaida(Usuario aluno, ProvaPresenca prova) {
        Ponto ponto = pontoRepository.findFirstByContratoAlunoIdAndAtivoTrue(aluno.getId())
                .orElseThrow(() -> ApiException.notFound("Nenhum ponto em aberto para registrar a saída."));

        double distancia = validarPresenca(ponto.getContrato().getEndereco(), prova);

        LocalDateTime saida = agoraUtc();
        long minutos = Duration.between(ponto.getHoraEntrada(), saida).toMinutes();

        ponto.setHoraSaida(saida);
        // Limite por registro evita contar horas absurdas se o aluno esquecer o ponto aberto.
        ponto.setTempoTrabalhadoMinutos((int) Math.max(0, Math.min(minutos, maxMinutosPorRegistro)));
        ponto.setAtivo(false);
        ponto.setLatitudeSaida(prova.latitude());
        ponto.setLongitudeSaida(prova.longitude());
        ponto.setDistanciaSaidaMetros(arredondar(distancia, 1));
        ponto.setWifiSaida(limparSsid(prova.wifiSsid()));
        ponto.setIpSaida(prova.ip());

        if (prova.selfie() != null) {
            salvarFoto(ponto, FotoPonto.SAIDA, prova.selfie());
            ponto.setFotoSaida(true);
        }
        return PontoResponse.from(ponto);
    }

    /** Devolve a selfie de entrada ou saída de um ponto (para o aluno dono ou quem acompanha o contrato). */
    @Transactional(readOnly = true)
    public FotoPonto buscarFoto(Long idPonto, String momento, Usuario usuario) {
        Ponto ponto = pontoRepository.findById(idPonto)
                .orElseThrow(() -> ApiException.notFound("Ponto não encontrado."));
        contratoService.buscarComPermissao(ponto.getContrato().getId(), usuario);
        String m = momento == null ? "" : momento.toLowerCase();
        if (!m.equals(FotoPonto.ENTRADA) && !m.equals(FotoPonto.SAIDA)) {
            throw ApiException.badRequest("Use 'entrada' ou 'saida'.");
        }
        return fotoPontoRepository.findByPontoIdAndMomento(idPonto, m)
                .orElseThrow(() -> ApiException.notFound("Este registro não tem foto de " + m + "."));
    }

    @Transactional(readOnly = true)
    public PontoResponse pontoAberto(Usuario aluno) {
        return pontoRepository.findFirstByContratoAlunoIdAndAtivoTrue(aluno.getId())
                .map(PontoResponse::from)
                .orElseThrow(() -> ApiException.notFound("Nenhum ponto em aberto para este aluno."));
    }

    @Transactional(readOnly = true)
    public List<PontoResponse> historicoDoAluno(Usuario aluno) {
        return pontoRepository.findByContratoAlunoIdOrderByHoraEntradaDesc(aluno.getId()).stream()
                .map(PontoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ResumoHorasResponse resumoDoAluno(Usuario aluno) {
        return montarResumo(contratoAtivo(aluno));
    }

    @Transactional(readOnly = true)
    public VerificacaoLocalizacaoResponse verificarLocalizacao(Usuario aluno, LocalizacaoRequest local) {
        Endereco endereco = contratoAtivo(aluno).getEndereco();
        if (endereco.isDemonstracao()) {
            // Na demonstração o aluno "está" sempre no local.
            return new VerificacaoLocalizacaoResponse(true, 0.0, enderecoService.raioDoLocal(endereco));
        }
        double distancia = distancia(endereco, local);
        double raio = enderecoService.raioDoLocal(endereco);
        return new VerificacaoLocalizacaoResponse(distancia <= raio, arredondar(distancia, 1), raio);
    }

    // --------------------------------------------- professor / coordenação

    @Transactional(readOnly = true)
    public List<PontoResponse> historicoDoContrato(Long idContrato, Usuario usuario) {
        Contrato contrato = contratoService.buscarComPermissao(idContrato, usuario);
        return pontoRepository.findByContratoIdOrderByHoraEntradaDesc(contrato.getId()).stream()
                .map(PontoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ResumoHorasResponse resumoDoContrato(Long idContrato, Usuario usuario) {
        return montarResumo(contratoService.buscarComPermissao(idContrato, usuario));
    }

    /** Fecha um ponto que o aluno esqueceu aberto, definindo quantos minutos valem. */
    @Transactional
    public PontoResponse encerrarManualmente(Long idPonto, EncerrarPontoRequest req, Usuario responsavel) {
        Ponto ponto = pontoRepository.findById(idPonto)
                .orElseThrow(() -> ApiException.notFound("Ponto não encontrado."));
        contratoService.buscarComPermissao(ponto.getContrato().getId(), responsavel);

        if (!ponto.isAtivo()) {
            throw ApiException.conflict("Este ponto já está fechado.");
        }
        if (req.tempoTrabalhadoMinutos() > maxMinutosPorRegistro) {
            throw ApiException.badRequest("O máximo permitido por registro é " + maxMinutosPorRegistro + " minutos.");
        }

        ponto.setTempoTrabalhadoMinutos(req.tempoTrabalhadoMinutos());
        ponto.setHoraSaida(ponto.getHoraEntrada().plusMinutes(req.tempoTrabalhadoMinutos()));
        ponto.setAtivo(false);
        ponto.setEncerradoPor(responsavel.getId());
        return PontoResponse.from(ponto);
    }

    // ---------------------------------------------------------------- apoio

    private Contrato contratoAtivo(Usuario aluno) {
        return contratoRepository.findFirstByAlunoIdAndStatusTrueOrderByIdDesc(aluno.getId())
                .orElseThrow(() -> ApiException.badRequest("Você não tem nenhum contrato de estágio ativo."));
    }

    private void validarVigencia(Contrato contrato, LocalDate hoje) {
        if (contrato.getDataInicio() != null && hoje.isBefore(contrato.getDataInicio())) {
            throw ApiException.badRequest("Seu contrato só começa em " + contrato.getDataInicio().format(DATA_BR) + ".");
        }
        if (contrato.getDataFinal() != null && hoje.isAfter(contrato.getDataFinal())) {
            throw ApiException.badRequest("Seu contrato terminou em " + contrato.getDataFinal().format(DATA_BR) + ".");
        }
    }

    /**
     * Confere todas as provas de presença, na ordem: QR Code do local, GPS, Wi-Fi e selfie.
     * Em locais de demonstração o GPS e o Wi-Fi são simulados (o QR Code continua valendo).
     * Devolve a distância até o local em metros.
     */
    private double validarPresenca(Endereco endereco, ProvaPresenca prova) {
        if (exigirQrCode) {
            validarQrCode(endereco, prova.qrCode());
        }

        if (endereco.isDemonstracao()) {
            // Demonstração: localização e rede simuladas; a selfie é opcional.
            if (prova.selfie() != null) validarImagem(prova.selfie());
            return 0.0;
        }

        double distancia = validarDentroDoRaio(endereco, new LocalizacaoRequest(prova.latitude(), prova.longitude()));
        validarRede(endereco, prova);

        if (prova.selfie() == null) {
            if (exigirSelfie) {
                throw ApiException.badRequest("Tire uma selfie no local para comprovar a sua presença.");
            }
        } else {
            validarImagem(prova.selfie());
        }
        return distancia;
    }

    /**
     * Aceita o QR Code ao vivo (celular do professor, muda a cada 30 s) ou o
     * impresso, se o professor não tiver desligado o impresso para este local.
     */
    private void validarQrCode(Endereco endereco, String qrCode) {
        String esperado = endereco.getQrToken();
        if (esperado == null) {
            throw ApiException.badRequest("O local do estágio ainda não tem QR Code. "
                    + "Peça ao professor para gerar o QR Code do local.");
        }
        if (qrCodeService.ehAoVivo(qrCode)) {
            if (!qrCodeService.validarAoVivo(qrCode, endereco.getId(), esperado, Instant.now())) {
                throw ApiException.badRequest("Este QR Code ao vivo expirou ou não é do seu local de estágio. "
                        + "Escaneie de novo o QR Code na tela do professor.");
            }
            return;
        }
        String lido = qrCodeService.extrairToken(qrCode);
        if (lido == null || !MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
                lido.getBytes(StandardCharsets.UTF_8))) {
            throw ApiException.badRequest("QR Code inválido. Escaneie o QR Code do local do seu estágio.");
        }
        if (!endereco.isAceitaQrImpresso()) {
            throw ApiException.badRequest("Este local só aceita o QR Code ao vivo, mostrado no celular do professor.");
        }
    }

    /** O celular precisa estar no Wi-Fi do local (nome da rede, MAC do roteador e/ou IP público). */
    private void validarRede(Endereco endereco, ProvaPresenca prova) {
        if (endereco.getWifiSsid() != null) {
            String ssid = limparSsid(prova.wifiSsid());
            if (ssid == null || !ssid.equalsIgnoreCase(endereco.getWifiSsid().trim())) {
                throw ApiException.badRequest("Conecte-se ao Wi-Fi do local do estágio (rede \""
                        + endereco.getWifiSsid() + "\") para registrar o ponto.");
            }
        }
        if (endereco.getWifiBssid() != null) {
            String bssid = EnderecoService.normalizarBssid(prova.wifiBssid());
            if (bssid == null || !bssid.equals(endereco.getWifiBssid())) {
                throw ApiException.badRequest("Você não está conectado ao roteador Wi-Fi do local do estágio.");
            }
        }
        if (endereco.getIpPublico() != null) {
            List<String> permitidos = Arrays.stream(endereco.getIpPublico().split(","))
                    .map(String::trim).filter(ip -> !ip.isEmpty()).toList();
            if (prova.ip() == null || !permitidos.contains(prova.ip())) {
                throw ApiException.badRequest("Você precisa estar conectado à internet (Wi-Fi) do local "
                        + "do estágio para registrar o ponto.");
            }
        }
    }

    private void validarImagem(ProvaPresenca.Selfie selfie) {
        byte[] d = selfie.dados();
        if (d == null || d.length < 100) {
            throw ApiException.badRequest("A selfie enviada está vazia. Tire a foto novamente.");
        }
        if (d.length > TAMANHO_MAXIMO_FOTO) {
            throw ApiException.badRequest("A selfie é muito grande (máximo 5 MB).");
        }
        boolean jpeg = (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8;
        boolean png = (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G';
        boolean webp = d.length > 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P';
        if (!jpeg && !png && !webp) {
            throw ApiException.badRequest("A selfie precisa ser uma imagem JPG, PNG ou WEBP.");
        }
    }

    private void salvarFoto(Ponto ponto, String momento, ProvaPresenca.Selfie selfie) {
        FotoPonto foto = new FotoPonto();
        foto.setPonto(ponto);
        foto.setMomento(momento);
        byte[] d = selfie.dados();
        String tipo = (d[0] & 0xFF) == 0x89 ? "image/png" : d[0] == 'R' ? "image/webp" : "image/jpeg";
        foto.setContentType(tipo);
        foto.setDados(d);
        foto.setCriadoEm(agoraUtc());
        fotoPontoRepository.save(foto);
    }

    /** Android às vezes devolve o nome com aspas ou "<unknown ssid>". */
    static String limparSsid(String ssid) {
        if (ssid == null) return null;
        String s = ssid.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        if (s.isEmpty() || s.equalsIgnoreCase("<unknown ssid>")) return null;
        return s;
    }

    private double validarDentroDoRaio(Endereco endereco, LocalizacaoRequest local) {
        double distancia = distancia(endereco, local);
        double raio = enderecoService.raioDoLocal(endereco);
        if (distancia > raio) {
            throw ApiException.badRequest(String.format(
                    "Você está a %.0f m do local do estágio. O ponto só pode ser registrado a até %.0f m.",
                    distancia, raio));
        }
        return distancia;
    }

    private double distancia(Endereco endereco, LocalizacaoRequest local) {
        if (!endereco.temCoordenadas()) {
            throw ApiException.badRequest("O endereço do estágio ainda não tem coordenadas cadastradas. "
                    + "Peça ao professor ou à coordenação para atualizar o endereço.");
        }
        return GeoUtils.distanciaMetros(endereco.getLatitude(), endereco.getLongitude(),
                local.latitude(), local.longitude());
    }

    private ResumoHorasResponse montarResumo(Contrato contrato) {
        long minutos = pontoRepository.somarMinutosPorContrato(contrato.getId()).longValue();
        double horas = arredondar(minutos / 60.0, 2);
        Integer carga = contrato.getCargaHorariaTotal();

        Double restantes = null;
        Double percentual = null;
        if (carga != null && carga > 0) {
            restantes = arredondar(Math.max(0, carga - minutos / 60.0), 2);
            percentual = arredondar(Math.min(100.0, minutos / (carga * 60.0) * 100.0), 1);
        }

        boolean aberto = pontoRepository.existsByContratoIdAndAtivoTrue(contrato.getId());
        return new ResumoHorasResponse(contrato.getId(), minutos, horas, carga, restantes, percentual, aberto);
    }

    private static LocalDateTime agoraUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private static double arredondar(double valor, int casas) {
        double fator = Math.pow(10, casas);
        return Math.round(valor * fator) / fator;
    }
}
