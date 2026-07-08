package org.example.Data;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Resolves an exact filename, relative path, or unique filename prefix inside
 * one selected dataset family.
 */
public final class DatasetResolver {
    public static final Path DEFAULT_DATA_ROOT =
            Path.of("src", "main", "java", "org", "example", "Data");

    private final Path dataRoot;

    public DatasetResolver(Path dataRoot) {
        this.dataRoot = Objects.requireNonNull(dataRoot, "dataRoot").normalize();
    }

    public Path resolve(DatasetFamily family, String selector) {
        Objects.requireNonNull(family, "family");
        if (selector == null || selector.isBlank()) {
            throw new IllegalArgumentException("Instance filename or prefix must not be blank");
        }

        Path familyDirectory = family.directoryUnder(dataRoot);
        if (!Files.isDirectory(familyDirectory)) {
            throw new IllegalArgumentException(
                    "Dataset directory does not exist: " + familyDirectory.toAbsolutePath());
        }

        Path suppliedPath = Path.of(selector);
        Path directCandidate = suppliedPath.isAbsolute()
                ? suppliedPath.normalize()
                : familyDirectory.resolve(suppliedPath).normalize();
        ensureInsideFamily(familyDirectory, directCandidate);
        if (Files.isRegularFile(directCandidate)) {
            return directCandidate.toAbsolutePath();
        }

        Path jsonCandidate = directCandidate.resolveSibling(
                directCandidate.getFileName() + ".json");
        if (Files.isRegularFile(jsonCandidate)) {
            return jsonCandidate.toAbsolutePath();
        }

        Path relativeParent = suppliedPath.getParent();
        Path searchDirectory = relativeParent == null
                ? familyDirectory
                : familyDirectory.resolve(relativeParent).normalize();
        ensureInsideFamily(familyDirectory, searchDirectory);
        if (!Files.isDirectory(searchDirectory)) {
            throw new IllegalArgumentException(
                    "Dataset subdirectory does not exist: " + searchDirectory.toAbsolutePath());
        }

        String prefix = suppliedPath.getFileName().toString();
        List<Path> matches;
        try (Stream<Path> files = Files.walk(searchDirectory)) {
            matches = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Unable to search dataset directory: " + searchDirectory, exception);
        }

        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                    "No " + family + " instance matches '" + selector + "'");
        }
        if (matches.size() > 1) {
            String candidates = matches.stream()
                    .limit(5)
                    .map(familyDirectory::relativize)
                    .map(Path::toString)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
            throw new IllegalArgumentException(
                    "Instance selector '" + selector + "' is ambiguous. Matches: " + candidates);
        }
        return matches.getFirst().toAbsolutePath().normalize();
    }

    private static void ensureInsideFamily(Path familyDirectory, Path candidate) {
        Path normalizedFamily = familyDirectory.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        if (!normalizedCandidate.startsWith(normalizedFamily)) {
            throw new IllegalArgumentException(
                    "Instance selector must stay inside dataset family directory: " + candidate);
        }
    }
}
