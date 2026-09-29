package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.features.user.dto.*;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface UserMapper {
    UserResponseDTO toResponse(User entity);
    List<UserResponseDTO> toResponseList(List<User> entities);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    void updateEntityFromDto(UserUpdateDTO dto, @MappingTarget User entity);

    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    void patchEntityFromDto(UserPatchDTO dto, @MappingTarget User entity);
}
