package br.edu.ufersa.pw.focustask;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.edu.ufersa.pw.focustask.features.user.UserInternalApi;
import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import org.springframework.transaction.IllegalTransactionStateException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FocustaskApplicationTests extends MySqlIntegrationTest {
    @Autowired UserInternalApi users;
    @Autowired ProjectInternalApi projects;
    @Autowired TaskInternalApi tasks;
    @Autowired FocusSessionInternalApi sessions;

    @Test
    void contextLoadsWithWorkingFacadesAndNoDependencyCycles() {
        long user = user("student@example.com");
        long project = project(user);
        long task = task(project);
        assertTrue(users.existePorId(user));
        assertTrue(projects.pertenceAoUsuario(user, project));
        assertTrue(tasks.pertenceAoUsuario(user, task));
        assertEquals(List.of(task), tasks.listarIdsPorProjetos(user, List.of(project)));
    }

    @Test
    void bulkFacadesRequireTheCoordinatorsTransaction() {
        assertThrows(IllegalTransactionStateException.class,
                () -> sessions.desvincularDeTarefas(1L, List.of(1L)));
        assertThrows(IllegalTransactionStateException.class, () -> projects.excluirPorUsuario(1L));
        assertThrows(IllegalTransactionStateException.class,
                () -> tasks.excluirPorProjetos(1L, List.of(1L)));
    }
}
