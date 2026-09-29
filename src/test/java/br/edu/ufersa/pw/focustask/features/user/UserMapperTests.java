package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.features.user.dto.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class UserMapperTests {
    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void createsThroughDomainConstructorAndMapsResponses() {
        User user = new User("Carol", "  CAROL@example.com  ", "test-hash");
        assertNull(user.getId());
        assertEquals("carol@example.com", user.getEmail());
        ReflectionTestUtils.setField(user, "id", 7L);
        assertEquals(new UserResponseDTO(7L, "Carol", "carol@example.com"), mapper.toResponse(user));
        assertEquals(List.of(mapper.toResponse(user)), mapper.toResponseList(List.of(user)));
    }

    @Test
    void updateAndPatchPreserveIdentityAndDomainNormalization() {
        User user = new User("Before", "before@example.com", "test-hash");
        ReflectionTestUtils.setField(user, "id", 7L);
        mapper.updateEntityFromDto(new UserUpdateDTO("After", "AFTER@example.com"), user);
        mapper.patchEntityFromDto(new UserPatchDTO(null, null), user);
        assertEquals(new UserResponseDTO(7L, "After", "after@example.com"), mapper.toResponse(user));
        mapper.patchEntityFromDto(new UserPatchDTO("Patched", null), user);
        assertEquals("Patched", user.getName());
        assertEquals("after@example.com", user.getEmail());
        assertEquals(7L, user.getId());
    }
}
