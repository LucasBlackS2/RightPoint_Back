package br.com.checkpoint.repository;

import br.com.checkpoint.domain.TipoUsuario;
import br.com.checkpoint.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByMatricula(String matricula);

    boolean existsByMatricula(String matricula);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByContato(String contato);

    boolean existsByTipoAcesso(TipoUsuario tipoAcesso);

    List<Usuario> findByTipoAcessoOrderByNomeAsc(TipoUsuario tipoAcesso);

    List<Usuario> findAllByOrderByNomeAsc();

    List<Usuario> findByDemonstracaoOrderByNomeAsc(boolean demonstracao);

    List<Usuario> findByTipoAcessoAndDemonstracaoOrderByNomeAsc(TipoUsuario tipoAcesso, boolean demonstracao);
}
