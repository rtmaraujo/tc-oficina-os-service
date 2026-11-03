# tc-oficina-os-service

Microsserviço **orquestrador da Saga** da Oficina Mecânica. Gerencia o ciclo de vida da Ordem de Serviço (OS) e publica/consome eventos no Kafka para coordenar os serviços de Billing e Execution.

- **Porta:** `8081`
- **Banco:** PostgreSQL (`oficina_os`)
- **Namespace Kubernetes:** `oficina-os`
- **Repositório:** https://github.com/rtmaraujo/tc-oficina-os-service
- **Documentação da entrega:** https://github.com/rtmaraujo/tc-oficina-docs

## Stack

- Java 17 + Spring Boot 3.2.2
- Spring Web, Spring Data JPA, PostgreSQL
- Spring Kafka
- Springdoc OpenAPI (Swagger UI)
- JUnit 5 + Mockito + Cucumber (BDD)
- JaCoCo (cobertura mínima de 85%)
- Docker multi-stage + Kubernetes (Traefik)

## Arquitetura

Organização em camadas:

```
presentation/   -> Controllers REST
application/    -> Casos de uso (interface + impl)
domain/         -> Modelo de domínio (OrdemServico, StatusOS)
infrastructure/ -> Entidades JPA e repositórios
config/         -> Listeners Kafka
```

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/os?clienteCpf={cpf}&descricao={texto}` | Cria uma OS (status `ABERTA`) |
| `GET` | `/api/os/{id}` | Consulta OS por ID |
| `GET` | `/api/os/cliente/{clienteCpf}` | Lista OS por cliente |
| `GET` | `/api/os/{id}/historico` | Histórico de transições com `sagaId` |
| `PUT` | `/api/os/{id}/status?novoStatus={status}` | Atualiza o status da OS |

> No cluster, os endpoints ficam sob o prefixo `/os` (ex.: `http://44.253.115.185/os/api/os/1`).

### Exemplos

```bash
# Criar OS
curl -X POST "http://localhost:8081/api/os?clienteCpf=12345678909&descricao=Revisao%20completa"

# Consultar
curl "http://localhost:8081/api/os/1"

# Listar por cliente
curl "http://localhost:8081/api/os/cliente/12345678909"

# Aprovar orçamento
curl -X PUT "http://localhost:8081/api/os/1/status?novoStatus=APROVADA"
```

Swagger desabilitado no perfil local. No perfil `k8s`: `http://44.253.115.185/os/swagger-ui/index.html`.

## Saga orquestrada

Este serviço é o **orquestrador** da Saga. Ele mantém a máquina de estados, registra cada passo e dispara as compensações.

```
ABERTA → ORCAMENTO_GERADO → APROVADA → PAGA → EXECUCAO → CONCLUIDA → FINALIZADA
                                    ↘ CANCELADA (compensação)
```

- `SagaOrchestrator` concentra as transições e a compensação (idempotente por `sagaId`).
- `saga_instancia` guarda o estado da saga (`EM_ANDAMENTO`, `CONCLUIDA`, `COMPENSADA`) e o motivo.
- `saga_passo` registra `OS_CRIADA`, `ORCAMENTO_GERADO`, `ORCAMENTO_APROVADO`, `PAGAMENTO_CONFIRMADO`, `EXECUCAO_INICIADA`, `EXECUCAO_FINALIZADA` e `COMPENSACAO`.
- `ordem_servico_historico` guarda cada transição com `sagaId` e motivo.
- `evento_processado` garante idempotência dos eventos consumidos.

Diagramas completos na [documentação de arquitetura](https://github.com/rtmaraujo/tc-oficina-docs/blob/main/ARQUITETURA.md).

## Status da OS

`ABERTA` → `ORCAMENTO_GERADO` → `APROVADA` → `PAGA` → `EXECUCAO` → `CONCLUIDA` → `FINALIZADA`
Caminho alternativo: `CANCELADA` (compensação por orçamento rejeitado, pagamento recusado, timeout de 30 minutos sem evolução ou cancelamento manual).

## Modelo de dados

Tabela `ordem_servico`:

| Campo | Tipo | Observação |
|---|---|---|
| `id` | Long | PK, auto incremento |
| `numero_os` | String | único, gerado como `OS-<timestamp>` |
| `descricao` | String | descrição do serviço |
| `status` | Enum | `StatusOS` |
| `cliente_cpf` | String | CPF do cliente |
| `orcamento` | Double | valor do orçamento |
| `data_criacao` | LocalDateTime | preenchida na criação |
| `data_atualizacao` | LocalDateTime | atualizada a cada mudança de status |

Tabelas da Saga:

| Tabela | Campos principais |
|---|---|
| `saga_instancia` | `saga_id`, `os_id`, `os_numero`, `status` (`EM_ANDAMENTO`/`CONCLUIDA`/`COMPENSADA`), `passo_atual`, `motivo` |
| `saga_passo` | `saga_id`, `passo`, `status`, `detalhe`, `timestamp` |
| `ordem_servico_historico` | `os_id`, `status_anterior`, `status_novo`, `motivo`, `saga_id`, `timestamp` |
| `evento_processado` | `event_id`, `topico` (idempotência) |

## Eventos Kafka

| Tópico | Direção | Quando |
|---|---|---|
| `os-created` | Produz | Ao criar a OS |
| `execution-requested` | Produz | Após `payment-completed` (OS `PAGA`) |
| `compensate-budget` | Produz | Ao compensar a saga |
| `compensate-execution` | Produz | Ao compensar a saga |
| `saga-compensated` | Produz | Auditoria do fechamento da saga compensada |
| `budget-generated` | Consome | Orçamento criado → OS `ORCAMENTO_GERADO` |
| `budget-approved` | Consome | Orçamento aprovado → OS `APROVADA` |
| `budget-rejected` | Consome | Orçamento rejeitado → compensação |
| `payment-completed` | Consome | Pagamento confirmado → OS `PAGA` |
| `payment-failed` | Consome | Pagamento recusado → compensação |
| `order-progress` | Consome | Execução iniciada → OS `EXECUCAO` |
| `order-finalized` | Consome | Execução finalizada → OS `CONCLUIDA` |
| `execution-compensated` | Consome | Confirmação da compensação da execução |

Grupo de consumo: `os-service-group`. Todos os tópicos têm `.DLT` para mensagens que esgotam as tentativas.

> `FINALIZADA` é uma transição manual (`PUT /api/os/{id}/status?novoStatus=FINALIZADA`) e não publica evento.

## Configuração

| Variável | Padrão (local) | Descrição |
|---|---|---|
| `SERVER_PORT` | `8081` | Porta HTTP |
| `SPRING_PROFILES_ACTIVE` | — | `docker` ou `k8s` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/oficina_os` | URL do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `oficina_user` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | `oficina_pass` | Senha do banco |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Endereço do Kafka |

## Como executar localmente

### Com Docker Compose (recomendado)

```bash
docker compose up --build
```

Sobe PostgreSQL, Zookeeper, Kafka e o serviço. A porta HTTP é `8081`.

### Sem Docker

Pré-requisitos: PostgreSQL em `localhost:5432` (database `oficina_os`) e Kafka em `localhost:9092`.

```bash
mvn spring-boot:run
```

## Testes e cobertura

```bash
mvn test              # testes unitários
mvn verify            # testes + jacoco:check (mínimo 85% LINE e BRANCH)
```

- Testes unitários: domínio, entidade de persistência, serviço de aplicação, orquestrador da Saga e listeners.
- **BDD (Cucumber):** `src/test/resources/features/ordem-servico-saga.feature`
  - Fluxo completo: criação → aprovação → execução → conclusão → finalização.
  - Cenário de cancelamento.
- Cobertura atual: **66 testes**, **100%** de linhas e **96,36%** de branches.
- Relatório: `target/site/jacoco/index.html`.

O build falha se a cobertura ficar abaixo de 85%, via `jacoco-maven-plugin` (`check`).

## Docker

```bash
docker build -t tc-oficina-os-service:local .
docker run -p 8081:8081 --env SPRING_DATASOURCE_URL=... --env KAFKA_BOOTSTRAP_SERVERS=... tc-oficina-os-service:local
```

## Deploy no Kubernetes

Manifests em `k8s/`:

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
cp k8s/secret.example.yaml k8s/secret.yaml   # preencha as credenciais reais
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/ingressroute.yaml
```

- Imagem: `200111905660.dkr.ecr.us-west-2.amazonaws.com/tc-oficina-os-service:latest`
- 2 réplicas, probes em `/actuator/health/{readiness,liveness}`
- Rota Traefik: `http://44.253.115.185/os/*` (prefixo removido)

## CI/CD

`.github/workflows/ci.yml`:

1. Build e testes com Java 17.
2. `mvn verify` — executa testes + `jacoco:check` (>= 85%).
3. Em push para `main`: build/push da imagem no ECR e `kubectl set image` no namespace `oficina-os`.

## Observabilidade

- Actuator em `/actuator` (`health`, `info`, `metrics`, `prometheus` no perfil `k8s`).
- Probes de readiness/liveness em `/actuator/health/readiness` e `/actuator/health/liveness`.
- Métricas coletadas pelo Prometheus: `http://44.253.115.185/prometheus/`.
- Dashboard: `http://44.253.115.185/grafana/` → **Oficina Fase 4 - Microsservicos**.
- Traces distribuídos no Zipkin: `http://44.253.115.185/zipkin/`.
- O `sagaId` é propagado no MDC e aparece nos logs, permitindo correlacionar a OS entre os três serviços.

## Estrutura

```
src/main/java/com/oficina/os/service/
├── application/        # interface e implementação dos casos de uso
├── config/             # listeners Kafka
├── domain/             # OrdemServico, StatusOS
├── infrastructure/     # entidade JPA e repositório
└── presentation/       # controller REST
src/test/java/...        # testes unitários e BDD (steps + runner)
src/test/resources/features/
```
