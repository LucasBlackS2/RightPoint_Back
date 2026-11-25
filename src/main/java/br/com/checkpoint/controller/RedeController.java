package br.com.checkpoint.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Rede")
public class RedeController {

    /**
     * Mostra o IP com que a requisição chegou ao servidor. O professor usa
     * estando no Wi-Fi da empresa para preencher o "IP público" do local.
     */
    @Operation(summary = "IP da rede de onde você está acessando (para configurar o IP público do local)")
    @GetMapping("/rede/meu-ip")
    @PreAuthorize("hasAnyRole('PROFESSOR','COORDENADOR','ADMIN')")
    public Map<String, String> meuIp(HttpServletRequest request) {
        return Map.of("ip", request.getRemoteAddr());
    }
}
