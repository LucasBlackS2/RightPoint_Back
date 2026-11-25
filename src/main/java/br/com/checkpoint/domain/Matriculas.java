package br.com.checkpoint.domain;

import br.com.checkpoint.repository.UsuarioRepository;
import org.slf4j.Logger;

/** Regra da matrícula: só números, de 4 a 20 dígitos. */
public final class Matriculas {

    public static final String REGEX = "\\d{4,20}";
    public static final String MENSAGEM = "A matrícula deve ter apenas números (de 4 a 20 dígitos).";

    private Matriculas() {
    }

    public static boolean valida(String matricula) {
        return matricula != null && matricula.matches(REGEX);
    }

    /**
     * Contas criadas antes desta regra (ex.: "admin", "professor", "aluno") recebem a
     * nova matrícula numérica, mantendo a senha e os dados. Só renomeia se a conta antiga
     * for do perfil esperado e a nova matrícula ainda estiver livre.
     */
    public static void migrar(UsuarioRepository repo, String antiga, String nova, TipoUsuario tipo, Logger log) {
        if (!valida(nova) || repo.existsByMatricula(nova)) {
            return;
        }
        repo.findByMatricula(antiga)
                .filter(u -> u.getTipoAcesso() == tipo)
                .ifPresent(u -> {
                    u.setMatricula(nova);
                    repo.save(u);
                    log.info("Matrícula '{}' trocada para '{}' (matrículas agora são só números).", antiga, nova);
                });
    }
}
