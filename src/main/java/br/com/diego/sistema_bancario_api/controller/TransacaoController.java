package br.com.diego.sistema_bancario_api.controller;

import br.com.diego.sistema_bancario_api.model.Transacao;
import br.com.diego.sistema_bancario_api.service.TransacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/contas")
public class TransacaoController {

    private final TransacaoService transacaoService;

    public TransacaoController(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @GetMapping("/{contaId}/extrato")
    public ResponseEntity<List<Transacao>> listarExtrato(@PathVariable Long contaId) {
        List<Transacao> extrato = transacaoService.listarExtrato(contaId);
        return ResponseEntity.ok(extrato);
    }
}