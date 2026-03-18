-- blocked_aggregates CTE 최적화: 매 1초 claim 쿼리에서 PROCESSING/retry-pending aggregate 조회 시 사용
-- 쿼리 패턴: SELECT DISTINCT aggregate_id FROM outbox_event
--            WHERE event_type IN (...) AND (status = 'PROCESSING' OR (status = 'PENDING' AND retry_count > 0))
CREATE INDEX IF NOT EXISTS idx_outbox_event_type_status_aggregate
    ON outbox_event (event_type, status, aggregate_id);
