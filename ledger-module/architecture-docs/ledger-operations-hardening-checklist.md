# Ledger Operations Hardening Checklist

아래 항목은 운영 단계 전 보강 권장 사항이다.

## 1. Data Integrity Hardening

1. `ledger_entry` append-only DB 강제
- 방법 A: `BEFORE UPDATE/DELETE` 트리거에서 예외 발생
- 방법 B: DB 권한에서 `UPDATE/DELETE` 제거
- 목표: 애플리케이션 버그/운영 실수로 원장 변경 불가

2. 해시 규칙 변경 방지
- `entry_hash` 계산식 문서 고정 및 변경 시 ADR 필수
- canonicalization 규칙 회귀 테스트 유지

## 2. Outbox Publisher Hardening

1. 다중 인스턴스 경쟁 제어
- `FOR UPDATE SKIP LOCKED` 기반 PENDING 배치 픽업으로 개선
- 현재 단순 조회+갱신 구조를 잠금 기반으로 보강

2. 재시도 정책 고도화
- 오류 분류(일시적/영구적) 후 `FAILED` 전환 기준 분리
- backoff/jitter 적용

3. 상태 전이 감사
- `PENDING -> PUBLISHED/FAILED` 변경 이력(메트릭/로그) 표준화

## 3. Consumer Hardening

1. DLQ 메타데이터 강화
- 원본 payload 외에 실패 이유, 스택 요약, 처리 시각, event_id 헤더 기록

2. 재처리 워크플로우
- DLQ -> 재검증 -> 재발행 API/Job 제공
- 수동 운영 절차(runbook) 문서화

3. 멱등성 검증
- 동일 idempotency key 재처리 테스트(consumer 측 포함) 강화

## 4. Security & Access Control

1. `/ledgers/**` 권한 정책 확정
- tenant isolation + scope 기반 접근 제어

2. `/internal/ledger/outbox` 보호
- 내부망 제한 또는 인증 스코프 강제
- 운영에서는 public exposure 금지

## 5. Observability

1. 메트릭
- outbox pending 건수
- publish 성공/실패 건수
- consumer 처리량, 실패량, DLQ 적재량
- ledger append latency

2. 알림
- outbox FAILED 임계치 초과
- DLQ 증가율 급증
- ledger verify 실패 감지

3. 주기 검증 Job
- passport 단위 체인 무결성 배치 검증
- 실패 시 즉시 경보

## 6. Performance & Capacity

1. 인덱스/파티셔닝 전략 재검토
- `ledger_entry` 대용량 시 `passport_id` 기준 파티셔닝 고려

2. 배치 파라미터 튜닝
- `app.kafka.outbox.batch-size`
- `app.kafka.outbox.publish-interval-ms`
- Kafka partition 수와 consumer concurrency 정렬

## 7. DR/Recovery

1. 백업 정책
- `ledger_entry`, `ledger_chain`, `outbox_event` 정기 백업

2. 장애 복구 리허설
- Kafka 다운/복구, DB failover 시나리오 점검
- 재처리 이후 체인 무결성 확인 절차 필수

## 8. Release Gate (운영 반영 전)

1. Load test
- 고경합 passport append 시나리오
- outbox burst publish 시나리오

2. Chaos test
- Kafka 일시 장애, network partition, duplicate delivery

3. Runbook 승인
- 장애 대응, DLQ 재처리, 롤백/비상중지 절차 서면 완료

