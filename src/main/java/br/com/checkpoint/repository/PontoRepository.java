package br.com.checkpoint.repository;

import br.com.checkpoint.domain.Ponto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PontoRepository extends JpaRepository<Ponto, Long> {

    /** Ponto aberto (sem saída) do aluno, em qualquer contrato. */
    Optional<Ponto> findFirstByContratoAlunoIdAndAtivoTrue(Long alunoId);

    boolean existsByContratoIdAndAtivoTrue(Long contratoId);

    List<Ponto> findByContratoAlunoIdOrderByHoraEntradaDesc(Long alunoId);

    List<Ponto> findByContratoIdOrderByHoraEntradaDesc(Long contratoId);

    /** Soma dos minutos dos pontos já fechados de um contrato. */
    @Query("select coalesce(sum(p.tempoTrabalhadoMinutos), 0) from Ponto p "
            + "where p.contrato.id = :contratoId and p.ativo = false")
    Number somarMinutosPorContrato(@Param("contratoId") Long contratoId);
}
