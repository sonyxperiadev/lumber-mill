package lumbermill.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okio.ByteString;

import java.io.IOException;


public class Json {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static JsonNode parse(ByteString string) {
        try {
            return OBJECT_MAPPER.readTree(string.utf8());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
