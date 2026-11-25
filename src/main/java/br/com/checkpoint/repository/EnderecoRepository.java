package br.com.checkpoint.repository;

import br.com.checkpoint.domain.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnderecoRepository extends JpaRepository<Endereco, Long> {

    List<Endereco> findByDemonstracaoOrderByIdDesc(boolean demonstracao);

    Optional<Endereco> findFirstByDemonstracaoTrueOrderByIdAsc();
}
