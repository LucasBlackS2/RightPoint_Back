package br.com.checkpoint.repository;

import br.com.checkpoint.domain.FotoPonto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FotoPontoRepository extends JpaRepository<FotoPonto, Long> {

    Optional<FotoPonto> findByPontoIdAndMomento(Long pontoId, String momento);
}
