package hr.tvz.brewlog.brewlogapp.converter;

import hr.tvz.brewlog.brewlogapp.dto.BrewsPerDayPoint;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@Converter
public class BrewsPerDayConverter extends JsonListConverter<BrewsPerDayPoint>{

    public BrewsPerDayConverter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    protected TypeReference<List<BrewsPerDayPoint>> typeRef() {
        return new TypeReference<>() {};
    }
}
