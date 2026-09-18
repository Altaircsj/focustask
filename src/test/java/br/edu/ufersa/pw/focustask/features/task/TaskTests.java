package br.edu.ufersa.pw.focustask.features.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskTests {

    @Test
    void rejectsMissingProjectAndInvalidTitles() {
        Long project = 1L;

        assertThrows(NullPointerException.class, () -> new Task(null, "Study"));
        assertThrows(IllegalArgumentException.class, () -> new Task(project, null));
        assertThrows(IllegalArgumentException.class, () -> new Task(project, "  "));
        assertThrows(IllegalArgumentException.class, () -> new Task(project, "a".repeat(256)));
    }

    @Test
    void invalidUpdatesPreserveThePreviousValues() {
        Task task = new Task(1L, "Study");
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
