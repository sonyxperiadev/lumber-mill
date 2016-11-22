package lumbermill.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

    public static ArrayNode createArrayNode(double... values) {
        ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
        for (double value : values) {
            arrayNode.add(value);
        }
        return arrayNode;
    }
}
