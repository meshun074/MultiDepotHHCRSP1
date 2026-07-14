package Data;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

public class ReadData {
    private static final ObjectMapper mapper = new ObjectMapper();
    public static InstancesClass read(File file){
        InstancesClass instance;
        try {
            return mapper.readValue(file, InstancesClass.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
