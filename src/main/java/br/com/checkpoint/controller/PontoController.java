package br.com.checkpoint.controller;

import br.com.checkpoint.domain.FotoPonto;
import br.com.checkpoint.domain.Usuario;
import br.com.checkpoint.dto.EncerrarPontoRequest;
import br.com.checkpoint.dto.LocalizacaoRequest;
import br.com.checkpoint.dto.PontoResponse;
import br.com.checkpoint.dto.RegistroPontoRequest;
import br.com.checkpoint.dto.ResumoHorasResponse;
import br.com.checkpoint.dto.VerificacaoLocalizacaoResponse;
import br.com.checkpoint.exception.ApiException;
import br.com.checkpoint.service.PontoService;
import br.com.checkpoint.service.ProvaPresenca;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/ponto")
@Tag(name = "Ponto eletrônico")
public class PontoController {

    private final PontoService pontoService;

    public PontoController(PontoService pontoService) {
        this.pontoService = pontoService;
    }

    @Operation(summary = "Registrar entrada pelo app: multipart com latitude, longitude, qr_code, wifi_ssid, "
            + "wifi_bssid e o arquivo foto (selfie)")
    @PostMapping(path = "/entrada", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ALUNO')")
    public PontoResponse entradaComFoto(@RequestParam("latitude") Double latitude,
                                        @RequestParam("longitude") Double longitude,
                                        @RequestParam(value = "qr_code", required = false) String qrCode,
                                        @RequestParam(value = "wifi_ssid", required = false) String wifiSsid,
                                        @RequestParam(value = "wifi_bssid", required = false) String wifiBssid,
                                        @RequestPart(value = "foto", required = false) MultipartFile foto,
                                        HttpServletRequest request, @AuthenticationPrincipal Usuario aluno) {
        return pontoService.registrarEntrada(aluno,
                prova(latitude, longitude, qrCode, wifiSsid, wifiBssid, foto, request));
    }

    @Operation(summary = "Registrar entrada por JSON (sem selfie; só funciona se a selfie não for obrigatória)")
    @PostMapping(path = "/entrada", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ALUNO')")
    public PontoResponse entrada(@Valid @RequestBody RegistroPontoRequest req, HttpServletRequest request,
                                 @AuthenticationPrincipal Usuario aluno) {
        return pontoService.registrarEntrada(aluno,
                prova(req.latitude(), req.longitude(), req.qrCode(), req.wifiSsid(), req.wifiBssid(), null, request));
    }

    @Operation(summary = "Registrar saída pelo app (mesmos campos da entrada, em multipart)")
    @PostMapping(path = "/saida", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ALUNO')")
    public PontoResponse saidaComFoto(@RequestParam("latitude") Double latitude,
                                      @RequestParam("longitude") Double longitude,
                                      @RequestParam(value = "qr_code", required = false) String qrCode,
                                      @RequestParam(value = "wifi_ssid", required = false) String wifiSsid,
                                      @RequestParam(value = "wifi_bssid", required = false) String wifiBssid,
                                      @RequestPart(value = "foto", required = false) MultipartFile foto,
                                      HttpServletRequest request, @AuthenticationPrincipal Usuario aluno) {
        return pontoService.registrarSaida(aluno,
                prova(latitude, longitude, qrCode, wifiSsid, wifiBssid, foto, request));
    }

    @Operation(summary = "Registrar saída por JSON (sem selfie)")
    @PatchMapping(path = "/saida", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ALUNO')")
    public PontoResponse saida(@Valid @RequestBody RegistroPontoRequest req, HttpServletRequest request,
                               @AuthenticationPrincipal Usuario aluno) {
        return pontoService.registrarSaida(aluno,
                prova(req.latitude(), req.longitude(), req.qrCode(), req.wifiSsid(), req.wifiBssid(), null, request));
    }

    @Operation(summary = "Selfie de entrada ou saída de um registro (momento = entrada | saida)")
    @GetMapping("/{id}/foto/{momento}")
    public ResponseEntity<byte[]> foto(@PathVariable("id") Long id, @PathVariable("momento") String momento,
                                       @AuthenticationPrincipal Usuario usuario) {
        FotoPonto foto = pontoService.buscarFoto(id, momento, usuario);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .contentType(MediaType.parseMediaType(foto.getContentType()))
                .body(foto.getDados());
    }

    private static ProvaPresenca prova(Double latitude, Double longitude, String qrCode, String wifiSsid,
                                       String wifiBssid, MultipartFile foto, HttpServletRequest request) {
        if (latitude == null || longitude == null || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
            throw ApiException.badRequest("Localização inválida. Ative o GPS e tente novamente.");
        }
        ProvaPresenca.Selfie selfie = null;
        if (foto != null && !foto.isEmpty()) {
            try {
                selfie = new ProvaPresenca.Selfie(foto.getBytes(), foto.getContentType());
            } catch (IOException e) {
                throw ApiException.badRequest("Não foi possível ler a selfie enviada.");
            }
        }
        return new ProvaPresenca(latitude, longitude, qrCode, wifiSsid, wifiBssid, request.getRemoteAddr(), selfie);
    }

    @Operation(summary = "Ponto aberto no momento (404 se não houver)")
    @GetMapping("/aberto")
    @PreAuthorize("hasRole('ALUNO')")
    public PontoResponse aberto(@AuthenticationPrincipal Usuario aluno) {
        return pontoService.pontoAberto(aluno);
    }

    @Operation(summary = "Histórico de pontos do aluno logado")
    @GetMapping("/historico")
    @PreAuthorize("hasRole('ALUNO')")
    public List<PontoResponse> historico(@AuthenticationPrincipal Usuario aluno) {
        return pontoService.historicoDoAluno(aluno);
    }

    @Operation(summary = "Horas cumpridas x carga horária do contrato ativo")
    @GetMapping("/resumo")
    @PreAuthorize("hasRole('ALUNO')")
    public ResumoHorasResponse resumo(@AuthenticationPrincipal Usuario aluno) {
        return pontoService.resumoDoAluno(aluno);
    }

    @Operation(summary = "Verificar se a localização atual está dentro do raio permitido")
    @PostMapping("/verificar-localizacao")
    @PreAuthorize("hasRole('ALUNO')")
    public VerificacaoLocalizacaoResponse verificar(@Valid @RequestBody LocalizacaoRequest local,
                                                    @AuthenticationPrincipal Usuario aluno) {
        return pontoService.verificarLocalizacao(aluno, local);
    }

    @Operation(summary = "Professor/coordenação fecha um ponto esquecido aberto, informando os minutos válidos")
    @PatchMapping("/{id}/encerrar")
    @PreAuthorize("hasAnyRole('PROFESSOR','COORDENADOR','ADMIN')")
    public PontoResponse encerrar(@PathVariable("id") Long id, @Valid @RequestBody EncerrarPontoRequest req,
                                  @AuthenticationPrincipal Usuario usuario) {
        return pontoService.encerrarManualmente(id, req, usuario);
    }
}
