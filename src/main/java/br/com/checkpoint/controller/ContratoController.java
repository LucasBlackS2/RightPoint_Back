package br.com.checkpoint.controller;

import br.com.checkpoint.domain.Usuario;
import br.com.checkpoint.dto.ContratoRequest;
import br.com.checkpoint.dto.ContratoResponse;
import br.com.checkpoint.dto.ContratoStatusRequest;
import br.com.checkpoint.dto.PontoResponse;
import br.com.checkpoint.dto.ResumoHorasResponse;
import br.com.checkpoint.service.ContratoService;
import br.com.checkpoint.service.PontoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/contratos")
@Tag(name = "Contratos")
public class ContratoController {

    private final ContratoService contratoService;
    private final PontoService pontoService;

    public ContratoController(ContratoService contratoService, PontoService pontoService) {
        this.contratoService = contratoService;
        this.pontoService = pontoService;
    }

    @Operation(summary = "Criar contrato de estágio")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PROFESSOR','COORDENADOR','ADMIN')")
    public ContratoResponse criar(@Valid @RequestBody ContratoRequest req, @AuthenticationPrincipal Usuario usuario) {
        return contratoService.criar(req, usuario);
    }

    @Operation(summary = "Listar contratos (aluno vê os seus; professor vê os que orienta; coordenação vê todos)")
    @GetMapping
    public List<ContratoResponse> listar(@AuthenticationPrincipal Usuario usuario) {
        return contratoService.listar(usuario);
    }

    @Operation(summary = "Detalhar contrato")
    @GetMapping("/{id}")
    public ContratoResponse buscar(@PathVariable("id") Long id, @AuthenticationPrincipal Usuario usuario) {
        return contratoService.buscar(id, usuario);
    }

    @Operation(summary = "Ativar/encerrar contrato")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PROFESSOR','COORDENADOR','ADMIN')")
    public ContratoResponse alterarStatus(@PathVariable("id") Long id, @Valid @RequestBody ContratoStatusRequest req,
                                          @AuthenticationPrincipal Usuario usuario) {
        return contratoService.alterarStatus(id, req.status(), usuario);
    }

    @Operation(summary = "Histórico de pontos do contrato")
    @GetMapping("/{id}/pontos")
    public List<PontoResponse> pontos(@PathVariable("id") Long id, @AuthenticationPrincipal Usuario usuario) {
        return pontoService.historicoDoContrato(id, usuario);
    }

    @Operation(summary = "Horas cumpridas x carga horária do contrato")
    @GetMapping("/{id}/resumo")
    public ResumoHorasResponse resumo(@PathVariable("id") Long id, @AuthenticationPrincipal Usuario usuario) {
        return pontoService.resumoDoContrato(id, usuario);
    }
}
