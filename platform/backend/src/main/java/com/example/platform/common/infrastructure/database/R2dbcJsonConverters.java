package com.example.platform.common.infrastructure.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.spi.ConnectionFactory;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;

@Configuration
public class R2dbcJsonConverters {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions(ObjectMapper objectMapper, ConnectionFactory connectionFactory) {
        R2dbcDialect dialect = DialectResolver.getDialect(connectionFactory);
        return R2dbcCustomConversions.of(
                dialect,
                List.of(
                        new JsonNodeToStringConverter(),
                        new StringToJsonNodeConverter(objectMapper),
                        new PostgresJsonToJsonNodeConverter(objectMapper)
                )
        );
    }

    @WritingConverter
    private static final class JsonNodeToStringConverter implements Converter<JsonNode, String> {
        @Override
        public String convert(JsonNode source) {
            return source == null ? null : source.toString();
        }
    }

    @ReadingConverter
    private record StringToJsonNodeConverter(ObjectMapper objectMapper) implements Converter<String, JsonNode> {
        @Override
        public JsonNode convert(String source) {
            return readJson(objectMapper, source);
        }
    }

    @ReadingConverter
    private record PostgresJsonToJsonNodeConverter(ObjectMapper objectMapper) implements ConditionalGenericConverter {
        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Set.of(new ConvertiblePair(Object.class, JsonNode.class));
        }

        @Override
        public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
            return JsonNode.class.isAssignableFrom(targetType.getType())
                    && sourceType.getType().getName().startsWith("io.r2dbc.postgresql.codec.Json");
        }

        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            if (source == null) {
                return null;
            }
            try {
                Method asString = source.getClass().getMethod("asString");
                return readJson(objectMapper, (String) asString.invoke(source));
            } catch (ReflectiveOperationException exception) {
                throw new IllegalArgumentException("Failed to read PostgreSQL JSON database value", exception);
            }
        }
    }

    private static JsonNode readJson(ObjectMapper objectMapper, String source) {
        if (source == null || source.isBlank()) {
            return objectMapper.nullNode();
        }
        try {
            JsonNode parsed = objectMapper.readTree(source);
            if (parsed != null && parsed.isTextual()) {
                String text = parsed.asText();
                if (text.startsWith("{") || text.startsWith("[")) {
                    return objectMapper.readTree(text);
                }
            }
            return parsed;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to read JSON database value", exception);
        }
    }
}
