package br.com.diego.sistema_bancario_api.repository;

import br.com.diego.sistema_bancario_api.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContaRepository extends JpaRepository<Conta, Long> {

    List<Conta> findByClienteId(Long clienteId);

}