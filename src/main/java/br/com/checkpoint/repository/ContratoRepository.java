package br.com.checkpoint.repository;

import br.com.checkpoint.domain.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    Optional<Contrato> findFirstByAlunoIdAndStatusTrueOrderByIdDesc(Long alunoId);

    boolean existsByAlunoIdAndStatusTrue(Long alunoId);

    boolean existsByAlunoIdAndStatusTrueAndIdNot(Long alunoId, Long contratoId);

    List<Contrato> findByAlunoIdOrderByIdDesc(Long alunoId);

    List<Contrato> findByProfessorIdOrderByIdDesc(Long professorId);

    List<Contrato> findAllByOrderByIdDesc();

    List<Contrato> findByAlunoDemonstracaoOrderByIdDesc(boolean demonstracao);
}
