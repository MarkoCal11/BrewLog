package hr.tvz.brewlog.brewlogapp.mapper;

import hr.tvz.brewlog.brewlogapp.dto.FlavorNoteCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.FlavorNoteResponse;
import hr.tvz.brewlog.brewlogapp.dto.FlavorNoteUpdateRequest;
import hr.tvz.brewlog.brewlogapp.model.FlavorNote;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FlavorNoteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    FlavorNote toEntity(FlavorNoteCreateRequest request);

    @Mapping(target = "global", expression = "java(entity.getUser() == null)")
    FlavorNoteResponse toResponse(FlavorNote entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntityFromRequest(FlavorNoteUpdateRequest request, @MappingTarget FlavorNote entity);
}
