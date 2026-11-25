package br.com.checkpoint.controller;

import br.com.checkpoint.domain.Usuario;
import br.com.checkpoint.dto.AlterarSenhaRequest;
import br.com.checkpoint.dto.LoginRequest;
import br.com.checkpoint.dto.TokenResponse;
import br.com.checkpoint.dto.UsuarioResponse;
import br.com.checkpoint.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Formato antigo (formulário): username = matrícula, password = senha. */
    @Operation(summary = "Login por formulário (username = matrícula, password = senha)")
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public TokenResponse loginFormulario(@RequestParam("username") String username,
                                         @RequestParam("password") String password) {
        return authService.login(username, password);
    }

    /** Formato JSON: {"matricula": "...", "senha": "..."}. Mais simples de usar no app. */
    @Operation(summary = "Login por JSON ({\"matricula\", \"senha\"})")
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TokenResponse loginJson(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.matricula(), req.senha());
    }

    @Operation(summary = "Dados do usuário logado")
    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal Usuario usuario) {
        return UsuarioResponse.from(usuario);
    }

    @Operation(summary = "Trocar a própria senha")
    @PatchMapping("/me/senha")
    public ResponseEntity<Void> alterarSenha(@AuthenticationPrincipal Usuario usuario,
                                             @Valid @RequestBody AlterarSenhaRequest req) {
        authService.alterarSenha(usuario.getId(), req.senhaAtual(), req.novaSenha());
        return ResponseEntity.noContent().build();
    }
}
