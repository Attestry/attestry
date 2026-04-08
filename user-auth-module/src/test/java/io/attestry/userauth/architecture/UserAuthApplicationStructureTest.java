package io.attestry.userauth.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class UserAuthApplicationStructureTest {

    private static final Path APPLICATION_ROOT = Path.of("src/main/java/io/attestry/userauth/application");

    @Test
    void membershipContextUsesEntrypointAndInternalPackages() {
        Path membershipRoot = APPLICATION_ROOT.resolve("membership");

        assertTrue(Files.isDirectory(membershipRoot.resolve("command")));
        assertTrue(Files.isDirectory(membershipRoot.resolve("query")));
        assertTrue(Files.isDirectory(membershipRoot.resolve("internal")));
    }

    @Test
    void onboardingContextUsesEntrypointAndInternalPackages() {
        Path onboardingRoot = APPLICATION_ROOT.resolve("onboarding");

        assertTrue(Files.isDirectory(onboardingRoot.resolve("command")));
        assertTrue(Files.isDirectory(onboardingRoot.resolve("query")));
        assertTrue(Files.isDirectory(onboardingRoot.resolve("internal")));
    }

    @Test
    void membershipLegacyPackagesAreRemoved() throws IOException {
        Path membershipRoot = APPLICATION_ROOT.resolve("membership");

        List<Path> forbiddenDirectories = List.of(
            membershipRoot.resolve("usecase"),
            membershipRoot.resolve("assembler"),
            membershipRoot.resolve("policy")
        );
        for (Path path : forbiddenDirectories) {
            assertFalse(Files.exists(path), path + " should not exist");
        }

        try (Stream<Path> javaFiles = Files.walk(membershipRoot)) {
            assertFalse(
                javaFiles
                    .filter(path -> path.toString().endsWith(".java"))
                    .anyMatch(path ->
                        path.toString().contains("/usecase/")
                            || path.toString().contains("/assembler/")
                            || path.toString().contains("/policy/")
                    ),
                "membership application should not keep legacy package layout"
            );
        }
    }

    @Test
    void onboardingLegacyPackagesAreRemoved() throws IOException {
        Path onboardingRoot = APPLICATION_ROOT.resolve("onboarding");

        List<Path> forbiddenDirectories = List.of(
            onboardingRoot.resolve("usecase"),
            onboardingRoot.resolve("assembler")
        );
        for (Path path : forbiddenDirectories) {
            assertFalse(Files.exists(path), path + " should not exist");
        }

        try (Stream<Path> javaFiles = Files.walk(onboardingRoot)) {
            assertFalse(
                javaFiles
                    .filter(path -> path.toString().endsWith(".java"))
                    .anyMatch(path ->
                        path.toString().contains("/usecase/")
                            || path.toString().contains("/assembler/")
                    ),
                "onboarding application should not keep legacy package layout"
            );
        }
    }
}
