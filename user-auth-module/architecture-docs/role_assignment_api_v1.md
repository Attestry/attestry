1) 전용 Role Assignment API를 “반드시” 만든다 (v1 핵심)

추가 결정(확정):
- v1은 단일 role assignment 모델로 운영한다.
- `membership_role_assignments.membership_id UNIQUE`는 유지한다.
- 다중 role assignment는 v2에서 도입한다(충돌/우선순위 규칙 확정 후).

문서에 적은 대로 다음 3개를 user-auth에 추가해.

API

POST /tenants/{tenantId}/admin/memberships/{membershipId}/roles/{roleCode}

DELETE /tenants/{tenantId}/admin/memberships/{membershipId}/roles/{roleCode}

GET /tenants/{tenantId}/admin/memberships/{membershipId}/roles

권한(권장)

기본: TENANT_ROLE_ASSIGN

민감 role 부여/회수는 LIVE_RECHECK 강제 + 추가 권한 필요(추천: TENANT_ROLE_ASSIGN_SENSITIVE 또는 TENANT_MEMBERSHIP_ENFORCE와 별개)

지금 seed에 TENANT_ROLE_ASSIGN는 여러 role에 이미 들어가 있으니 운영이 가능해짐.

2) “membership.role → role 자동 매핑”을 v1에서 줄여야 한다

현재 DefaultRoleIdMapper가 자동으로:

ADMIN+BRAND -> role-brand-admin-base

ADMIN+RETAIL -> role-retail-admin-base
… 를 매핑하고 있지?

이걸 그대로 두면, membership.role만 바꿔도 권한 번들이 바뀌는 효과가 나서
RBAC role 분리 운영(TO-BE)와 정면 충돌해.

추천 변경

로그인 권한 계산에서 “자동 매핑”은 베이스 role 1개만 주도록 축소:

예: 모든 membership은 기본적으로 role-group-staff만 자동 부여

또는 groupType별 최소 베이스:

BRAND 기본: role-brand-operator(업무 실행만)

RETAIL 기본: role-retail-operator

role-brand-admin-base, role-retail-admin-base, role-tenant-*는 자동 부여 금지

반드시 assignment API로만 부여

특히 민감 role(TENANT_OWNER, TENANT_MEMBERSHIP_ADMIN, TENANT_PASSPORT_ADMIN)은 자동 경로가 있으면 사고 확률이 높아.

3) “누가 어떤 role을 부여할 수 있나” 정책을 먼저 고정해

전용 API만 만들면 “누구나 roleCode만 알면 승격”이 되니까 위험해.
그래서 아래 규칙을 코드/문서로 고정.

3-1) self-escalation 금지(필수)

요청자 membershipId == 대상 membershipId 인 경우

“상위 role 부여” 금지

최소한 TENANT_OWNER/TENANT_MEMBERSHIP_ADMIN/TENANT_PASSPORT_ADMIN는 self-assign 금지

3-2) assignable role allowlist(필수)

TENANT_ROLE_ASSIGN만 가진 사람은 부여 가능한 role을 제한해야 해.

예시 v1 allowlist:

일반 운영자가 부여 가능:

BRAND_OPERATOR, RETAIL_OPERATOR, GROUP_STAFF

테넌트 오너급만 부여 가능:

BRAND_ADMIN_BASE, RETAIL_ADMIN_BASE, TENANT_MEMBERSHIP_ADMIN

플랫폼만 부여 가능:

TENANT_OWNER, TENANT_PASSPORT_ADMIN, PLATFORM_SUPER_ADMIN

“누가 부여 가능한가”를 roleCode 기준으로 고정하면 운영이 단순해져.

3-3) LIVE_RECHECK 강제(민감 role)

Role assignment 자체를 evaluate에서 민감 액션으로 분류:

roleCode가 민감 리스트에 포함되면:

/authz/evaluate decisionMode=LIVE_RECHECK로만 통과

DB에서 현재 requester/target membership 상태 재확인

3-4) 감사로그(필수)

누가(Actor) 누구(Target)에 어떤 role을 부여/회수했는지

이전 role set / 이후 role set

사유(optional), traceId

4) 지금 seed role 매트릭스는 방향이 괜찮은데, 한 가지 조정 추천
   role-retail-admin-base에 TENANT_GROUP_CREATE가 들어가 있음

리테일을 tenant로 고정할 거면, “리테일 admin”이 자기 tenant 내부 그룹(지점) 만드는 건 OK.
하지만 v1에서 그룹 생성 자체를 제한하려는 정책이라면:

TENANT_GROUP_CREATE는 TENANT_OWNER나 별도 TENANT_GROUP_ADMIN으로 빼는 것도 가능.

(이건 운영 정책 선택이지만, 최소한 “brand admin base가 group create를 갖는가”는 확실히 금지하는 게 좋아.)

5) 딱 “실행 체크리스트”로 요약

user-auth에 role assignment API 3개 추가

membership.role 기반 DefaultRoleIdMapper를 “최소 베이스 role만” 주도록 축소

allowlist + self-escalation 금지 + LIVE_RECHECK + audit 로직 추가

문서(role-scope-current-matrix.md)에 “승격 경로” 섹션 추가

누가 어떤 role을 부여 가능/불가능 표로 고정

가장 현실적인 v1 운영 시나리오

신규 가입/초대 → 기본은 GROUP_STAFF 또는 *_OPERATOR만 자동

팀 운영자가 사람을 추가로 승격해야 하면:

TENANT_ROLE_ASSIGN로 *_ADMIN_BASE까지(허용한다면) 부여

테넌트 오너/민감 권한은:

플랫폼이 직접 부여하거나

(추후) 2인 승인으로 부여




목표

TENANT_OWNER / TENANT_MEMBERSHIP_ADMIN / TENANT_PASSPORT_ADMIN 같은 민감 role도 운영 경로로 부여/회수 가능

자동 매핑은 최소화해서 membership.role(직책) ≠ 권한 번들(role) 이 되도록 분리

승격 사고(자기 승격/테넌트 경계 위반) 방지

Phase 0 — 정책 고정(1~2일)
할 일

role 분류표 확정

Base role: BRAND_OPERATOR, RETAIL_OPERATOR, GROUP_STAFF

Admin base role: BRAND_ADMIN_BASE, RETAIL_ADMIN_BASE

Tenant admin role: TENANT_MEMBERSHIP_ADMIN, TENANT_PASSPORT_ADMIN

Sensitive role: TENANT_OWNER, PLATFORM_SUPER_ADMIN

Assignment 규칙 확정

self-escalation 금지(최소 민감 role은 무조건 금지)

assignable allowlist(“누가 어떤 roleCode를 부여 가능한지”)

민감 role 부여/회수는 LIVE_RECHECK + 감사로그 필수

(v1) role 정의 편집은 플랫폼만

산출물

architecture-docs/role-assignment-policy.md (표 포함)

“민감 role 리스트” 상수(SensitiveRoleCodes)

완료 기준

“누가 어떤 role을 줄 수 있는지”가 표로 고정됨(논쟁 끝)

Phase 1 — 전용 Role Assignment API 도입(2~3일)
할 일

user-auth에 3개 endpoint 추가

POST assign

DELETE revoke

GET list

유스케이스/서비스 계층 추가

AssignRoleUseCase / RevokeRoleUseCase

DB: membership_role_assignments(이미 있으면 사용)

권한 요구

기본은 TENANT_ROLE_ASSIGN

민감 role은 추가 체크(아래 Phase 2에서)

산출물

Controller + UseCase + Repository Query

API 테스트(http collection)

완료 기준

임의의 membership에 role 부여/회수가 동작

tenant 경계 검증(tenantId mismatch 시 실패)

Phase 2 — 가드레일(필수 안전장치)(2~4일)
할 일

self-escalation 금지

요청자가 자기 membershipId에 민감 role 부여 시 403

assignable allowlist 적용

요청자 role set/permission set에 따라 “부여 가능한 roleCode” 제한

예: TENANT_MEMBERSHIP_ADMIN은 *_OPERATOR만, TENANT_OWNER만 TENANT_MEMBERSHIP_ADMIN 부여 가능 등

LIVE_RECHECK 강제

민감 role 부여/회수는 /authz/evaluate decisionMode=LIVE_RECHECK로만 허용

또는 role assignment usecase가 직접 DB 상태 재검증

감사 로그

actor/target/roleCode/action(ASSIGN/REVOKE)/timestamp/reason/traceId 기록

산출물

RoleAssignmentPolicy(도메인 정책 클래스)

audit table or 기존 audit 연동

완료 기준

자기 승격 차단됨

금지 role 부여 불가

민감 role 변경은 LIVE_RECHECK 없이 실패

audit 기록 남음

Phase 3 — 자동 매핑 축소(리스크 제거)(1~2일)
할 일

DefaultRoleIdMapper를 변경

membership.role + groupType로 BRAND_ADMIN_BASE 자동 부여 같은 로직 제거/축소

v1에서는 “베이스 role”만 자동

예: BRAND membership 기본 = BRAND_OPERATOR

RETAIL membership 기본 = RETAIL_OPERATOR

나머지 = GROUP_STAFF

“admin base role”부터는 Assignment API로만 승격

산출물

mapper 수정 PR + 회귀 테스트

완료 기준

membership.role을 바꿔도 권한 번들이 자동으로 커지지 않음

승격은 오직 assignment API 경로로만 가능

Phase 4 — 문서/운영 플로우 정리(1~2일)
할 일

role-scope-current-matrix.md에 “승격 경로” 섹션 추가

운영자 시나리오 문서:

신규 가입 → 기본 role

팀 확장 → operator 초대/부여

테넌트 운영자 지정 → platform이 tenant_owner 부여

에러코드(reasonCode) 표준화

완료 기준

운영/CS가 “어떻게 승격하나요?”에 문서로 답 가능

최종 체크(통합 테스트 시나리오)

A tenant에서 B tenant membership에 role assign 시도 → 거부

BRAND_ADMIN_BASE가 TENANT_OWNER 부여 시도 → 거부

TENANT_OWNER가 TENANT_MEMBERSHIP_ADMIN 부여 → 성공

role assign 후 토큰 권한 반영 정책 확인(짧은 TTL/재로그인/토큰 버전)
