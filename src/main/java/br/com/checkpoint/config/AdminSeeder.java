package br.com.checkpoint.config;

import br.com.checkpoint.domain.TipoUsuario;
import br.com.checkpoint.domain.Usuario;
import br.com.checkpoint.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro administrador ao iniciar, se ainda não existir nenhum.
 * Resolve o problema de "ninguém consegue criar o primeiro usuário".
 * Configure ADMIN_MATRICULA e ADMIN_SENHA no .env.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String matricula;
    private final String senha;
    private final String nome;
    private final String email;

    public AdminSeeder(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       @Value("${checkpoint.admin.matricula:}") String matricula,
                       @Value("${checkpoint.admin.senha:}") String senha,
                       @Value("${checkpoint.admin.nome:Administrador}") String nome,
                       @Value("${checkpoint.admin.email:admin@checkpoint.local}") String email) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.matricula = matricula;
        this.senha = senha;
        this.nome = nome;
        this.email = email;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByTipoAcesso(TipoUsuario.ADMIN)) {
            return;
        }
        if (matricula.isBlank() || senha.isBlank()) {
            log.warn("Nenhum administrador cadastrado. Defina ADMIN_MATRICULA e ADMIN_SENHA no .env "
                    + "para criar o primeiro admin automaticamente.");
            return;
        }
        if (usuarioRepository.existsByMatricula(matricula)) {
            log.warn("ADMIN_MATRICULA '{}' já pertence a outro usuário; admin não foi criado.", matricula);
            return;
        }

        Usuario admin = new Usuario();
        admin.setNome(nome);
        admin.setMatricula(matricula);
        admin.setEmail(email);
        admin.setSenhaHash(passwordEncoder.encode(senha));
        admin.setTipoAcesso(TipoUsuario.ADMIN);
        usuarioRepository.save(admin);
        log.info("Administrador inicial criado com a matrícula '{}'. Troque a senha depois do primeiro login.",
                matricula);
    }
}
