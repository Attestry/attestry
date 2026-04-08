# Attestry

Attestry는 멀티테넌트 제품 여권 플랫폼을 위한 백엔드입니다.  
인증/권한, 제품 생애주기, 상태머신 기반 워크플로우, 그리고 해시체인 기반 원장을 하나의 시스템으로 통합했습니다.

제품 여권(Digital Product Passport)은 제조사 → 유통사 → 소비자 → 서비스 운영 전 생애주기를 추적해야 하는 복잡한 도메인입니다.  
이 프로젝트는 그 복잡도를 단순 기능 나열이 아니라 **모듈 경계, 이벤트 정합성, 원장 무결성, 운영 관측성**으로 구조화해 풀어낸 사례입니다.

## 한눈에 보기

- 아키텍처: `Modular Monolith`
- 설계 방식: `DDD + Hexagonal Architecture`
- 이벤트 정합성: `Transactional Outbox + Kafka`
- 조회 전략: `Partial CQRS + Projection`
- 감사 가능성: `Hash-Chain Ledger`
- 런타임: `Java 21`, `Spring Boot 4`, `PostgreSQL`, `Redis`, `Kafka`, `MinIO`

## 어떤 문제를 푸는가

이 시스템은 단순 CRUD API가 아니라 다음 문제를 다룹니다.

- 브랜드, 운영자, 파트너, 최종 사용자가 얽힌 멀티테넌트 권한 모델
- 제품 발행, 리스크 변경, 폐기와 같은 상태 변경
- 이관, 유통, 배송, 클레임처럼 사람이 개입하는 장기 워크플로우
- 이벤트 유실 없이 원장과 읽기 모델을 함께 갱신해야 하는 정합성 문제
- 장애 이후에도 재처리, 복구, 검증 가능한 운영 구조

즉, 이 프로젝트의 본질은 "API를 많이 만든 것"이 아니라  
**복잡한 비즈니스 흐름을 운영 가능한 시스템 경계로 정리한 것**에 있습니다.


## 핵심 구조

```text
Client
  -> App API (8080)
     -> user-auth / product / workflow
     -> outbox / scheduler / projection
     -> PostgreSQL
     -> Redis / MinIO
     -> Kafka
        -> Ledger Service (8081)
           -> append / query / integrity verification
           -> ledger schema in PostgreSQL
```

## 모듈 책임

| Module | Responsibility |
| --- | --- |
| `app` | 메인 애플리케이션. API 진입점, 스케줄러, outbox publisher, projection consumer |
| `user-auth-module` | 인증, 계정, 테넌트, 멤버십, RBAC, 온보딩 |
| `product-module` | 제품 여권, 소유권, 리스크 상태, 제품 생애주기 |
| `workflow-module` | 이관, 배송, 서비스 요청, 파트너/유통, 구매 클레임 |
| `ledger-service` | 원장 적재, 조회, 해시체인 무결성 검증 |
| `common-lib-module` | 공통 계약, 에러 모델, 베이스 도메인 추상화 |

모듈 간 정적 의존 방향은 아래처럼 유지합니다.

```text
Code-level dependency

                 [ common-lib-module ]
                    ^      ^       ^
                    |      |       |
          +---------+      |       +-------------+
          |                |                     |
          |                |                     |
[ user-auth-module ]  [ product-module ]  [ ledger-service ]
          ^                ^
          |                |
          +--------+-------+
                   |
            [ workflow-module ]
                   ^
                   |
                 [ app ]

Runtime boundary

[ app ] --Kafka--> [ ledger-service ]
```

Static dependency intent
- app -> user-auth, product, workflow
- workflow -> user-auth, product
- all modules -> common-lib
- ledger-service -> common-lib
- ledger-service는 `app`의 하위 모듈이 아니라, 별도 모듈이자 별도 런타임입니다.


## 설계 판단

### 1. 왜 Modular Monolith인가

처음부터 서비스를 나누는 대신, 먼저 도메인 경계를 강하게 만드는 쪽을 택했습니다.

- 현재 단계에서 분산 시스템 비용보다 도메인 응집도 확보가 더 중요했기 때문입니다.
- 트랜잭션과 개발 속도는 단일 배포 단위에서 가져가되, 모듈 책임은 서비스 수준으로 분리했습니다.
- 즉, "지금은 모놀리식하게 배포하지만 나중에는 분리 가능한 구조"를 목표로 했습니다.

### 2. 왜 Outbox를 직접 구현했는가

이 시스템에서 Kafka는 선택적 비동기 채널이 아니라, 원장 반영과 프로젝션 갱신을 연결하는 핵심 런타임 경계입니다.

- CDC는 인프라 의존성과 운영 복잡도가 크고, 애플리케이션 레벨의 순서 제어와 복구 정책을 직접 드러내기 어렵습니다.
- 반면 Outbox는 비즈니스 변경과 이벤트 기록을 같은 DB 트랜잭션에 넣을 수 있어 정합성이 명확합니다.
- 이 프로젝트는 outbox 상태, 재시도, ordering, recovery scheduler를 애플리케이션에서 직접 통제하는 쪽이 더 적합했습니다.

```text
Domain Change
  + Outbox Insert
  -> Publisher
  -> Kafka
  -> Ledger / Projection Consumer
```

### 3. 왜 Ledger를 별도 서비스로 분리했는가

Ledger는 단순 조회 모듈이 아니라, append-only 기록과 무결성 검증을 책임지는 별도 런타임입니다.

- 제품/워크플로우 쓰기 모델과 원장 적재 책임을 분리해 경계를 명확히 했습니다.
- 장애 대응, 배포 주기, 운영 지표를 다른 서비스와 분리할 필요가 있었습니다.
- 해시체인 검증, 원장 소비, 정리 작업 같은 운영 책임이 일반 API 런타임과 성격이 달랐습니다.

### 4. 왜 Partial CQRS인가

모든 기능에 CQRS를 적용하지 않고, 읽기 부하와 결합도가 큰 영역에만 projection을 도입했습니다.

- 전체를 CQRS로 만들면 복잡도 대비 이점이 낮습니다.
- 대신 product/workflow read projection, ledger query 같은 곳에만 분리 전략을 적용했습니다.
- 과한 설계가 아니라, 비용 대비 효과가 높은 지점에만 복잡도를 사용했습니다.

## 도메인 표면

대표 기능은 아래 범주로 정리됩니다.

- Identity / Tenant / RBAC
- Product Passport / Ownership / Risk
- Transfer / Distribution / Shipment / Service Request
- Purchase Claim / Manual Delivery
- Immutable Ledger / Integrity Verification

대표 API 그룹:

- `/auth`
- `/me`
- `/tenants`
- `/onboarding`
- `/memberships`
- `/products`
- `/workflows`
- `/ledgers`

## 코드 진입점

README만 읽고 끝나는 프로젝트가 아니라, 실제 코드 구조는 아래 클래스들에서 바로 확인할 수 있습니다.

| 역할 | 코드 진입점 |
| --- | --- |
| 앱 시작점 | [AttestryApplication.java](./app/src/main/java/io/attestry/AttestryApplication.java) |
| 원장 서비스 시작점 | [LedgerServiceApplication.java](./ledger-service/src/main/java/io/attestry/ledgerservice/LedgerServiceApplication.java) |
| Outbox 스케줄러 | [LedgerOutboxPublisher.java](./app/src/main/java/io/attestry/job/outbox/schedule/LedgerOutboxPublisher.java) |
| Outbox 발행 조정 | [LedgerOutboxPublishCoordinator.java](./app/src/main/java/io/attestry/job/outbox/publish/LedgerOutboxPublishCoordinator.java) |
| Projection consumer | [WorkflowProductProjectionConsumer.java](./app/src/main/java/io/attestry/kafka/workflow/WorkflowProductProjectionConsumer.java) |
| 인증 API | [AuthHttp.java](./user-auth-module/src/main/java/io/attestry/userauth/interfaces/auth/AuthHttp.java) |
| 제품 발행 API | [ProductMintHttp.java](./product-module/src/main/java/io/attestry/product/interfaces/http/command/ProductMintHttp.java) |
| 원장 조회 API | [LedgerHttp.java](./ledger-service/src/main/java/io/attestry/ledger/interfaces/ledger/LedgerHttp.java) |

패키지 규율 역시 일관되게 유지합니다.

```text
interfaces
application
domain
infrastructure
```

즉, 컨트롤러 중심 구조가 아니라  
**도메인 모델과 유스케이스를 중심으로 계층을 나눈 코드베이스**입니다.

## 기술 스택

| Category | Stack |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 |
| Build | Gradle Multi-module |
| Database | PostgreSQL |
| Cache | Redis |
| Messaging | Kafka |
| Storage | MinIO, AWS SDK S3 |
| Migration | Flyway |
| Observability | Actuator, Micrometer, Prometheus |
| Test | JUnit 5, Mockito, Spring Boot Test |

## 배포 및 클라우드 아키텍처

이 애플리케이션은 로컬 실행만 고려한 프로젝트가 아니라, 실제 클라우드 운영을 전제로 인프라가 설계되었습니다.  
상세 구현은 별도 인프라 리포지토리에서 관리하고, 이 저장소에서는 애플리케이션 관점에서 필요한 배포 경계와 런타임 계약을 유지합니다.

### 운영 런타임 경계

- `app`와 `ledger-service`를 별도 배포 단위로 분리
- 각 서비스별 Ingress, Probe, PDB, Scaling 정책 분리
- Kafka 토픽은 애플리케이션이 아니라 클러스터 레벨에서 관리
- `dev` 환경도 운영과 유사한 경계로 설계

### 클라우드 스택

| Area | Stack |
| --- | --- |
| Orchestration | Amazon EKS |
| IaC | Terraform |
| Delivery | ArgoCD GitOps |
| Database | Amazon RDS PostgreSQL 16 |
| Messaging | Strimzi Kafka 4.2.0 on EKS, KRaft mode |
| Cache | Redis on Kubernetes |
| Object Storage | Amazon S3 |
| Edge | AWS ALB + ACM TLS |
| Notifications | AWS Lambda + SQS + SES + DynamoDB |
| Observability | Prometheus + Grafana + Loki + AlertManager |

### 운영 관점에서 중요한 점

- 단순히 앱을 띄우는 수준이 아니라, 배포 단위와 운영 책임을 서비스별로 분리했습니다.
- Kafka는 단일 브로커가 아닌 `3-broker`, `RF=3`, `minISR=2` 기준으로 운영해 원장/프로젝션 파이프라인의 내구성을 확보했습니다.
- Outbox, DLQ, Consumer Lag, Ledger Integrity 같은 비즈니스 운영 지표까지 관측 대상으로 다룹니다.
- GitOps, IaC, 최소 권한 IAM, NetworkPolicy, HTTPS 강제까지 포함해 운영 성숙도를 높였습니다.

### 클라우드 런타임 형태

```text
Internet
  -> ALB + TLS
  -> EKS
     -> Core App
     -> Ledger Service
     -> Redis
     -> Kafka (Strimzi / KRaft)
  -> RDS PostgreSQL
  -> S3
  -> Notification Pipeline
     -> SQS -> Lambda -> SES
     -> DynamoDB idempotency
  -> Prometheus / Grafana / Loki / AlertManager
```

## 로컬 실행

Prerequisites:

- Java 21
- Docker / Docker Compose

Infra:

```bash
docker compose up -d
```

App:

```bash
./gradlew :app:bootRun
```

Ledger:

```bash
./gradlew :ledger-service:bootRun
```

기본 포트:

- `8080`: app
- `8081`: ledger-service

기본 로컬 의존성:

- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- MinIO: `localhost:9000`

## 검증

멀티모듈 구성 확인:

```bash
./gradlew projects
```

전체 테스트:

```bash
./gradlew test
```

현재 저장소 기준으로 `./gradlew test`는 통과합니다.

## 저장소 구조

```text
.
├─ app/
├─ common-lib-module/
├─ ledger-service/
├─ product-module/
├─ user-auth-module/
├─ workflow-module/
├─ compose.yaml
├─ build.gradle.kts
└─ settings.gradle.kts
```
