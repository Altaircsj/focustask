package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.features.project.Project;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class TaskTests {

    @Test
    void rejectsMissingProjectAndInvalidTitles() {
        Project project = mock(Project.class);

        assertThrows(NullPointerException.class, () -> new Task(null, "Study"));
        assertThrows(IllegalArgumentException.class, () -> new Task(project, null));
        assertThrows(IllegalArgumentException.class, () -> new Task(project, "  "));
        assertThrows(IllegalArgumentException.class, () -> new Task(project, "a".repeat(256)));
    }

    @Test
    void invalidUpdatesPreserveThePreviousValues() {
        Task task = new Task(mock(Project.class), "Study");
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setPriority(TaskPriority.HIGH);

        assertThrows(IllegalArgumentException.class, () -> task.setTitle(""));
        assertThrows(NullPointerException.class, () -> task.setStatus(null));
        assertThrows(NullPointerException.class, () -> task.setPriority(null));

        assertEquals("Study", task.getTitle());
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertEquals(TaskPriority.HIGH, task.getPriority());
    }
}
