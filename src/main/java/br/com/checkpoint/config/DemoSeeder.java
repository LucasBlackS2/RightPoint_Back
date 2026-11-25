package br.com.checkpoint.config;

import br.com.checkpoint.service.DemoService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Prepara os dados de demonstração ao iniciar (se DEMO_HABILITADO=true). */
@Component
public class DemoSeeder implements ApplicationRunner {

    private final DemoService demoService;

    public DemoSeeder(DemoService demoService) {
        this.demoService = demoService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (demoService.isHabilitado()) {
            demoService.garantirDados();
        }
    }
}
