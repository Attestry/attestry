RBAC v1 Policy (Recommended)
0. 목표

플랫폼 권한과 테넌트 권한, 도메인 실행 권한을 명확히 분리한다.

v1에서는 권한 “정의/편집”은 플랫폼만, 테넌트는 운영(assignment/초대/그룹 생성) 위주로 제한한다.

MEMBERSHIP_MANAGE 같은 “덩어리 권한”을 반드시 분해해서 권한 상승/오용을 방지한다.

1. Permission 네임스페이스 원칙

permissions.code를 아래 범주로 분류한다.

Platform (전역)

플랫폼 운영/승인/전역 감사/전역 차단

테넌트 사용자에게 절대 부여 금지

Tenant (테넌트 운영)

해당 tenant 내부의 그룹/멤버/초대/감사 등 운영 권한

Domain (업무 실행)

BRAND/RETAIL/SERVICE/OWNER 같은 실제 비즈니스 액션 권한
(민팅, 출고, 서비스 기록 등)

2. v1에서 Permission 정리(Seed 수정)
   2.1 삭제/대체(네가 지적한 부분 포함)
   ❌ FULFILLMENT_RELEASE 제거 권장

이유: “fulfillment”는 실행 주체(브랜드/리테일/서비스/3PL) 모델이 정해지기 전의 임시 추상화로 보임.

v1에서는 권한을 “누가 하느냐(Actor/GroupType)” 기준으로 명확히 한다.

대체안 2가지 중 하나를 택해:

안 A(추천): RETAIL_RELEASE로 명확히 (리테일 출고 책임)

안 B: DELIVERY_RELEASE 같은 중립 용어로 바꾸고, groupType과 분리(추후 capability 기반 확장)

명품/리테일 시나리오에선 “출고=리테일”이 더 자연스러운 경우가 많으니 v1은 RETAIL_RELEASE 추천.

3. v1 Permission 최소 세트(추천)

너희 현재 seed에서 필요한 것만 남기고, 운영을 위해 아래를 추가/변경해.

3.1 Platform 전용

PLATFORM_ADMIN

TENANT_CREATE_APPROVE

TENANT_SUSPEND

GLOBAL_AUDIT_READ

플랫폼 계정만 가질 수 있음. 테넌트 membership 기반 role에는 절대 포함 금지.

3.2 Tenant 운영(핵심: 분해)

기존 TENANT_ADMIN, MEMBERSHIP_MANAGE는 의미가 애매하거나 너무 큼. v1에서 이렇게 바꿔:

TENANT_GROUP_CREATE

TENANT_GROUP_SUSPEND / TENANT_GROUP_RESUME

TENANT_INVITATION_CREATE / TENANT_INVITATION_REVOKE / TENANT_INVITATION_VIEW

TENANT_MEMBERSHIP_VIEW

TENANT_ROLE_ASSIGN (= assignment 전용)

(선택, 더 강함) TENANT_MEMBERSHIP_ENFORCE (정지/추방/상태 변경)

TENANT_AUDIT_READ

그리고:

MEMBERSHIP_MANAGE는 폐기하거나, 최소한 “role assign만”을 의미하도록 명확히 하고 위 새 코드로 대체하는 걸 권장.

3.3 Domain 실행(현행 유지)

BRAND_MINT

BRAND_VOID

RETAIL_TRANSFER_CREATE

(대체) RETAIL_RELEASE

PASSPORT_PERMISSION_GRANT (위임/회수는 민감)

owner 기본:

OWNER_TRANSFER_CREATE

OWNER_TRANSFER_ACCEPT

OWNER_RISK_FLAG

OWNER_RISK_CLEAR

4. Role 설계(v1)

v1은 단일 역할(membership당 role 1개)로 간다.

4.1 Platform Roles

PLATFORM_ADMIN_ROLE

permissions: PLATFORM_ADMIN, TENANT_CREATE_APPROVE, TENANT_SUSPEND, GLOBAL_AUDIT_READ

(필요시) RBAC 편집 권한은 별도 PLATFORM_RBAC_ADMIN로 분리 가능

4.2 Tenant Roles (템플릿)
TENANT_OWNER (테넌트 최상위 운영자)

tenant 운영 전체 + 민감 운영 포함

permissions:

TENANT_GROUP_CREATE

TENANT_GROUP_SUSPEND/RESUME

TENANT_INVITATION_*

TENANT_MEMBERSHIP_VIEW

TENANT_ROLE_ASSIGN

TENANT_MEMBERSHIP_ENFORCE (가능하면 approval 또는 LIVE_RECHECK 강제)

TENANT_AUDIT_READ

BRAND_ADMIN_BASE (브랜드 운영자: 팀 운영/업무 실행)

초대/할당은 가능하지만 “제재/킬스위치”는 불가

permissions:

TENANT_INVITATION_CREATE (+ VIEW, REVOKE는 선택)

TENANT_MEMBERSHIP_VIEW

TENANT_ROLE_ASSIGN (선택: 자기 그룹 범위/allowlist 제한 필수)

BRAND_MINT, BRAND_VOID

(필요시) PASSPORT_PERMISSION_GRANT는 v1에서는 TENANT_OWNER만 추천

RETAIL_OPERATOR

permissions:

RETAIL_RELEASE

RETAIL_TRANSFER_CREATE

SERVICE_OPERATOR (추가 예정)

permissions:

SERVICE_INSPECT_WRITE 등 서비스 업무 권한 (v1에선 placeholder 없이 명확히)

5. “Assignment-only” 정책(v1 핵심)
   5.1 TENANT_ADMIN의 정의

v1에서 TENANT_ADMIN(또는 TENANT_OWNER와 구분되는 운영자)을 둔다면,

assignment(부여/회수)만 가능

status 변경/그룹 정지/승인류는 불가

즉 TENANT_ROLE_ASSIGN만 가지고:

멤버십에 role 부여/회수는 가능

멤버십 status 변경은 불가 (TENANT_MEMBERSHIP_ENFORCE 없음)

5.2 반드시 필요한 제약(가드레일)

TENANT_ROLE_ASSIGN 실행 시 정책:

부여 가능한 role allowlist 적용

예: BRAND_ADMIN_BASE, RETAIL_OPERATOR, SERVICE_OPERATOR만 허용

TENANT_OWNER 같은 최상위 role은 부여 불가(플랫폼 또는 승인 필요)

self-assign 제한

본인에게 상위 role 부여 금지 또는 approval 필수

tenant isolation 강제(토큰 tenantId == path tenantId)

6. API 권한 매핑(너희 현재 API 기준 수정안)

현재 SCOPE_MEMBERSHIP_MANAGE로 다 막은 걸 아래로 분리한다.

GroupAdminHttp

/tenants/{tenantId}/admin/groups/{id}/suspend

SCOPE_TENANT_GROUP_SUSPEND

/tenants/{tenantId}/admin/groups/{id}/unsuspend

SCOPE_TENANT_GROUP_RESUME

MembershipAdminHttp

POST /tenants/{tenantId}/admin/invitations

SCOPE_TENANT_INVITATION_CREATE

GET /tenants/{tenantId}/admin/memberships

SCOPE_TENANT_MEMBERSHIP_VIEW

중요: updateMembership 분리

role 변경:

PATCH /tenants/{tenantId}/admin/memberships/{id}/role

SCOPE_TENANT_ROLE_ASSIGN

status 변경:

PATCH /tenants/{tenantId}/admin/memberships/{id}/status

SCOPE_TENANT_MEMBERSHIP_ENFORCE

7. /authz/evaluate v1 운영 정책

기본: decisionMode=TOKEN_SNAPSHOT

민감 액션은 LIVE_RECHECK 강제

TENANT_GROUP_SUSPEND/RESUME

TENANT_MEMBERSHIP_ENFORCE

TENANT_ROLE_ASSIGN(특히 상위 role 부여)

PASSPORT_PERMISSION_GRANT

BRAND_MINT/VOID (원하면)

8. 마이그레이션(최소 단계)

permissions에 v1 신규 코드 추가 + FULFILLMENT_RELEASE deprecated 처리

roles/role_permissions에 위 권한 매핑 seed

기존 MEMBERSHIP_MANAGE로 보호된 API들을 새 권한으로 분리

로그인 권한 계산을 DB 기반으로 전환(이미 To-Be 방향)

MEMBERSHIP_MANAGE는 점진 제거(호환 기간 동안만)

네 질문(“그럼 이제 최종 결론?”)에 대한 딱 한 줄 답

admin-brand-base에게 MEMBERSHIP_MANAGE를 주지 말고,
초대/조회/할당만 가능한 분리된 tenant 운영 permission을 주고,
그룹 정지/멤버십 제재/플랫폼 승인 권한은 테넌트 오너/플랫폼으로 제한해라.

FULFILLMENT_RELEASE는 v1에서 RETAIL_RELEASE로 명확히 바꿔라.