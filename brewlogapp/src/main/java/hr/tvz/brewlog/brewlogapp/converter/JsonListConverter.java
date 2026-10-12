package hr.tvz.brewlog.brewlogapp.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Converter
public abstract class JsonListConverter<T> implements AttributeConverter<List<T>, String> {

    private final ObjectMapper objectMapper;

    protected JsonListConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected abstract TypeReference<List<T>> typeRef();

    @Override
    public String convertToDatabaseColumn(List<T> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return objectMapper.writeValueAsString(attribute);
    }

    @Override
    public List<T> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(dbData, typeRef());
    }
}
