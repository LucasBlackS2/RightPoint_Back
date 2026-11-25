package br.com.checkpoint.controller;

import br.com.checkpoint.domain.Usuario;
import br.com.checkpoint.dto.DemoRequest;
import br.com.checkpoint.dto.DemoResponse;
import br.com.checkpoint.service.DemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/demo")
@Tag(name = "Demonstração")
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @Operation(summary = "Entrar numa conta de demonstração ({\"perfil\": \"aluno\" | \"professor\"}) sem senha")
    @PostMapping("/entrar")
    public DemoResponse entrar(@Valid @RequestBody DemoRequest req) {
        return demoService.entrar(req.perfil());
    }

    @Operation(summary = "Conteúdo do QR Code do local de demonstração (para simular a leitura)")
    @GetMapping("/qrcode")
    public Map<String, String> qrCode(@AuthenticationPrincipal Usuario usuario) {
        return Map.of("qr_code", demoService.qrCodeSimulado(usuario));
    }

    @Operation(summary = "Diz se o modo demonstração está ligado")
    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("habilitado", demoService.isHabilitado());
    }
}
