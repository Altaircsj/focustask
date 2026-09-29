package br.edu.ufersa.pw.focustask;

import br.edu.ufersa.pw.focustask.features.user.dto.*;
import br.edu.ufersa.pw.focustask.features.project.dto.*;
import br.edu.ufersa.pw.focustask.features.task.dto.*;
import br.edu.ufersa.pw.focustask.features.focusSession.dto.*;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.LocalDate;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class DtoValidationTests {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void startValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @ParameterizedTest
    @MethodSource("validInputs")
    void acceptsOptionalFieldsAndExistingDomainLimits(Object dto) {
        assertTrue(validator.validate(dto).isEmpty(), () -> validator.validate(dto).toString());
    }

    static Stream<Object> validInputs() {
        LocalDate past = LocalDate.of(2000, 1, 1);
        return Stream.of(
                new UserCreateDTO("A", "a@example.com"),
                new UserUpdateDTO("N".repeat(255), "a@example.com"),
                new UserPatchDTO(null, null), new UserPatchDTO(" A\n", "new@example.com"),
                new ProjectCreateDTO("A", null), new ProjectUpdateDTO("N".repeat(255), null),
                new ProjectPatchDTO(null, null), new ProjectPatchDTO("Name", ""),
                new TaskCreateDTO("T", null, null), new TaskCreateDTO("T".repeat(255), "", past),
                new TaskUpdateDTO(1L, "T", null, TaskStatusDTO.TODO, TaskPriorityDTO.MEDIUM, null),
                new TaskUpdateDTO(1L, "T", null, TaskStatusDTO.DONE, TaskPriorityDTO.HIGH, past),
                new TaskPatchDTO(null, null, null, null, null, null),
                new TaskPatchDTO(2L, "T", "", TaskStatusDTO.IN_PROGRESS, TaskPriorityDTO.LOW, past),
                new FocusSessionCreateDTO(null), new FocusSessionCreateDTO(1L),
                new FocusSessionUpdateDTO(null, FocusSessionStatusDTO.COMPLETED),
                new FocusSessionPatchDTO(null, null), new FocusSessionPatchDTO(1L, FocusSessionStatusDTO.PAUSED));
    }

    @ParameterizedTest
    @MethodSource("invalidInputs")
    void rejectsInvalidPresentValues(Object dto, String property) {
        assertTrue(validator.validate(dto).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals(property)),
                () -> "Expected a violation of " + property + " in " + dto);
    }

    static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of(new UserCreateDTO(null, "a@example.com"), "name"),
                Arguments.of(new UserCreateDTO(" ", "a@example.com"), "name"),
                Arguments.of(new UserCreateDTO("N".repeat(256), "a@example.com"), "name"),
                Arguments.of(new UserCreateDTO("A", null), "email"),
                Arguments.of(new UserCreateDTO("A", "bad-email"), "email"),
                Arguments.of(new UserUpdateDTO("A", "a".repeat(255) + "@example.com"), "email"),
                Arguments.of(new UserUpdateDTO(null, "a@example.com"), "name"),
                Arguments.of(new UserPatchDTO("", null), "name"),
                Arguments.of(new UserPatchDTO("\t\n\u2003", null), "name"),
                Arguments.of(new UserPatchDTO(null, ""), "email"),
                Arguments.of(new UserPatchDTO(null, "bad-email"), "email"),
                Arguments.of(new ProjectCreateDTO(null, null), "name"),
                Arguments.of(new ProjectUpdateDTO(" ", null), "name"),
                Arguments.of(new ProjectPatchDTO("\u2003", null), "name"),
                Arguments.of(new ProjectPatchDTO("N".repeat(256), null), "name"),
                Arguments.of(new TaskCreateDTO("", null, null), "title"),
                Arguments.of(new TaskCreateDTO(null, null, null), "title"),
                Arguments.of(new TaskCreateDTO("T".repeat(256), null, null), "title"),
                Arguments.of(new TaskUpdateDTO(null, "T", null, TaskStatusDTO.TODO, TaskPriorityDTO.LOW, null), "projectId"),
                Arguments.of(new TaskUpdateDTO(0L, "T", null, TaskStatusDTO.TODO, TaskPriorityDTO.LOW, null), "projectId"),
                Arguments.of(new TaskUpdateDTO(1L, null, null, TaskStatusDTO.TODO, TaskPriorityDTO.LOW, null), "title"),
                Arguments.of(new TaskUpdateDTO(1L, "T", null, null, TaskPriorityDTO.LOW, null), "status"),
                Arguments.of(new TaskUpdateDTO(1L, "T", null, TaskStatusDTO.TODO, null, null), "priority"),
                Arguments.of(new TaskPatchDTO(-1L, null, null, null, null, null), "projectId"),
                Arguments.of(new TaskPatchDTO(null, " \t\n", null, null, null, null), "title"),
                Arguments.of(new FocusSessionCreateDTO(0L), "taskId"),
                Arguments.of(new FocusSessionUpdateDTO(null, null), "status"),
                Arguments.of(new FocusSessionUpdateDTO(-1L, FocusSessionStatusDTO.RUNNING), "taskId"),
                Arguments.of(new FocusSessionPatchDTO(-1L, null), "taskId"));
    }
}
