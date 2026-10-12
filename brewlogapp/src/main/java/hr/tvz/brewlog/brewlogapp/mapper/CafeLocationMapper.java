package hr.tvz.brewlog.brewlogapp.mapper;

import hr.tvz.brewlog.brewlogapp.dto.AdminCafeLocationResponse;
import hr.tvz.brewlog.brewlogapp.dto.CafeLocationCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.CafeLocationResponse;
import hr.tvz.brewlog.brewlogapp.dto.CafeLocationUpdateRequest;
import hr.tvz.brewlog.brewlogapp.model.CafeLocation;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CafeLocationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    CafeLocation toEntity(CafeLocationCreateRequest request);

    CafeLocationResponse toResponse(CafeLocation entity);

    @Mapping(target = "ownerId", source = "entity.user.id")
    @Mapping(target = "ownerUsername", source = "entity.user.username")
    AdminCafeLocationResponse toAdminResponse(CafeLocation entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntityFromRequest(CafeLocationUpdateRequest request, @MappingTarget CafeLocation entity);
}
