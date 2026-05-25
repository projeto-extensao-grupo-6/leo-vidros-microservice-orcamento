# Leo Vidros — Microsserviço de Orçamentos

Microsserviço assíncrono responsável pela geração de documentos PDF/DOCX de orçamentos. Opera de forma independente da API principal via RabbitMQ, seguindo os princípios de Clean Architecture.

---

## Stack

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem |
| Spring Boot | 4.0.2 | Framework |
| Spring AMQP | — | Integração RabbitMQ |
| OpenPDF | 1.3.30 | Geração de PDF |
| Apache POI | 5.2.3 | Manipulação de XLSX |
| XDocReport | 2.0.6 | Templates DOCX |
| AWS SDK v2 | 2.25.27 | Armazenamento S3 |
| Lombok | — | Boilerplate |

---

## Arquitetura

```
domain/
└── dto/              # Contratos de dados (OrcamentoDTO, OrcamentoPdfResponseDTO...)

application/
├── ports/            # Interfaces: PdfGenerator, PdfStorageService
└── usecases/         # GerarPdfUseCase — orquestra geração e armazenamento

infrastructure/
├── adapters/
│   ├── OpenPdfAdapter          # Geração de PDF via OpenPDF
│   ├── DocxTemplateAdapter     # Geração via template DOCX (XDocReport)
│   ├── ExcelTemplateAdapter    # Geração via template XLSX (Apache POI)
│   ├── FileStorageAdapter      # Armazenamento local (profile: development)
│   └── S3StorageAdapter        # Armazenamento AWS S3 (profile: production)
├── config/
│   ├── RabbitMQConfig          # Filas, exchanges, DLQ, conversor JSON
│   ├── AwsConfig               # S3Client (profile: production)
│   └── BeanConfig              # Wiring dos use cases
└── queue/
    └── RabbitMQListener        # Consumer da fila (Dev e Prod separados por @Profile)
```

---

## Fluxo de Mensagens

```
API Principal
  └─► exchange.leovidros.direct  (routing key: orcamento.gerar)
        └─► fila.orcamento.pdf
              └─► RabbitMQListener.receberMensagem(OrcamentoDTO)
                    └─► GerarPdfUseCase.executar()
                          ├─► PdfGenerator → gera bytes do PDF
                          └─► PdfStorageService → salva (local ou S3)
                                └─► exchange.leovidros.orcamento.resposta.fanout
                                      └─► fila.orcamento.pdf.resposta
                                            └─► PdfCacheService (API Principal)
```

**Dead Letter:** falhas após 3 tentativas são enviadas para `fila.orcamento.pdf.dlq` via `exchange.leovidros.dlx`.

---

## Filas e Exchanges

| Nome | Tipo | Descrição |
|---|---|---|
| `fila.orcamento.pdf` | Queue (durable) | Recebe solicitações de geração |
| `fila.orcamento.pdf.resposta` | Queue | Resposta para a API principal |
| `fila.orcamento.pdf.dlq` | Dead Letter Queue | Mensagens com falha |
| `exchange.leovidros.direct` | Direct Exchange | Roteamento de entrada |
| `exchange.leovidros.orcamento.resposta.fanout` | Fanout Exchange | Broadcast da resposta |
| `exchange.leovidros.dlx` | Direct Exchange | Roteamento para DLQ |

---

## Profiles

| Profile | Armazenamento do PDF | Resposta publicada |
|---|---|---|
| `development` | Sistema de arquivos local (`PDF_STORAGE_PATH`) | `OrcamentoPdfResponseDTO` (com bytes do PDF) |
| `production` | AWS S3 (`AWS_S3_BUCKET`) | `OrcamentoPdfProdResponseDTO` (com nome do arquivo) |

---

## Variáveis de Ambiente

```env
# Servidor
SERVER_PORT=8082
APP_ENV=development          # development | production

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=leo_vidros
RABBITMQ_PASS=leo_vidros_pass

# Armazenamento local (development)
PDF_STORAGE_PATH=/tmp/orcamentos

# AWS S3 (production)
AWS_REGION=us-east-1
AWS_S3_BUCKET=leo-vidros-orcamentos
aws_access_key_id=             # opcional: usa DefaultCredentialsProvider se vazio
aws_secret_access_key=
aws_session_token=             # necessário para credenciais temporárias (AWS Academy)
```

---

## Como Rodar

### Pré-requisitos
- Java 21 e Maven 3.9+
- RabbitMQ rodando (via `docker-compose up -d rabbitmq` na raiz do monorepo)

### Development (armazenamento local)

```bash
cd leo-vidros-microservice-orcamento/microservice
./mvnw spring-boot:run
# Disponível em: http://localhost:8082/api
```

### Production (AWS S3)

Com credenciais permanentes no `application.yaml` ou via variáveis de ambiente:

```bash
APP_ENV=production ./mvnw spring-boot:run
```

Com credenciais temporárias (AWS Academy):

```bash
export aws_session_token=seu_token_temporario
APP_ENV=production ./mvnw spring-boot:run
```

---

## Templates

Os templates ficam em `src/main/resources/templates/`:

| Arquivo | Uso |
|---|---|
| `orcamento.docx` | Template principal usado pelo `DocxTemplateAdapter` |
| `orçamento.xlsx` | Template alternativo Excel |
