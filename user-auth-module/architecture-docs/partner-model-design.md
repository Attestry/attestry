Partner Model (Retail = Tenant) Design Note (Draft)

1. 목표


Retail 업체는 별도 Tenant로 온보딩된다.


Brand tenant와 Retail tenant는 서로 독립이며, 사용자는 필요 시 여러 tenant에 소속될 수 있다.


Brand ↔ Retail 협업은 “초대(invitation)”가 아니라 파트너 연결(PartnerLink) + 권한 위임(Delegation) 으로 모델링한다.


invitation은 tenant 내부 사용자 관리(팀원 초대/권한 부여)에만 사용한다.


2. 핵심 원칙

   2.1 Tenant isolation


모든 리소스 접근은 tenantId 경계를 기준으로 강제한다.


토큰 컨텍스트(tenantId, groupId)와 요청 경로/리소스의 tenantId가 불일치하면 거부한다.


2.2 Invitation 범위 제한


invitation은 동일 tenant 내부의 group에 사용자(사람)를 초대하는 용도로만 사용한다.


다른 tenant(=다른 회사/조직)를 초대하는 행위는 invitation으로 표현하지 않는다.


2.3 Partner 관계는 별도 리소스로 표현


Brand tenant와 Retail tenant의 관계는 사용자 단위가 아니라 조직 단위(tenant↔tenant) 관계로 관리한다.


위임/회수, 만료, 감사, 승인 흐름을 PartnerLink 기반으로 통제한다.


3. 개념 모델

   3.1 Tenant


Brand tenant A


Retail tenant B


Service tenant C (추후 동일 방식 적용 가능)


각 tenant는 자체:


groups


memberships/invitations


tenant-level policies

를 가진다.


3.2 PartnerLink (Organization-to-Organization Relationship)


PartnerLink는 Brand tenant가 Retail tenant와 협업 관계를 맺는 계약/파트너십 리소스다.


필드 예시:


partner_link_id


brand_tenant_id


partner_tenant_id (retail tenant)


partner_type (RETAIL | SERVICE | …)


status (PENDING | ACTIVE | SUSPENDED | TERMINATED)


created_by, created_at


approved_by, approved_at (선택: 승인 프로세스)


terminated_at, reason


제약:


(brand_tenant_id, partner_tenant_id, partner_type)에 대해 ACTIVE는 중복 불가


4. 권한 위임(Delegation) 모델 (추후 모듈)


현재 passport_permissions가 user-auth-module에 없으므로, 추후 workflow 모듈에서 구현한다.


4.1 위임의 목적


Brand tenant가 Retail tenant에게 특정 passport/asset/업무 범위에 대해 권한을 부여한다.


Retail tenant는 자기 tenant의 사용자/그룹을 통해 실제 업무(출고, 이전 등)를 수행한다.


4.2 Delegation Scope 예시 (permission code)


RETAIL_RELEASE


RETAIL_TRANSFER_CREATE


SERVICE_INSPECT_WRITE


SERVICE_REPAIR_WRITE


등


4.3 위임 리소스(예시)


delegations (또는 passport_permissions)


delegation_id


brand_tenant_id (grantor)


partner_tenant_id (grantee)


resource_type (PASSPORT | ASSET | COLLECTION | …)


resource_id


permission_code


status (ACTIVE | REVOKED | EXPIRED)


expires_at


granted_by, created_at


제약:


ACTIVE 중복 방지: (brand_tenant_id, partner_tenant_id, resource_type, resource_id, permission_code) UNIQUE WHERE status=ACTIVE


5. 운영 시나리오 (End-to-End)

   5.1 Retail 온보딩


Retail 업체는 별도 tenant로 가입/승인되어 tenant B를 가진다.


Retail tenant B 내부에서 invitations로 직원들을 초대하여 membership을 구성한다.


5.2 Brand ↔ Retail 파트너 연결


Brand tenant A가 Retail tenant B와 PartnerLink를 생성한다.


필요하면 승인(Platform 또는 Brand 내부 승인) 후 ACTIVE로 전이한다.


5.3 위임(Delegation) 부여


PartnerLink가 ACTIVE일 때만 위임 가능.


Brand tenant A가 특정 passport/asset에 대해 Retail tenant B에 위임을 부여한다.


5.4 Retail 업무 수행


Retail tenant B의 사용자는 자기 토큰 컨텍스트로 로그인한다.


업무 수행 시 시스템은:


(tenant isolation) B tenant 컨텍스트 확인


(delegation check) B가 해당 리소스/액션에 대해 A로부터 위임을 받았는지 확인


(내부 RBAC) B 내부 역할/권한도 확인


결과 이벤트는 감사 로그에 남긴다(brand/retail 모두 trace 가능).


6. 보안/감사 원칙


PartnerLink/Delegation 생성·변경·회수는 모두 감사 로그 필수


민감 액션(위임 부여/회수, 출고, 민팅 등)은 LIVE_RECHECK 정책 적용 가능


위임은 “조직 단위”이므로 담당자 교체에도 계약 관계가 유지된다.


7. 구현 분리 계획


user-auth-module: tenant/group/membership/invitation/RBAC


partner-link module (추후): brand↔partner 관계 관리


delegation/passport-permission module (추후): 리소스 권한 위임/회수 + 평가 API 연동


8. 결정 사항(문서에 명시)


invitation은 cross-tenant 용도로 사용하지 않는다.


retail/service는 독립 tenant이며, 협업은 PartnerLink + Delegation으로만 표현한다.


추후 위임 모듈이 도입되면 /authz/evaluate는 delegation check(ABAC)와 연동한다.



---- 



PartnerLink + Delegation API Spec (Draft)

0. 범위


Brand tenant A ↔ Retail tenant B는 독립 tenant


협업은:


PartnerLink: 조직↔조직 계약/관계


Delegation: 리소스(패스포트/자산)에 대한 액션 권한 위임


invitation은 tenant 내부 사용자 초대만 담당 (cross-tenant 초대 금지)


1. 용어


Brand Tenant: 위임 주체(Grantor)


Partner Tenant: 위임 받는 주체(Grantee) (Retail/Service)


PartnerLink: Brand tenant와 Partner tenant의 계약/관계 리소스


Delegation: 특정 resource에 대한 permission code 위임 레코드


PDP: /authz/evaluate (권한 판정), 추후 delegation check 포함


2. 권한(권장 permission codes)


플랫폼/테넌트 운영/RBAC 전환(To-Be) 기준 권장.


2.1 PartnerLink 관리(Brand tenant 내)


PARTNER_LINK_CREATE


PARTNER_LINK_READ


PARTNER_LINK_SUSPEND


PARTNER_LINK_RESUME


PARTNER_LINK_TERMINATE


2.2 Delegation 관리(Brand tenant 내)


DELEGATION_GRANT


DELEGATION_REVOKE


DELEGATION_READ


2.3 Partner 쪽(Partner tenant 내) 실행 권한


RETAIL_RELEASE, RETAIL_TRANSFER_CREATE 등은 Partner tenant 내부 RBAC로 부여


단, 실행 시 반드시 delegation이 있어야 통과(서버 정책)


v1에선 PartnerLink/Delegation 편집 권한은 Brand의 TENANT_OWNER(또는 BrandAdmin 상위)로 제한 권장.


3. PartnerLink Module API

   3.1 Partner tenant 탐색(선택)


Brand가 파트너를 연결하려면 partner tenant id를 알아야 함.


GET /partners/search?email=… (플랫폼 제공 or 디렉토리 서비스)


목적: 파트너 tenant 식별(또는 초대 코드로 연결)


응답: partnerTenantId, orgName, type(RETAIL|SERVICE), status


플랫폼 정책상 tenantId를 외부에 노출하기 싫으면 “invite code” 기반으로 대체 가능.


3.2 PartnerLink 생성(Brand → Partner 연결 요청)


POST /tenants/{brandTenantId}/partner-links


Auth: Brand tenant 컨텍스트 토큰 필요


Permission: PARTNER_LINK_CREATE


Request:


{

"partnerTenantId": "tenant_B",

"partnerType": "RETAIL",

"message": "Optional",

"proposedExpiresAt": "2026-12-31T00:00:00Z"

}


Response (201):


{

"partnerLinkId": "pl_...",

"brandTenantId": "tenant_A",

"partnerTenantId": "tenant_B",

"partnerType": "RETAIL",

"status": "PENDING",

"createdAt": "..."

}


규칙


ACTIVE 상태 중복 금지: (brandTenantId, partnerTenantId, partnerType)


partnerTenantId는 실제 존재하는 tenant여야 함


partner tenant가 SUSPENDED면 생성 불가


3.3 PartnerLink 승인/수락 (선택 모델)


너희 운영 모델에 따라 2가지 중 하나.


모델 1) 플랫폼 승인형(권장: 초기 안전)


POST /admin/partner-links/{partnerLinkId}/approve


Permission: PLATFORM_ADMIN 또는 PARTNER_LINK_APPROVE


Response: status=ACTIVE


POST /admin/partner-links/{partnerLinkId}/reject


Request: { "reason": "..." }


Response: status=REJECTED


모델 2) 파트너 tenant 수락형(파트너가 직접 수락)


POST /tenants/{partnerTenantId}/partner-links/{partnerLinkId}/accept


Auth: Partner tenant 컨텍스트 토큰


Permission: PARTNER_LINK_ACCEPT


Response: status=ACTIVE


3.4 PartnerLink 조회


GET /tenants/{tenantId}/partner-links


Permission: PARTNER_LINK_READ


tenantId는 brandTenantId 또는 partnerTenantId일 수 있음


Response: list with status, partnerType, dates


GET /partner-links/{partnerLinkId}


Permission: PARTNER_LINK_READ


둘 중 어느 tenant(brand/partner) 소속이든 접근 가능(컨텍스트 검증)


3.5 PartnerLink 중지/재개/종료


POST /partner-links/{partnerLinkId}/suspend


Permission: PARTNER_LINK_SUSPEND


효과: status=SUSPENDED (delegation grant/usage 차단)


POST /partner-links/{partnerLinkId}/resume


Permission: PARTNER_LINK_RESUME


효과: status=ACTIVE 복귀


POST /partner-links/{partnerLinkId}/terminate


Permission: PARTNER_LINK_TERMINATE


Request: { "reason": "..." }


효과: status=TERMINATED


규칙: terminate 시 ACTIVE delegations은 자동 REVOKE 처리(또는 사용 불가로 간주)


4. Delegation Module API (Passport Permission)

   4.1 Delegation Grant (Brand가 Partner에게 권한 위임)


POST /tenants/{brandTenantId}/delegations


Permission: DELEGATION_GRANT


Request:


{

"partnerLinkId": "pl_...",

"partnerTenantId": "tenant_B",

"resourceType": "PASSPORT",

"resourceId": "passport_123",

"permissionCode": "RETAIL_RELEASE",

"expiresAt": "2026-03-31T00:00:00Z",

"note": "optional"

}


Response (201):


{

"delegationId": "del_...",

"status": "ACTIVE",

"brandTenantId": "tenant_A",

"partnerTenantId": "tenant_B",

"resourceType": "PASSPORT",

"resourceId": "passport_123",

"permissionCode": "RETAIL_RELEASE",

"expiresAt": "..."

}


규칙


partnerLinkId는 ACTIVE여야 함


partnerTenantId는 partnerLink에 포함된 tenant여야 함


중복 방지(권장): ACTIVE 중복 금지

(brandTenantId, partnerTenantId, resourceType, resourceId, permissionCode) where status=ACTIVE


expiresAt 없으면 무기한(운영 정책에 따라 제한 가능)


4.2 Delegation Revoke (회수)


POST /delegations/{delegationId}/revoke


Permission: DELEGATION_REVOKE


Request: { "reason": "..." }


Response:


{ "delegationId": "...", "status": "REVOKED", "revokedAt": "..." }


POST /tenants/{brandTenantId}/delegations/revoke


bulk revoke용(리소스/파트너 기준)


Request:


{

"partnerTenantId": "tenant_B",

"resourceType": "PASSPORT",

"resourceId": "passport_123",

"permissionCode": "RETAIL_RELEASE"

}

4.3 Delegation 조회


GET /tenants/{tenantId}/delegations


Permission: DELEGATION_READ


Query:


partnerTenantId


resourceType, resourceId


status


tenantId가 brandTenantId이면 “내가 준 delegations”


tenantId가 partnerTenantId이면 “내가 받은 delegations”


GET /delegations/{delegationId}


Permission: DELEGATION_READ


5. 권한 평가(/authz/evaluate) 연동 방식

   5.1 Partner tenant에서 액션 실행 시 체크 흐름


예: Retail tenant B 사용자가 RETAIL_RELEASE를 실행한다.


서버는:


토큰 권한(스코프) 체크: B 내부 RBAC에서 RETAIL_RELEASE가 있는가?


tenant isolation: 요청 리소스가 B 컨텍스트에서 접근 가능한가?


delegation check:


해당 passport/resource에 대해


Brand tenant A로부터 B에게 RETAIL_RELEASE 위임이 ACTIVE인지


partnerLink가 ACTIVE인지


통과 시 실행


5.2 /authz/evaluate 확장(권장)


POST /authz/evaluate


request:


{

"tenantId": "tenant_B",

"action": "RETAIL_RELEASE",

"resourceType": "PASSPORT",

"resourceRef": "passport_123",

"decisionMode": "TOKEN_SNAPSHOT"

}


response:


{

"allowed": true,

"reasonCode": null,

"decisionMode": "LIVE_RECHECK",

"effectiveScopes": ["RETAIL_RELEASE", "..."],

"traceId": "..."

}


LIVE_RECHECK가 필요한 액션


delegation grant/revoke


partnerLink suspend/terminate


release/mint 같은 고위험 액션


6. 감사 로그(필수)


모든 PartnerLink/Delegation 변경은 audit 이벤트로 남긴다.


PartnerLink: create/approve/reject/suspend/resume/terminate


Delegation: grant/revoke/expire

로그 필드 권장:


actor(userId), actorTenantId


brandTenantId, partnerTenantId


resourceType/resourceId/permissionCode


reason, timestamps, traceId


7. “초대(invitation)” 사용 정책 (명시)


invitation은 tenant 내부 사용자 초대 용도로만 사용


cross-tenant 협업은 PartnerLink/Delegation으로만 구현


파트너 업체의 직원은 자기 tenant에서 관리하며, brand tenant로 “들어오지 않는다”


8. 구현 우선순위(로드맵)


PartnerLink API (create/read/approve/terminate) + audit


Delegation API (grant/revoke/read) + audit


/authz/evaluate에 delegation check 연동


도메인 서비스(출고/이전/서비스 기록)에서 evaluate 사용

## 9. 현재 리팩토링 범위

- 포함:
  - Phase 1 (Retail = 별도 tenant 온보딩)
  - Phase 2 (workflow PartnerLink)
  - Phase 3 (workflow Delegation, 다음 단계)
- 제외:
  - Phase 4 (`product`/`ledger` 실행 시 delegation evaluate 연결)

Phase 4는 추후 별도 문서로 설계/적용한다.

## 10. 현재 구현 상태

- 완료:
  - Phase 1: Retail 승인 시 별도 retail tenant 생성
  - Phase 2: workflow PartnerLink API/모델/DB
  - Phase 3: workflow Delegation API/모델/DB + internal evaluate
- 보류:
  - Phase 4: `product`/`ledger` 실행 경로에 delegation evaluate 강제 연동
