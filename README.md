<h1 align="center">Sistema Bancário API</h1>

<p align="center">
  <b>API REST de contas e transações bancárias com consistência transacional, em Java 21 e Spring Boot.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/status-concluído-2ea44f?style=flat-square" alt="Status">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/MySQL-8-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL">
  <img src="https://img.shields.io/badge/testes-JUnit_5_+_Mockito-25A162?style=flat-square&logo=junit5&logoColor=white" alt="Testes">
</p>

<p align="center">
  <a href="#-como-rodar-localmente">Rodar localmente</a> ·
  <a href="#-endpoints">Endpoints</a> ·
  <a href="#-decisões-técnicas">Decisões técnicas</a>
</p>

---

## 🎯 O problema

Eu já tinha um sistema bancário orientado a objetos em Python ([sistema_bancario](https://github.com/diegoomarcelo/sistema_bancario)), rodando só em memória. A proposta foi portar essas mesmas regras de negócio para uma **API REST com persistência real**, onde o desafio deixa de ser só "calcular o saldo" e passa a ser **garantir que saldo e extrato nunca fiquem inconsistentes**, mesmo se uma operação falhar no meio.

## ✅ A solução

Uma API em camadas (Controller → Service → Repository) que gerencia clientes, contas e movimentações, com validação de entrada, tratamento global de erros e testes unitários das regras de negócio.

**Principais funcionalidades**

- Cadastra clientes e bloqueia CPF duplicado (na regra de negócio e por constraint no banco).
- Abre contas dos tipos corrente, poupança, salário e investimento, vinculadas a um cliente.
- Realiza depósitos e saques com validação de valor positivo e de saldo suficiente.
- Registra cada movimentação e devolve o extrato da conta.
- Retorna erros de regra de negócio como `400 Bad Request` com mensagem clara, em vez de stack trace.

## 📸 Demonstração

Exemplo de fluxo completo (o arquivo [`testes.http`](testes.http) tem todas as requisições prontas para o HTTP Client do IntelliJ):

```http
### 1. Criar cliente
POST http://localhost:8080/clientes
Content-Type: application/json

{ "nome": "Maria Silva", "cpf": "12345678900", "email": "maria@email.com" }

### 2. Abrir conta para o cliente 1
POST http://localhost:8080/contas/clientes/1
Content-Type: application/json

{ "numero": "0001-1", "tipo": "CORRENTE", "saldo": 0 }

### 3. Depositar R$ 500,00
POST http://localhost:8080/contas/1/depositar
Content-Type: application/json

500.00

### 4. Tentar sacar mais do que o saldo → 400 Bad Request
POST http://localhost:8080/contas/1/sacar
Content-Type: application/json

800.00

### 5. Consultar extrato
GET http://localhost:8080/contas/1/extrato
```

<!-- Sugestão: adicione aqui um GIF curto (docs/img/demo.gif) rodando essas requisições no IntelliJ ou no Postman. -->

## 🛠️ Tecnologias

| Camada | Tecnologia | Por que escolhi |
|---|---|---|
| Linguagem | Java 21 | Tipagem forte e o ecossistema mais usado em sistemas financeiros; objetivo do projeto era aprender a stack. |
| Framework | Spring Boot 4.1 | Injeção de dependência e configuração automática deixam o foco nas regras de negócio. |
| Persistência | Spring Data JPA / Hibernate | Mapeamento objeto-relacional e *query methods* sem SQL manual. |
| Banco de dados | MySQL 8 | Os dados são relacionais por natureza (cliente → conta → transação) e precisam de integridade referencial. |
| Validação | Jakarta Bean Validation | Regras de entrada declaradas na entidade (`@NotBlank`, `@Email`, `@NotNull`) e acionadas com `@Valid`. |
| Testes | JUnit 5 + Mockito | Testar as regras do `ContaService` isoladas do banco, de forma rápida e determinística. |
| Build | Maven (wrapper incluso) | Roda sem instalar Maven na máquina: `./mvnw`. |

## 🧱 Arquitetura

```mermaid
flowchart LR
    U[Cliente HTTP] -->|JSON| C[Controller<br/>Cliente · Conta · Transação]
    C --> S[Service<br/>regras de negócio<br/>@Transactional]
    S --> R[Repository<br/>Spring Data JPA]
    R --> DB[(MySQL)]
    C -. erros .-> H[GlobalExceptionHandler<br/>@ControllerAdvice]
```

**Modelo de dados:** `Cliente 1 ── N Conta 1 ── N Transacao`

**Estrutura de pastas**

```
src/main/java/br/com/diego/sistema_bancario_api/
├── controller/   # endpoints REST, sem regra de negócio
├── service/      # regras: CPF único, saldo suficiente, valor positivo
├── repository/   # interfaces JpaRepository + query methods
├── model/        # entidades JPA e enums (TipoConta, TipoTransacao)
└── exception/    # GlobalExceptionHandler
src/test/java/.../service/ContaServiceTest.java
testes.http       # requisições prontas para testes manuais
```

## 💡 Decisões técnicas

- **`BigDecimal` para valores monetários**, comparados com `.compareTo()`: `double`/`float` geram erros de arredondamento inaceitáveis em dinheiro. *Trade-off:* código mais verboso nas operações aritméticas.
- **`@Transactional` em depósito e saque**: atualizar o saldo e registrar a transação acontecem juntos ou não acontecem (rollback automático em caso de falha). Foi o principal problema que o projeto se propôs a resolver.
- **Relacionamento unidirecional `Transacao → Conta`**: a `Conta` não carrega a lista de transações, para não trazer um histórico potencialmente grande a cada consulta. O extrato é buscado sob demanda (`findByContaId`).
- **Unicidade em duas camadas**: o serviço verifica CPF duplicado antes de salvar, e `@Column(unique = true)` no banco é a última linha de defesa contra condições de corrida.
- **`@JsonManagedReference` / `@JsonBackReference`** em `Cliente ↔ Conta`: resolve o loop infinito de serialização do relacionamento bidirecional.
- **Senha do banco via variável de ambiente** (`${DB_PASSWORD}`): o repositório pode ser público sem expor credenciais.

## 🧪 Testes

| Tipo | Ferramenta | Quantidade | O que cobre |
|---|---|---|---|
| Unitários | JUnit 5 + Mockito | 5 | `ContaService`: depósito e saque com sucesso, saldo insuficiente, valor zero/negativo, conta inexistente |
| Manuais | HTTP Client (IntelliJ) | fluxo completo | Cadastro, abertura de conta, movimentações, bloqueios e extrato ([`testes.http`](testes.http)) |

```bash
./mvnw test -Dtest=ContaServiceTest   # testes unitários, não precisam de banco
```

## 🚀 Como rodar localmente

**Pré-requisitos:** Java 21 e MySQL 8 rodando.

```bash
# 1. Clonar
git clone https://github.com/diegoomarcelo/sistema-bancario-api.git
cd sistema-bancario-api

# 2. Criar o banco
mysql -u root -p -e "CREATE DATABASE sistema_bancario;"

# 3. Informar a senha do MySQL
export DB_PASSWORD=sua_senha          # Windows (PowerShell): $env:DB_PASSWORD="sua_senha"

# 4. Subir a aplicação (as tabelas são criadas pelo Hibernate)
./mvnw spring-boot:run                # Windows: mvnw.cmd spring-boot:run
```

A API sobe em `http://localhost:8080`. Ajuste usuário e URL do banco em `src/main/resources/application.properties`, se necessário.

## 🔌 Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/clientes` | Cria um cliente |
| `GET` | `/clientes` | Lista os clientes |
| `GET` | `/clientes/{id}` | Busca um cliente por id |
| `POST` | `/contas/clientes/{clienteId}` | Abre uma conta para o cliente |
| `GET` | `/contas/clientes/{clienteId}` | Lista as contas do cliente |
| `POST` | `/contas/{contaId}/depositar` | Deposita (corpo: valor numérico) |
| `POST` | `/contas/{contaId}/sacar` | Saca (corpo: valor numérico) |
| `GET` | `/contas/{contaId}/extrato` | Lista as transações da conta |

## 🗺️ Próximos passos

- [ ] Migrar `ddl-auto=update` para Flyway ou Liquibase (controle de versão do schema).
- [ ] Interface front-end em React consumindo a API.

## 👤 Autor

**Diego Marcelo**, Desenvolvimento de Software e Dados
[LinkedIn](https://linkedin.com/in/diegomarcelo) · [GitHub](https://github.com/diegoomarcelo) · diegoomarcelo@gmail.com
