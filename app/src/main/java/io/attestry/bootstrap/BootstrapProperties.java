package io.attestry.bootstrap;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.bootstrap")
public class BootstrapProperties {

    private PlatformAdmin platformAdmin = new PlatformAdmin();

    @Getter
    @Setter
    public static class PlatformAdmin {
        private boolean enabled = false;
        private String email = "platform.admin@attestry.local";
        private String password = "PlatformAdm1n!2026";
        private String phone = "010-0000-0000";
    }
}
