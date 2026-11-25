package br.com.checkpoint.service;

import br.com.checkpoint.domain.Endereco;
import br.com.checkpoint.domain.Usuario;
import br.com.checkpoint.dto.EnderecoRequest;
import br.com.checkpoint.dto.EnderecoResponse;
import br.com.checkpoint.dto.LocalValidoRequest;
import br.com.checkpoint.dto.QrAoVivoResponse;
import br.com.checkpoint.exception.ApiException;
import br.com.checkpoint.repository.EnderecoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

@Service
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final GeocodingService geocodingService;
    private final QrCodeService qrCodeService;
    private final double raioPadrao;

    public EnderecoService(EnderecoRepository enderecoRepository, GeocodingService geocodingService,
                           QrCodeService qrCodeService,
                           @Value("${checkpoint.geofence.raio-metros}") double raioPadrao) {
        this.enderecoRepository = enderecoRepository;
        this.geocodingService = geocodingService;
        this.qrCodeService = qrCodeService;
        this.raioPadrao = raioPadrao;
    }

    /** Raio válido do local: o definido pelo professor ou o padrão do servidor. */
    public double raioDoLocal(Endereco e) {
        return e.getRaioMetros() != null ? e.getRaioMetros() : raioPadrao;
    }

    @Transactional(readOnly = true)
    public EnderecoResponse buscar(Long id, Usuario solicitante) {
        return EnderecoResponse.from(buscarComPermissao(id, solicitante));
    }

    /**
     * O professor define onde o QR Code vale. Normalmente ele está no local
     * e envia a posição atual do celular, mais o raio desejado.
     */
    @Transactional
    public EnderecoResponse definirLocalValido(Long id, LocalValidoRequest req, Usuario solicitante) {
        Endereco e = buscarComPermissao(id, solicitante);
        if ((req.latitude() == null) != (req.longitude() == null)) {
            throw ApiException.badRequest("Envie latitude e longitude juntas.");
        }
        if (req.latitude() != null) {
            e.setLatitude(req.latitude());
            e.setLongitude(req.longitude());
        }
        if (req.raioMetros() != null) e.setRaioMetros(req.raioMetros());
        if (req.aceitaQrImpresso() != null) e.setAceitaQrImpresso(req.aceitaQrImpresso());
        return EnderecoResponse.from(e);
    }

    /**
     * QR Code ao vivo: só é gerado se o professor estiver dentro do local válido.
     * O app chama de novo a cada poucos segundos, conferindo a posição de novo.
     */
    @Transactional
    public QrAoVivoResponse qrAoVivo(Long id, double latitude, double longitude, Usuario solicitante) {
        Endereco e = buscarComPermissao(id, solicitante);
        double raio = raioDoLocal(e);
        double distancia = 0.0;
        if (!e.isDemonstracao()) {
            if (!e.temCoordenadas()) {
                throw ApiException.badRequest("Defina primeiro o local válido deste endereço "
                        + "(toque em \"Definir aqui como local válido\" estando no local).");
            }
            distancia = GeoUtils.distanciaMetros(e.getLatitude(), e.getLongitude(), latitude, longitude);
            if (distancia > raio) {
                throw ApiException.forbidden(String.format(
                        "Você está a %.0f m do local válido. O QR Code ao vivo só aparece a até %.0f m de lá.",
                        distancia, raio));
            }
        }
        if (e.getQrToken() == null) {
            e.setQrToken(qrCodeService.novoToken());
        }
        Instant agora = Instant.now();
        String conteudo = qrCodeService.gerarAoVivo(e.getId(), e.getQrToken(), agora);
        String png = Base64.getEncoder().encodeToString(qrCodeService.gerarPng(conteudo, 500));
        return new QrAoVivoResponse(conteudo, png, qrCodeService.fimDaJanela(agora).atOffset(ZoneOffset.UTC),
                qrCodeService.getJanelaSegundos(), Math.round(distancia * 10) / 10.0, raio);
    }

    @Transactional
    public EnderecoResponse criar(EnderecoRequest req, Usuario solicitante) {
        Endereco endereco = new Endereco();
        preencher(endereco, req);
        endereco.setQrToken(qrCodeService.novoToken());
        // Locais criados por contas de demonstração também são de demonstração.
        endereco.setDemonstracao(solicitante.isDemonstracao());
        return EnderecoResponse.from(enderecoRepository.save(endereco));
    }

    @Transactional
    public EnderecoResponse atualizar(Long id, EnderecoRequest req, Usuario solicitante) {
        Endereco endereco = buscarComPermissao(id, solicitante);
        preencher(endereco, req);
        return EnderecoResponse.from(endereco);
    }

    @Transactional(readOnly = true)
    public List<EnderecoResponse> listar(Usuario solicitante) {
        return enderecoRepository.findByDemonstracaoOrderByIdDesc(solicitante.isDemonstracao()).stream()
                .map(EnderecoResponse::from).toList();
    }

    /** Imagem PNG do QR Code do local, para imprimir e fixar na empresa. */
    @Transactional
    public byte[] qrCodePng(Long id, Usuario solicitante) {
        Endereco endereco = buscarComPermissao(id, solicitante);
        if (endereco.getQrToken() == null) {
            // Locais cadastrados antes desta versão ainda não tinham QR Code.
            endereco.setQrToken(qrCodeService.novoToken());
        }
        return qrCodeService.gerarPng(qrCodeService.conteudo(endereco.getQrToken()), 600);
    }

    /** Gera um QR Code novo; o antigo deixa de funcionar (útil se alguém fotografou o QR). */
    @Transactional
    public void gerarNovoQrCode(Long id, Usuario solicitante) {
        buscarComPermissao(id, solicitante).setQrToken(qrCodeService.novoToken());
    }

    private Endereco buscarComPermissao(Long id, Usuario solicitante) {
        Endereco endereco = enderecoRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Endereço não encontrado."));
        if (endereco.isDemonstracao() != solicitante.isDemonstracao()) {
            throw ApiException.forbidden("Você não tem acesso a este endereço.");
        }
        return endereco;
    }

    private void preencher(Endereco e, EnderecoRequest req) {
        if ((req.latitude() == null) != (req.longitude() == null)) {
            throw ApiException.badRequest("Envie latitude e longitude juntas (ou nenhuma das duas).");
        }

        e.setCep(req.cep());
        e.setLogradouro(req.logradouro().trim());
        e.setCidade(req.cidade().trim());
        e.setEstado(req.estado().trim());
        e.setNumero(req.numero());
        e.setBairro(req.bairro());
        e.setWifiSsid(vazioParaNull(req.wifiSsid()));
        e.setWifiBssid(normalizarBssid(req.wifiBssid()));
        e.setIpPublico(vazioParaNull(req.ipPublico()));
        e.setRaioMetros(req.raioMetros());
        if (req.aceitaQrImpresso() != null) e.setAceitaQrImpresso(req.aceitaQrImpresso());

        if (req.latitude() != null) {
            // Coordenadas informadas manualmente (ex.: copiadas do Google Maps ou do GPS).
            e.setLatitude(req.latitude());
            e.setLongitude(req.longitude());
        } else {
            // Sem coordenadas: tenta descobrir pelo Google. Se falhar, fica sem e o ponto avisa.
            geocodingService.buscarCoordenadas(e.enderecoCompleto()).ifPresentOrElse(c -> {
                e.setLatitude(c.latitude());
                e.setLongitude(c.longitude());
            }, () -> {
                e.setLatitude(null);
                e.setLongitude(null);
            });
        }
    }

    static String vazioParaNull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    /** "AA-BB-CC-DD-EE-FF" -> "aa:bb:cc:dd:ee:ff" */
    static String normalizarBssid(String valor) {
        String v = vazioParaNull(valor);
        return v == null ? null : v.toLowerCase().replace('-', ':');
    }
}
