# Ledger DDD Hardening (2026-03-02)

## 목표
- Ledger를 강한 DDD 경계로 정리
- Application은 orchestration만 수행
- 입력/규칙/검증은 Domain으로 이동

## 변경 요약

### 1) Domain 예외/에러 코드 도입
- `LedgerDomainException`
- `LedgerErrorCode.INVALID_LEDGER_REQUEST`

의도:
- `IllegalArgumentException`에 의존하지 않고 도메인 의미가 있는 실패 타입 사용

### 2) Domain Value Object / 입력 모델 도입
- `PassportId`
- `LedgerId`
- `LedgerAppendInput`

이전:
- application service 내부 `requireText`, `validate` 수행

이후:
- 도메인 모델 생성 단계에서 필수값/정규화 검증

### 3) Append 비즈니스 준비 로직 Domain Service 이동
- 신규: `LedgerAppendDomainService`
  - payload canonicalize
  - payload serialize
  - data hash 계산

이전:
- `LedgerAppendService`에서 직접 canonicalizer/hash 호출 + validate

이후:
- `LedgerAppendService`는 command -> domain input 변환 후 repository orchestration만 수행

### 4) Chain 검증 규칙 Domain Service 이동
- 신규: `LedgerChainVerifier`
- 신규 결과 모델: `LedgerChainVerification`

이전:
- `LedgerVerificationService`가 seq/hash/prev_hash 규칙을 직접 수행

이후:
- `LedgerVerificationService`는 repository 조회 + domain verifier 호출 + 결과 매핑만 수행

### 5) API 예외 매핑 강화
- `LedgerApiExceptionHandler`에 `LedgerDomainException` 처리 추가
- 도메인 검증 실패는 `400` + 도메인 에러 코드 응답

### 6) append 계산 로직의 Domain 이전(2차)
- `LedgerChainState`에 `planNext(...)` 추가
  - `nextSeq`, `prevHash`, `entryHash` 계산
  - 다음 체인 상태(`nextState`) 생성
- `JpaLedgerAppendRepositoryAdapter`는 lock 후
  - `LedgerChainState.of(...)`로 도메인 상태 복원
  - `planNext(...)` 결과를 저장에만 사용

의도:
- 인프라가 규칙을 계산하지 않고 도메인 연산 결과를 저장만 하도록 경계 강화

### 7) LedgerEntry Aggregate 생성 경로 통일(3차)
- `LedgerEntry.append(...)` 도입
  - append 시 신규 엔트리 생성 책임을 도메인으로 이동
- `LedgerEntry.rehydrate(...)` 도입
  - persistence 로드 시 의도를 분리해 재구성 경로 명시
- JPA 어댑터는 `new LedgerEntry(...)` 직접 생성 제거

### 8) LedgerChain Aggregate 승격 + Port 도메인 시그니처화(4차)
- 신규 Aggregate: `LedgerChain`
  - `initialize(...)`
  - `restore(...)`
  - `append(...)` -> `AppendPlan(entry, nextChain)` 반환
- append 저장 Port 변경:
  - 이전: 원시 필드 기반 `AppendRequest`
  - 이후: 도메인 객체 기반 `AppendCommand(LedgerAppendInput, LedgerPayloadMaterialized)`
- JPA 어댑터는 Aggregate 연산 결과를 저장만 수행

## 레이어 책임 (현재)
- **Domain**:
  - 식별자/입력 불변식 (`PassportId`, `LedgerId`, `LedgerAppendInput`)
  - append Aggregate (`LedgerChain`, `LedgerEntry`)
  - materialized payload 모델 (`LedgerPayloadMaterialized`)
  - append materialization 규칙 (`LedgerAppendDomainService`)
  - 체인 무결성 규칙 (`LedgerChainVerifier`)
- **Application**:
  - 유스케이스 orchestration
  - 트랜잭션 경계
  - port 호출 및 결과 DTO 매핑
- **Infrastructure**:
  - JPA persistence
  - 동시성 락/업서트
  - canonicalizer/hash 구현체
  - 도메인 계획(`planNext`) 결과 저장

## 남은 강화 후보
- `LedgerEntry`를 단순 record에서 Aggregate Root 성격으로 승격
- `LedgerChainState` 기반 append 연산(`nextEntry`)을 domain으로 이동
- 인프라에서 계산 중인 `entryHash` 생성도 도메인 aggregate로 완전 이전
