package br.com.diego.sistema_bancario_api.service;

import br.com.diego.sistema_bancario_api.model.Conta;
import br.com.diego.sistema_bancario_api.model.Transacao;
import br.com.diego.sistema_bancario_api.model.TipoTransacao;
import br.com.diego.sistema_bancario_api.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;

    public TransacaoService(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    public Transacao registrar(Conta conta, TipoTransacao tipo, BigDecimal valor) {
        Transacao transacao = new Transacao();
        transacao.setConta(conta);
        transacao.setTipo(tipo);
        transacao.setValor(valor);

        return transacaoRepository.save(transacao);
    }

    public List<Transacao> listarExtrato(Long contaId) {
        return transacaoRepository.findByContaId(contaId);
    }
}