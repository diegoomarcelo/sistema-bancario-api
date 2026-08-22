# Sistema Bancário API

API REST desenvolvida em **Java + Spring Boot**, portando a lógica de negócio de um sistema bancário que eu já havia desenvolvido anteriormente em Python (orientado a objetos). O objetivo foi aplicar os mesmos conceitos de POO em um novo ecossistema, aprofundando conhecimento em Java, Spring Boot, JPA/Hibernate e boas práticas de arquitetura de APIs REST.

Repositório: [github.com/diegoomarcelo/sistema-bancario-api](https://github.com/diegoomarcelo/sistema-bancario-api)

---

## Tecnologias utilizadas

| Categoria | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Persistência | Spring Data JPA / Hibernate |
| Banco de dados | MySQL 8 |
| Build | Maven |
| Validação | Jakarta Bean Validation |
| Serialização JSON | Jackson |
| IDE | IntelliJ IDEA Community |
| Testes manuais | HTTP Client nativo do IntelliJ |
| Versionamento | Git / GitHub |

---

## Arquitetura

O projeto segue a arquitetura em camadas padrão do Spring Boot:

```
Controller → Service → Repository → Banco de Dados (MySQL)
```

- **`model`** — entidades JPA que representam as tabelas do banco.
- **`repository`** — interfaces `JpaRepository`, responsáveis pelo acesso a dados (sem SQL manual).
- **`service`** — regras de negócio (validações, cálculos, orquestração entre entidades).
- **`controller`** — endpoints REST, responsáveis por receber requisições HTTP e devolver respostas.

Essa separação garante que cada camada tenha uma responsabilidade única, facilitando manutenção e testes.

---

## Modelagem de dados

### Entidades

**`Cliente`**
- `id`, `nome`, `cpf` (único), `email`
- Relacionamento `@OneToMany` com `Conta`

**`Conta`**
- `id`, `numero` (único), `tipo` (enum `TipoConta`: `CORRENTE`, `POUPANCA`, `SALARIO`, `INVESTIMENTO`), `saldo` (`BigDecimal`)
- Relacionamento `@ManyToOne` com `Cliente`

**`Transacao`**
- `id`, `tipo` (enum `TipoTransacao`: `DEPOSITO`, `SAQUE`), `valor` (`BigDecimal`), `dataHora` (`LocalDateTime`)
- Relacionamento `@ManyToOne` com `Conta`

### Diagrama de relacionamento

```
Cliente 1 ──── N Conta 1 ──── N Transacao
```

### Decisões técnicas de modelagem

- **`BigDecimal` em vez de `double`/`float`** para todos os valores monetários (`saldo`, `valor`), evitando erros de arredondamento em ponto flutuante — essencial para sistemas financeiros.
- **`String` em vez de tipo numérico** para `cpf` e `numero` da conta, preservando zeros à esquerda e evitando operações matemáticas indevidas sobre identificadores.
- **Enums (`TipoConta`, `TipoTransacao`)** em vez de `String` livre, garantindo que apenas valores válidos possam ser atribuídos — validado em tempo de compilação.
- **`@Enumerated(EnumType.STRING)`** para persistir o nome do enum no banco (`"CORRENTE"`) em vez do índice numérico, tornando os dados mais legíveis e resilientes a mudanças na ordem dos valores.
- **Relacionamento unidirecional entre `Transacao` e `Conta`** (sem lista de transações dentro de `Conta`) para evitar carregar um histórico potencialmente grande sempre que uma conta é consultada.

---

## Camada de persistência (Repository)

Todas as interfaces estendem `JpaRepository<Entidade, Long>`, herdando automaticamente métodos como `save`, `findById`, `findAll`, `deleteById`.

Métodos customizados criados via **query methods** (o Spring Data JPA gera a query a partir do nome do método, sem SQL manual):

- `ClienteRepository.findByCpf(String cpf)`
- `ContaRepository.findByClienteId(Long clienteId)`
- `TransacaoRepository.findByContaId(Long contaId)`

---

## Regras de negócio (Service)

### `ClienteService`
- `criarCliente` — impede cadastro de CPF duplicado (verifica via `findByCpf` antes de salvar).
- `buscarPorId` — lança exceção caso o cliente não exista.
- `listarTodos`.

### `ContaService`
- `criarConta` — associa a conta a um cliente existente (validado via `ClienteService`).
- `depositar` — valida valor positivo, atualiza saldo, registra a transação.
- `sacar` — valida valor positivo, valida saldo suficiente, atualiza saldo, registra a transação.
- Ambos os métodos de movimentação usam **`@Transactional`**, garantindo que a atualização do saldo e o registro da transação aconteçam como uma única operação atômica (rollback automático em caso de falha).

### `TransacaoService`
- `registrar` — cria e persiste um novo registro de transação.
- `listarExtrato` — retorna o histórico de transações de uma conta.

---

## Endpoints da API

### Clientes

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/clientes` | Cria um novo cliente |
| `GET` | `/clientes` | Lista todos os clientes |
| `GET` | `/clientes/{id}` | Busca um cliente por id |

### Contas

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/contas/clientes/{clienteId}` | Cria uma conta para um cliente |
| `GET` | `/contas/clientes/{clienteId}` | Lista as contas de um cliente |
| `POST` | `/contas/{contaId}/depositar` | Realiza um depósito |
| `POST` | `/contas/{contaId}/sacar` | Realiza um saque |

### Transações

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/contas/{contaId}/extrato` | Lista o extrato (transações) de uma conta |

---

## Validações implementadas

- `@NotBlank` — campos de texto obrigatórios (nome, CPF, email, número da conta).
- `@Email` — formato válido de e-mail.
- `@NotNull` — campos obrigatórios não textuais (tipo de conta, valor, data/hora).
- `@Column(unique = true)` — constraints de unicidade a nível de banco (CPF, número da conta), como última camada de proteção contra duplicidade.
- `@Valid` nos controllers, acionando as validações da entidade antes de processar a requisição.

---

## Desafios técnicos resolvidos durante o desenvolvimento

1. **Serialização JSON em loop infinito**
   O relacionamento bidirecional `Cliente ↔ Conta` causava recursão infinita ao converter para JSON (`Conta` mostra `Cliente`, que mostra suas `Conta`s, que mostram `Cliente`...). Resolvido com `@JsonManagedReference` (em `Cliente.contas`) e `@JsonBackReference` (em `Conta.cliente`), quebrando o ciclo de serialização.

2. **Precisão de valores monetários**
   Uso de `BigDecimal` com `precision`/`scale` definidos na coluna, e comparação de valores via `.compareTo()` (nunca `==` ou operadores relacionais diretos, que não funcionam como esperado para tipos objeto).

3. **Consistência transacional**
   Garantida com `@Transactional` nos métodos que envolvem múltiplas escritas no banco (atualizar saldo + registrar transação), evitando estados inconsistentes em caso de falha parcial.

4. **Proteção de credenciais**
   A senha do banco de dados não fica no código-fonte — é injetada via variável de ambiente (`${DB_PASSWORD}` no `application.properties`, configurada localmente na IDE), permitindo que o repositório seja público sem expor dados sensíveis.

---

## Como rodar o projeto localmente

1. Ter o MySQL instalado e rodando, com um banco criado:
   ```sql
   CREATE DATABASE sistema_bancario;
   ```
2. Configurar a variável de ambiente `DB_PASSWORD` com a senha do MySQL.
3. Clonar o repositório e abrir no IntelliJ IDEA (ou outra IDE Java).
4. Rodar a classe `SistemaBancarioApiApplication`.
5. A aplicação inicia em `http://localhost:8080`, e as tabelas são criadas automaticamente pelo Hibernate (`ddl-auto=update`).

---

## Testes realizados

Toda a API foi validada manualmente via **HTTP Client do IntelliJ**, cobrindo o fluxo completo:

- ✅ Criação de cliente (com bloqueio de CPF duplicado)
- ✅ Criação de conta vinculada a um cliente
- ✅ Depósito (validação de valor positivo, atualização de saldo)
- ✅ Saque (validação de saldo suficiente, atualização de saldo)
- ✅ Bloqueio de saque com saldo insuficiente
- ✅ Extrato com histórico correto de transações

---

## Próximos passos (evolução futura)

- Testes automatizados (JUnit + Mockito) para a camada de serviço.
- Tratamento global de exceções (`@ControllerAdvice`) para respostas de erro padronizadas (atualmente os erros de validação retornam stack trace bruto).
- Documentação interativa da API com Swagger/OpenAPI.
- Possível interface front-end (React) consumindo a API.
- Migração de `ddl-auto=update` para uma ferramenta de migração de schema (Flyway ou Liquibase), mais adequada para produção.
