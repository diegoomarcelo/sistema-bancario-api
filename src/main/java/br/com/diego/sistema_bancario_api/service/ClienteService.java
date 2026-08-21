package br.com.diego.sistema_bancario_api.service;

import br.com.diego.sistema_bancario_api.model.Cliente;
import br.com.diego.sistema_bancario_api.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public Cliente criarCliente(Cliente cliente) {
        Optional<Cliente> clienteExistente = clienteRepository.findByCpf(cliente.getCpf());

        if (clienteExistente.isPresent()) {
            throw new IllegalArgumentException("Já existe um cliente cadastrado com esse CPF");
        }

        return clienteRepository.save(cliente);
    }

    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }
}