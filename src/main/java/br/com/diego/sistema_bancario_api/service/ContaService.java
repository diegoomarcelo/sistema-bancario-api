package br.com.diego.sistema_bancario_api.service;

import br.com.diego.sistema_bancario_api.model.Conta;
import br.com.diego.sistema_bancario_api.model.Cliente;
import br.com.diego.sistema_bancario_api.model.TipoTransacao;
import br.com.diego.sistema_bancario_api.repository.ContaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ContaService {

    private final ContaRepository contaRepository;
    private final ClienteService clienteService;
    private final TransacaoService transacaoService;

    public ContaService(ContaRepository contaRepository, ClienteService clienteService, TransacaoService transacaoService) {
        this.contaRepository = contaRepository;
        this.clienteService = clienteService;
        this.transacaoService = transacaoService;
    }

    public Conta criarConta(Long clienteId, Conta conta) {
        Cliente cliente = clienteService.buscarPorId(clienteId);
        conta.setCliente(cliente);
        return contaRepository.save(conta);
    }

    public List<Conta> listarContasDoCliente(Long clienteId) {
        return contaRepository.findByClienteId(clienteId);
    }

    @Transactional
    public Conta depositar(Long contaId, BigDecimal valor) {
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do depósito deve ser maior que zero");
        }

        Conta conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));

        conta.setSaldo(conta.getSaldo().add(valor));
        Conta contaAtualizada = contaRepository.save(conta);

        transacaoService.registrar(contaAtualizada, TipoTransacao.DEPOSITO, valor);

        return contaAtualizada;
    }

    @Transactional
    public Conta sacar(Long contaId, BigDecimal valor) {
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do saque deve ser maior que zero");
        }

        Conta conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));

        if (conta.getSaldo().compareTo(valor) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente para realizar o saque");
        }

        conta.setSaldo(conta.getSaldo().subtract(valor));
        Conta contaAtualizada = contaRepository.save(conta);

        transacaoService.registrar(contaAtualizada, TipoTransacao.SAQUE, valor);

        return contaAtualizada;
    }
}