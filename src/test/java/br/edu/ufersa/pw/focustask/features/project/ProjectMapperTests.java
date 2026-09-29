package br.edu.ufersa.pw.focustask.features.project;

import br.edu.ufersa.pw.focustask.features.project.dto.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProjectMapperTests {
    private final ProjectMapper mapper = Mappers.getMapper(ProjectMapper.class);

    @Test
    void createsWithExternalOwnerAndMapsOnlyIdsInResponse() {
        Project project = mapper.toEntity(new ProjectCreateDTO("Study", "Notes"), 7L);
        assertNull(project.getId());
        ReflectionTestUtils.setField(project, "id", 42L);
        assertEquals(new ProjectResponseDTO(42L, 7L, "Study", "Notes"), mapper.toResponse(project));
        assertEquals(List.of(mapper.toResponse(project)), mapper.toResponseList(List.of(project)));
    }

    @Test
    void patchKeepsNullFieldsWhilePutClearsDescriptionWithoutChangingOwnerOrId() {
        Project project = new Project(7L, "Study", "Notes");
        ReflectionTestUtils.setField(project, "id", 42L);
        mapper.patchEntityFromDto(new ProjectPatchDTO(null, null), project);
        assertEquals("Study", project.getName());
        assertEquals("Notes", project.getDescription());
        mapper.patchEntityFromDto(new ProjectPatchDTO("Patched", "New notes"), project);
        assertEquals("Patched", project.getName());
        assertEquals("New notes", project.getDescription());
        mapper.updateEntityFromDto(new ProjectUpdateDTO("Updated", null), project);
        assertEquals(new ProjectResponseDTO(42L, 7L, "Updated", null), mapper.toResponse(project));
    }
}
