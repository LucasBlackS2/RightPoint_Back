package br.com.checkpoint.controller;

import br.com.checkpoint.domain.Usuario;
import br.com.checkpoint.dto.UsuarioCreateRequest;
import br.com.checkpoint.dto.UsuarioResponse;
import br.com.checkpoint.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/utilizadores")
@Tag(name = "Usuários")
@PreAuthorize("hasAnyRole('PROFESSOR','COORDENADOR','ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Criar usuário (professor cria aluno; coordenador cria todos menos admin; admin cria todos)")
    @PostMapping({"", "/criar"})
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody UsuarioCreateRequest req,
                                 @AuthenticationPrincipal Usuario usuario) {
        return usuarioService.criar(req, usuario);
    }

    @Operation(summary = "Listar usuários (filtro opcional ?tipo=aluno|professor|coordenador|admin)")
    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(name = "tipo", required = false) String tipo,
                                        @AuthenticationPrincipal Usuario usuario) {
        return usuarioService.listar(tipo, usuario);
    }
}
