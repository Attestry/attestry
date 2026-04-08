package io.attestry.workflow.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class WorkflowApplicationStructureTest {

    private static final Path APPLICATION_ROOT = Path.of("src/main/java/io/attestry/workflow/application");

    @Test
    void workflowContextsExposeExpectedEntrypointPackages() {
        List<String> commandContexts = List.of(
            "claim",
            "delegation",
            "distribution",
            "manual",
            "partner",
            "servicerequest",
            "shipment",
            "transfer"
        );
        for (String context : commandContexts) {
            assertTrue(Files.isDirectory(APPLICATION_ROOT.resolve(context).resolve("command")));
        }

        List<String> queryContexts = List.of("distribution", "partner", "servicerequest", "shipment", "transfer");
        for (String context : queryContexts) {
            assertTrue(Files.isDirectory(APPLICATION_ROOT.resolve(context).resolve("query")));
        }
    }

    @Test
    void internalCollaboratorsAreIsolatedAndLegacyPackagesAreGone() throws IOException {
        List<String> internalContexts = List.of("claim", "distribution", "manual", "servicerequest", "transfer");
        for (String context : internalContexts) {
            assertTrue(Files.isDirectory(APPLICATION_ROOT.resolve(context).resolve("internal")));
        }

        List<Path> forbiddenDirectories = List.of(
            APPLICATION_ROOT.resolve("claim/usecase"),
            APPLICATION_ROOT.resolve("manual/usecase"),
            APPLICATION_ROOT.resolve("distribution/usecase"),
            APPLICATION_ROOT.resolve("transfer/usecase"),
            APPLICATION_ROOT.resolve("servicerequest/usecase"),
            APPLICATION_ROOT.resolve("transfer/support"),
            APPLICATION_ROOT.resolve("transfer/policy"),
            APPLICATION_ROOT.resolve("servicerequest/support"),
            APPLICATION_ROOT.resolve("servicerequest/policy"),
            APPLICATION_ROOT.resolve("servicerequest/assembler")
        );
        for (Path path : forbiddenDirectories) {
            assertFalse(Files.exists(path), path + " should not exist");
        }

        try (Stream<Path> javaFiles = Files.walk(APPLICATION_ROOT)) {
            assertFalse(
                javaFiles
                    .filter(path -> path.toString().endsWith(".java"))
                    .anyMatch(path -> path.toString().contains("/usecase/")),
                "workflow application should not keep legacy usecase package"
            );
        }
    }
}
