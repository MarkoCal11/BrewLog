package hr.tvz.brewlog.brewlogapp.converter;

import hr.tvz.brewlog.brewlogapp.dto.RatingTrendPoint;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@Converter
public class RatingTrendConverter extends JsonListConverter<RatingTrendPoint> {

    public RatingTrendConverter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    protected TypeReference<List<RatingTrendPoint>> typeRef() {
        return new TypeReference<>() {};
    }
}
