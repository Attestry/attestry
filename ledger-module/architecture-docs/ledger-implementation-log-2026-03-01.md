# Ledger Implementation Log (2026-03-01)

## 1. Summary

`ledger-module` 기준으로 아래 기능이 구현되었다.

- Ledger append core (canonicalize + hash + chain append)
- Passport 단위 조회 API
- 체인 검증 API
- Kafka 기반 Outbox publisher/consumer 파이프라인
- Docker Compose Kafka 로컬 실행 환경

비즈니스 도메인(Workflow/Product) 이벤트 연결은 아직 미적용이며, 추후 연결 예정이다.

## 2. Delivered Scope

### 2.1 Ledger Schema

- `app/src/main/resources/db/migration/V17__ledger_schema.sql`
  - `ledger_chain`
  - `ledger_entry`
  - `UNIQUE(passport_id, seq)`
  - `idempotency_key UNIQUE`
  - 조회 인덱스

### 2.2 Outbox Schema

- `app/src/main/resources/db/migration/V18__outbox_event.sql`
  - `outbox_event`
  - `status`, `retry_count`, `last_error`, `published_at`
  - `idempotency_key` unique index
  - `(status, created_at)` 인덱스

### 2.3 Ledger Core (Module)

- Append use case/service
  - `ledger-module/src/main/java/io/attestry/ledger/application/ledger/usecase/LedgerAppendUseCase.java`
  - `ledger-module/src/main/java/io/attestry/ledger/application/ledger/LedgerAppendService.java`
- Canonicalize / hash
  - `ledger-module/src/main/java/io/attestry/ledger/infrastructure/persistence/jpa/JacksonLedgerCanonicalizer.java`
  - `ledger-module/src/main/java/io/attestry/ledger/infrastructure/persistence/jpa/Sha256LedgerHashService.java`
- Append persistence
  - `ledger-module/src/main/java/io/attestry/ledger/infrastructure/persistence/jpa/JpaLedgerAppendRepositoryAdapter.java`
  - `ledger-module/src/main/java/io/attestry/ledger/infrastructure/persistence/jpa/entity/LedgerEntryJpaEntity.java`
  - `ledger-module/src/main/java/io/attestry/ledger/infrastructure/persistence/jpa/entity/LedgerChainJpaEntity.java`

### 2.4 External Ledger API

- `ledger-module/src/main/java/io/attestry/ledger/interfaces/ledger/LedgerHttp.java`
  - `GET /ledgers/passports/{passportId}/entries`
  - `GET /ledgers/passports/{passportId}/entries/{ledgerId}`
  - `GET /ledgers/passports/{passportId}/verify`
- `ledger-module/src/main/java/io/attestry/ledger/interfaces/ledger/LedgerApiExceptionHandler.java`

### 2.5 Verification

- `ledger-module/src/main/java/io/attestry/ledger/application/ledger/LedgerVerificationService.java`
- 단위 테스트
  - `ledger-module/src/test/java/io/attestry/ledger/domain/ledger/service/JacksonLedgerCanonicalizerTest.java`
  - `ledger-module/src/test/java/io/attestry/ledger/domain/ledger/service/Sha256LedgerHashServiceTest.java`
  - `ledger-module/src/test/java/io/attestry/ledger/application/ledger/LedgerVerificationServiceTest.java`

### 2.6 Kafka Outbox Pipeline (App)

- Kafka 설정
  - `app/src/main/java/io/attestry/config/KafkaProperties.java`
  - `app/src/main/java/io/attestry/config/KafkaConfig.java`
- Outbox enqueue/publish/consume
  - `app/src/main/java/io/attestry/kafka/outbox/LedgerOutboxEnqueueService.java`
  - `app/src/main/java/io/attestry/kafka/outbox/LedgerOutboxPublisher.java`
  - `app/src/main/java/io/attestry/kafka/ledger/LedgerOutboxConsumer.java`
  - `app/src/main/java/io/attestry/kafka/ledger/LedgerOutboxEventPayload.java`
  - `app/src/main/java/io/attestry/kafka/outbox/persistence/OutboxEventJpaEntity.java`
  - `app/src/main/java/io/attestry/kafka/outbox/persistence/OutboxEventJpaRepository.java`
- internal enqueue API
  - `app/src/main/java/io/attestry/interfaces/internal/LedgerOutboxInternalHttp.java`
    - `POST /internal/ledger/outbox`

### 2.7 Runtime/Infra Config

- Kafka compose
  - `compose.yaml` (`kafka` 서비스 추가)
- App 설정 반영
  - `app/src/main/resources/application.yaml`
  - `app/src/main/resources/application-local.yaml`
  - `app/src/main/resources/application-dev.yaml`
  - `app/src/test/resources/application.yaml` (`app.kafka.enabled=false`)
- scheduler 활성화
  - `app/src/main/java/io/attestry/AttestryApplication.java` (`@EnableScheduling`)

## 3. Current Non-Goals

- Workflow/Product/UserAuth 비즈니스 트랜잭션에서 Outbox enqueue 자동 호출
- 파트너/위임/패스포트 도메인 이벤트 표준 스키마 확정
- 운영 DLQ 재처리/모니터링 자동화

## 4. Basic E2E Check (Current)

1. 앱 실행
2. `POST /internal/ledger/outbox`로 이벤트 enqueue
3. scheduler가 outbox publish
4. Kafka consumer가 ledger append
5. `GET /ledgers/passports/{passportId}/entries`로 결과 확인
6. `GET /ledgers/passports/{passportId}/verify`로 체인 검증

