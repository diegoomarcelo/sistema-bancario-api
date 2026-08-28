package br.com.diego.sistema_bancario_api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import br.com.diego.sistema_bancario_api.repository.ContaRepository;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import br.com.diego.sistema_bancario_api.model.Conta;
import br.com.diego.sistema_bancario_api.model.TipoTransacao;
import java.math.BigDecimal;
import java.util.Optional;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private ClienteService clienteService;

    @Mock
    private TransacaoService transacaoService;

    @InjectMocks
    private ContaService contaService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void deveDepositarComSucesso() {
        Conta conta = new Conta();
        conta.setId(1L);
        conta.setSaldo(BigDecimal.valueOf(100));

        when(contaRepository.findById(1L)).thenReturn(Optional.of(conta));
        when(contaRepository.save(any(Conta.class))).thenReturn(conta);

        Conta resultado = contaService.depositar(1L, BigDecimal.valueOf(50));

        assertEquals(BigDecimal.valueOf(150), resultado.getSaldo());
        verify(transacaoService).registrar(conta, TipoTransacao.DEPOSITO, BigDecimal.valueOf(50));
    }
    @Test
    void deveLancarExcecaoQuandoSaldoInsuficiente() {
        Conta conta = new Conta();
        conta.setId(1L);
        conta.setSaldo(BigDecimal.valueOf(100));

        when(contaRepository.findById(1L)).thenReturn(Optional.of(conta));

        assertThrows(IllegalArgumentException.class, () -> {
            contaService.sacar(1L, BigDecimal.valueOf(500));
        });
    }
    @Test
    void deveLancarExcecaoQuandoValorDepositoForZeroOuNegativo() {
        assertThrows(IllegalArgumentException.class, () -> {
            contaService.depositar(1L, BigDecimal.ZERO);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            contaService.depositar(1L, BigDecimal.valueOf(-10));
        });
    }

    @Test
    void deveLancarExcecaoQuandoContaNaoEncontrada() {
        when(contaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            contaService.depositar(1L, BigDecimal.valueOf(50));
        });
    }
    @Test
    void deveSacarComSucesso() {
        Conta conta = new Conta();
        conta.setId(1L);
        conta.setSaldo(BigDecimal.valueOf(100));

        when(contaRepository.findById(1L)).thenReturn(Optional.of(conta));
        when(contaRepository.save(any(Conta.class))).thenReturn(conta);

        Conta resultado = contaService.sacar(1L, BigDecimal.valueOf(30));

        assertEquals(BigDecimal.valueOf(70), resultado.getSaldo());
        verify(transacaoService).registrar(conta, TipoTransacao.SAQUE, BigDecimal.valueOf(30));
    }
}