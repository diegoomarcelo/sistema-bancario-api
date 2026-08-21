package br.com.diego.sistema_bancario_api.repository;

import br.com.diego.sistema_bancario_api.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    List<Transacao> findByContaId(Long contaId);

}