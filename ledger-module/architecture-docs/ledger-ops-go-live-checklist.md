# Ledger Ops Go-Live Checklist

## 1. 데이터 무결성
- [ ] `ledger_entry` UPDATE/DELETE 차단(DB 트리거 또는 권한 제거)
- [ ] 해시 계산식/캐노니컬 규칙 변경 금지 정책 확정
- [ ] 체인 검증 API(`GET /ledgers/passports/{passportId}/verify`) 점검 완료

## 2. Outbox 안정성
- [ ] Outbox 배치 처리에 동시성 제어(`FOR UPDATE SKIP LOCKED`) 적용
- [ ] 재시도 정책(`maxRetries`, backoff) 운영값 확정
- [ ] `FAILED` 이벤트 처리 절차(runbook) 문서화

## 3. Kafka 운영
- [ ] 토픽 생성/파티션/replica 설정 확인
- [ ] DLQ 토픽(`ledger.outbox.v1.dlq`) 수집 확인
- [ ] Consumer group lag 모니터링 대시보드 연결

## 4. 보안
- [ ] `/ledgers/**` 접근 권한 정책 적용
- [ ] `/internal/ledger/outbox` 내부망/인증으로 보호
- [ ] tenant isolation 검증 완료

## 5. 관측성
- [ ] 메트릭 수집: outbox pending, publish success/fail, DLQ 건수
- [ ] 경보 설정: FAILED 급증, DLQ 급증, verify 실패
- [ ] 구조화 로그(event_id, passport_id, idempotency_key) 적용

## 6. 성능/용량
- [ ] outbox batch-size / publish-interval 튜닝 완료
- [ ] 고경합 passport append 부하 테스트 통과
- [ ] 데이터 증가 대비 파티셔닝/아카이빙 전략 확정

## 7. 복구/운영 절차
- [ ] Kafka 장애/복구 리허설 완료
- [ ] DLQ 재처리 리허설 완료
- [ ] 복구 후 체인 무결성 재검증 절차 확정

## 8. 릴리즈 게이트
- [ ] 통합 테스트 통과
- [ ] 운영 문서 승인(장애 대응, 롤백, 비상중지)
- [ ] Go-Live 승인자 서명 완료
