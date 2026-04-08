package io.attestry.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RuntimePackageStructureTest {

    private static final Path MAIN_ROOT = Path.of("src/main/java/io/attestry");

    @Test
    void runtimeSubsystemsLiveUnderRuntimePackage() {
        assertTrue(Files.isDirectory(MAIN_ROOT.resolve("runtime/workflowprojection")));
        assertTrue(Files.isDirectory(MAIN_ROOT.resolve("runtime/notificationoutbox")));
        assertTrue(Files.isDirectory(MAIN_ROOT.resolve("runtime/ledgeroutbox/model")));
        assertTrue(Files.isDirectory(MAIN_ROOT.resolve("runtime/ledgeroutbox/publish")));
        assertTrue(Files.isDirectory(MAIN_ROOT.resolve("runtime/ledgeroutbox/schedule")));
    }

    @Test
    void legacyRuntimeDirectoriesAreRemoved() throws IOException {
        List<Path> forbiddenDirectories = List.of(
            MAIN_ROOT.resolve("job/notification"),
            MAIN_ROOT.resolve("job/outbox"),
            MAIN_ROOT.resolve("kafka/workflow")
        );
        for (Path path : forbiddenDirectories) {
            assertFalse(Files.exists(path), path + " should not exist");
        }

        try (Stream<Path> runtimeFiles = Files.walk(MAIN_ROOT.resolve("runtime"))) {
            assertTrue(runtimeFiles.anyMatch(path -> path.toString().endsWith(".java")));
        }
    }
}
