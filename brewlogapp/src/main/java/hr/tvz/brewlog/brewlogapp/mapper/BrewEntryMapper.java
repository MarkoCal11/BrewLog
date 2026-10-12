package hr.tvz.brewlog.brewlogapp.mapper;

import hr.tvz.brewlog.brewlogapp.dto.AdminBrewEntryResponse;
import hr.tvz.brewlog.brewlogapp.dto.BrewEntryCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.BrewEntryResponse;
import hr.tvz.brewlog.brewlogapp.dto.BrewEntryUpdateRequest;
import hr.tvz.brewlog.brewlogapp.model.BrewEntry;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BrewEntryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "coffeeBean", ignore = true)
    @Mapping(target = "user", ignore = true)
    BrewEntry toEntity(BrewEntryCreateRequest request);

    BrewEntryResponse toResponse(BrewEntry entity);

    @Mapping(target = "ownerId", source = "user.id")
    @Mapping(target = "ownerUsername", source = "user.username")
    AdminBrewEntryResponse toAdminResponse(BrewEntry entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "coffeeBean", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntityFromRequest(BrewEntryUpdateRequest request, @MappingTarget BrewEntry entity);
}
