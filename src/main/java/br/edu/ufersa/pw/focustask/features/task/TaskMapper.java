package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.features.task.dto.*;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface TaskMapper {
    TaskResponseDTO toResponse(Task entity);
    List<TaskResponseDTO> toResponseList(List<Task> entities);

    default Task toEntity(TaskCreateDTO dto, Long projectId) {
        if (dto == null) return null;
        Task task = new Task(projectId, dto.title());
        task.setDescription(dto.description());
        task.setDueDate(dto.dueDate());
        return task;
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "projectId", source = "projectId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "priority", source = "priority")
    @Mapping(target = "dueDate", source = "dueDate")
    void updateEntityFromDto(TaskUpdateDTO dto, @MappingTarget Task entity);

    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "projectId", source = "projectId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "priority", source = "priority")
    @Mapping(target = "dueDate", source = "dueDate")
    void patchEntityFromDto(TaskPatchDTO dto, @MappingTarget Task entity);

    TaskStatusDTO toStatusDto(TaskStatus status);
    TaskStatus toStatus(TaskStatusDTO status);
    TaskPriorityDTO toPriorityDto(TaskPriority priority);
    TaskPriority toPriority(TaskPriorityDTO priority);
}
