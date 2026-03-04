package io.attestry.userauth.application.port;

import java.time.Instant;
import java.util.Set;

public interface MembershipPermissionOverridePort {
    void upsertPermissionOverrides(String membershipId, Set<String> permissionCodes,
                                    String source, String reason, String actorUserId, Instant now);

    void deletePermissionOverrides(String membershipId, Set<String> permissionCodes);

    Set<String> applyPermissionTemplateToMembership(String membershipId, String templateCode,
                                                     String reason, String actorUserId, Instant now);

    Set<String> revokePermissionTemplateFromMembership(String membershipId, String templateCode);
}
