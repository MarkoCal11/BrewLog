package hr.tvz.brewlog.brewlogapp.mapper;

import hr.tvz.brewlog.brewlogapp.dto.AdminCoffeeBeanResponse;
import hr.tvz.brewlog.brewlogapp.dto.CoffeeBeanCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.CoffeeBeanResponse;
import hr.tvz.brewlog.brewlogapp.dto.CoffeeBeanUpdateRequest;
import hr.tvz.brewlog.brewlogapp.model.CoffeeBean;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {FlavorNoteMapper.class})
public interface CoffeeBeanMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "flavorNotes", ignore = true)
    @Mapping(target = "user", ignore = true)
    CoffeeBean toEntity(CoffeeBeanCreateRequest request);

    @Mapping(target = "averageRating", source = "averageRating")
    @Mapping(target = "brewCount", source = "brewCount")
    CoffeeBeanResponse toResponse(CoffeeBean entity, Double averageRating, Integer brewCount);

    @Mapping(target = "averageRating", source = "averageRating")
    @Mapping(target = "brewCount", source = "brewCount")
    @Mapping(target = "ownerId", source = "entity.user.id")
    @Mapping(target = "ownerUsername", source = "entity.user.username")
    AdminCoffeeBeanResponse toAdminResponse(CoffeeBean entity, Double averageRating, Integer brewCount);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntityFromRequest(CoffeeBeanUpdateRequest request, @MappingTarget CoffeeBean entity);
}
