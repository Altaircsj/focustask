package br.edu.ufersa.pw.focustask.features.project;

import br.edu.ufersa.pw.focustask.features.project.dto.*;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface ProjectMapper {
    ProjectResponseDTO toResponse(Project entity);
    List<ProjectResponseDTO> toResponseList(List<Project> entities);

    default Project toEntity(ProjectCreateDTO dto, Long userId) {
        if (dto == null) return null;
        Project project = new Project(userId, dto.name(), dto.description());
        project.updateAppearance(dto.color(), dto.tags());
        return project;
    }

    @AfterMapping
    default void applyUpdate(ProjectUpdateDTO dto, @MappingTarget Project entity) {
        entity.updateAppearance(dto.color(), dto.tags());
    }

    @AfterMapping
    default void applyPatch(ProjectPatchDTO dto, @MappingTarget Project entity) {
        entity.updateAppearance(dto.color(), dto.tags());
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    void updateEntityFromDto(ProjectUpdateDTO dto, @MappingTarget Project entity);

    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    void patchEntityFromDto(ProjectPatchDTO dto, @MappingTarget Project entity);
}
