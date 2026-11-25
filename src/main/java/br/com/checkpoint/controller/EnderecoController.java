package br.com.checkpoint.controller;

import br.com.checkpoint.domain.Usuario;
import br.com.checkpoint.dto.EnderecoRequest;
import br.com.checkpoint.dto.EnderecoResponse;
import br.com.checkpoint.dto.LocalValidoRequest;
import br.com.checkpoint.dto.LocalizacaoRequest;
import br.com.checkpoint.dto.QrAoVivoResponse;
import br.com.checkpoint.service.EnderecoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/enderecos")
@Tag(name = "Endereços")
@PreAuthorize("hasAnyRole('PROFESSOR','COORDENADOR','ADMIN')")
public class EnderecoController {

    private final EnderecoService enderecoService;

    public EnderecoController(EnderecoService enderecoService) {
        this.enderecoService = enderecoService;
    }

    @Operation(summary = "Cadastrar endereço do estágio (coordenadas pelo Google se não forem enviadas)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnderecoResponse criar(@Valid @RequestBody EnderecoRequest req, @AuthenticationPrincipal Usuario usuario) {
        return enderecoService.criar(req, usuario);
    }

    @Operation(summary = "Atualizar endereço (e coordenadas)")
    @PutMapping("/{id}")
    public EnderecoResponse atualizar(@PathVariable("id") Long id, @Valid @RequestBody EnderecoRequest req,
                                      @AuthenticationPrincipal Usuario usuario) {
        return enderecoService.atualizar(id, req, usuario);
    }

    @Operation(summary = "Listar endereços")
    @GetMapping
    public List<EnderecoResponse> listar(@AuthenticationPrincipal Usuario usuario) {
        return enderecoService.listar(usuario);
    }

    @Operation(summary = "Detalhar um local")
    @GetMapping("/{id}")
    public EnderecoResponse buscar(@PathVariable("id") Long id, @AuthenticationPrincipal Usuario usuario) {
        return enderecoService.buscar(id, usuario);
    }

    @Operation(summary = "Definir o local válido do QR Code: posição, raio e se aceita o QR impresso")
    @PatchMapping("/{id}/local-valido")
    public EnderecoResponse definirLocalValido(@PathVariable("id") Long id, @Valid @RequestBody LocalValidoRequest req,
                                               @AuthenticationPrincipal Usuario usuario) {
        return enderecoService.definirLocalValido(id, req, usuario);
    }

    @Operation(summary = "QR Code ao vivo para o professor mostrar (só dentro do local válido; muda a cada 30 s)")
    @PostMapping("/{id}/qrcode-ao-vivo")
    public QrAoVivoResponse qrAoVivo(@PathVariable("id") Long id, @Valid @RequestBody LocalizacaoRequest posicao,
                                     @AuthenticationPrincipal Usuario usuario) {
        return enderecoService.qrAoVivo(id, posicao.latitude(), posicao.longitude(), usuario);
    }

    @Operation(summary = "Imagem PNG do QR Code do local (imprima e fixe na empresa)")
    @GetMapping(path = "/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCode(@PathVariable("id") Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_PNG)
                .body(enderecoService.qrCodePng(id, usuario));
    }

    @Operation(summary = "Gerar um novo QR Code para o local (o antigo para de funcionar)")
    @PostMapping("/{id}/qrcode/novo")
    public ResponseEntity<Void> novoQrCode(@PathVariable("id") Long id, @AuthenticationPrincipal Usuario usuario) {
        enderecoService.gerarNovoQrCode(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
