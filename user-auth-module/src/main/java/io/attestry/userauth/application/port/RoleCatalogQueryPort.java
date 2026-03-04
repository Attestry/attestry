package io.attestry.userauth.application.port;

import java.util.Set;

public interface RoleCatalogQueryPort {
    Set<String> findGlobalEnabledRoleCodes();
}
