package org.example.Data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;

public final class ReadData {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ReadData() {
    }

    /**
     * Reads either a legacy instance file or an Extended instance file.
     */
    public static InstancesClass read(File file) {
        Objects.requireNonNull(file, "file");
        try {
            InstancesClass instance = MAPPER.readValue(file, InstancesClass.class);
            instance.initializeAfterDeserialization();
            return instance;
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read instance file: " + file, exception);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid instance file: " + file, exception);
        }
    }

    public static InstancesClass read(Path path) {
        Objects.requireNonNull(path, "path");
        return read(path.toFile());
    }
}
