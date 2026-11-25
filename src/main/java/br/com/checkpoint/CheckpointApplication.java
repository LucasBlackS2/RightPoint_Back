package br.com.checkpoint;

import br.com.checkpoint.config.CriarBancoSeNaoExistir;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@OpenAPIDefinition(
        info = @Info(
                title = "Right Point API",
                version = "2.0.0",
                description = "Gestão de estágios com ponto eletrônico por geolocalização"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class CheckpointApplication {

    public static void main(String[] args) {
        // Permite ao Hibernate/Byte Buddy rodar em versões de Java mais novas que as testadas oficialmente.
        System.setProperty("net.bytebuddy.experimental", "true");
        SpringApplication app = new SpringApplication(CheckpointApplication.class);
        // Cria o banco no PostgreSQL se ele ainda não existir (e explica erros de conexão).
        app.addListeners(new CriarBancoSeNaoExistir());
        app.run(args);
    }
}
