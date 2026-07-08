package org.example.Data;

import java.nio.file.Path;
import java.util.Locale;

public enum DatasetFamily {
    EXTENDED("Extended"),
    INSTANCE("instance"),
    ITALIAN("Italian"),
    KUMMER("kummer");

    private final String directoryName;

    DatasetFamily(String directoryName) {
        this.directoryName = directoryName;
    }

    public Path directoryUnder(Path dataRoot) {
        return dataRoot.resolve(directoryName);
    }

    public static DatasetFamily parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Dataset family must not be blank");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unknown dataset family '" + value
                            + "'. Supported families: EXTENDED, INSTANCE, ITALIAN, KUMMER",
                    exception);
        }
    }
}
