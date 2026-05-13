package com.example.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonFieldExtractor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonFieldExtractor() {
    }

    public static String read(String json, String fieldName) throws Exception {
        JsonNode node = OBJECT_MAPPER.readTree(json);
        return node.get(fieldName).asText();
    }
}
