package io.attestry.product.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProductApplicationStructureTest {

    private static final Path APPLICATION_ROOT = Path.of("src/main/java/io/attestry/product/application");

    @Test
    void productCommandAndQueryPackagesExposeInternalCollaborators() {
        assertTrue(Files.isDirectory(APPLICATION_ROOT.resolve("command/internal")));
        assertTrue(Files.isDirectory(APPLICATION_ROOT.resolve("query/internal")));
    }

    @Test
    void legacyProductPackagesAreRemoved() throws IOException {
        List<Path> forbiddenDirectories = List.of(
            APPLICATION_ROOT.resolve("command/usecase"),
            APPLICATION_ROOT.resolve("command/support"),
            APPLICATION_ROOT.resolve("policy"),
            APPLICATION_ROOT.resolve("query/usecase"),
            APPLICATION_ROOT.resolve("query/assembler")
        );
        for (Path path : forbiddenDirectories) {
            assertFalse(Files.exists(path), path + " should not exist");
        }

        try (Stream<Path> javaFiles = Files.walk(APPLICATION_ROOT)) {
            assertFalse(
                javaFiles
                    .filter(path -> path.toString().endsWith(".java"))
                    .anyMatch(path ->
                        path.toString().contains("/command/usecase/")
                            || path.toString().contains("/command/support/")
                            || path.toString().contains("/policy/")
                            || path.toString().contains("/query/usecase/")
                            || path.toString().contains("/query/assembler/")
                    ),
                "product application should not keep legacy package layout"
            );
        }
    }
}
