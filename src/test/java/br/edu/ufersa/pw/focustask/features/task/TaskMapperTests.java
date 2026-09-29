package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.features.task.dto.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TaskMapperTests {
    private final TaskMapper mapper = Mappers.getMapper(TaskMapper.class);

    @Test
    void creationKeepsDefaultsAndExternalProjectAndMapsResponse() {
        Task task = mapper.toEntity(new TaskCreateDTO("Study", null, null), 42L);
        assertNull(task.getId());
        assertEquals(42L, task.getProjectId());
        assertEquals(TaskStatus.TODO, task.getStatus());
        assertEquals(TaskPriority.MEDIUM, task.getPriority());
        assertNull(task.getDueDate());
        ReflectionTestUtils.setField(task, "id", 123L);
        assertEquals(new TaskResponseDTO(123L, 42L, "Study", null,
                TaskStatusDTO.TODO, TaskPriorityDTO.MEDIUM, null), mapper.toResponse(task));
        assertEquals(List.of(mapper.toResponse(task)), mapper.toResponseList(List.of(task)));
    }

    @Test
    void patchIgnoresNullAndPutClearsOptionalFields() {
        LocalDate past = LocalDate.of(2000, 1, 1);
        Task task = mapper.toEntity(new TaskCreateDTO("Study", "Notes", past), 42L);
        ReflectionTestUtils.setField(task, "id", 123L);
        var before = mapper.toResponse(task);
        mapper.patchEntityFromDto(new TaskPatchDTO(null, null, null, null, null, null), task);
        assertEquals(before, mapper.toResponse(task));
        mapper.patchEntityFromDto(new TaskPatchDTO(43L, "Patched", null,
                TaskStatusDTO.IN_PROGRESS, TaskPriorityDTO.HIGH, null), task);
        assertEquals(43L, task.getProjectId());
        assertEquals("Patched", task.getTitle());
        assertEquals("Notes", task.getDescription());
        assertEquals(past, task.getDueDate());
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertEquals(TaskPriority.HIGH, task.getPriority());
        mapper.updateEntityFromDto(new TaskUpdateDTO(44L, "Updated", null,
                TaskStatusDTO.DONE, TaskPriorityDTO.LOW, null), task);
        assertEquals(new TaskResponseDTO(123L, 44L, "Updated", null,
                TaskStatusDTO.DONE, TaskPriorityDTO.LOW, null), mapper.toResponse(task));
    }

    @Test
    void enumMappingsRoundTripWithoutChangingNames() {
        for (TaskStatus status : TaskStatus.values()) {
            assertEquals(status.name(), mapper.toStatusDto(status).name());
            assertEquals(status, mapper.toStatus(mapper.toStatusDto(status)));
        }
        for (TaskPriority priority : TaskPriority.values()) {
            assertEquals(priority.name(), mapper.toPriorityDto(priority).name());
            assertEquals(priority, mapper.toPriority(mapper.toPriorityDto(priority)));
        }
    }
}
