package br.com.diego.sistema_bancario_api.controller;

import br.com.diego.sistema_bancario_api.model.Conta;
import br.com.diego.sistema_bancario_api.service.ContaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/contas")
public class ContaController {

    private final ContaService contaService;

    public ContaController(ContaService contaService) {
        this.contaService = contaService;
    }

    @PostMapping("/clientes/{clienteId}")
    public ResponseEntity<Conta> criar(@PathVariable Long clienteId, @Valid @RequestBody Conta conta) {
        Conta contaCriada = contaService.criarConta(clienteId, conta);
        return ResponseEntity.ok(contaCriada);
    }

    @GetMapping("/clientes/{clienteId}")
    public ResponseEntity<List<Conta>> listarContasDoCliente(@PathVariable Long clienteId) {
        List<Conta> contas = contaService.listarContasDoCliente(clienteId);
        return ResponseEntity.ok(contas);
    }

    @PostMapping("/{contaId}/depositar")
    public ResponseEntity<Conta> depositar(@PathVariable Long contaId, @RequestBody BigDecimal valor) {
        Conta conta = contaService.depositar(contaId, valor);
        return ResponseEntity.ok(conta);
    }

    @PostMapping("/{contaId}/sacar")
    public ResponseEntity<Conta> sacar(@PathVariable Long contaId, @RequestBody BigDecimal valor) {
        Conta conta = contaService.sacar(contaId, valor);
        return ResponseEntity.ok(conta);
    }
}