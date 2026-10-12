package hr.tvz.brewlog.brewlogapp.converter;

import hr.tvz.brewlog.brewlogapp.dto.MethodBreakdownPoint;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@Converter
public class MethodBreakdownConverter extends JsonListConverter<MethodBreakdownPoint> {

    public MethodBreakdownConverter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    protected TypeReference<List<MethodBreakdownPoint>> typeRef() {
        return new TypeReference<>() {};
    }
}
