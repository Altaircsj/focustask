package br.edu.ufersa.pw.focustask;

import br.edu.ufersa.pw.focustask.features.user.UserController;
import br.edu.ufersa.pw.focustask.features.project.ProjectController;
import br.edu.ufersa.pw.focustask.features.task.TaskController;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest({UserController.class, ProjectController.class, TaskController.class, FocusSessionController.class})
class ControllerMappingsTests {
    @Autowired RequestMappingHandlerMapping mappings;
    @MockitoBean TaskController taskController;
    @MockitoBean UserController userController;
    @MockitoBean ProjectController projectController;
    @MockitoBean FocusSessionController focusSessionController;

    @Test
    void preservesAll27DistinctRoutesIncludingTheThreeRestoredOnes() {
        long featureMappings = mappings.getHandlerMethods().values().stream()
                .filter(method -> method.getBeanType().getPackageName().contains(".features.")).count();
        assertEquals(27, featureMappings);
        assertRoute(RequestMethod.POST, "/api/v1/projects/{projectId}/tasks");
        assertFalse(mappings.getHandlerMethods().keySet().stream()
                .anyMatch(mapping -> mapping.getPatternValues().contains("/api/v1/users/{userId}/tasks")
                        && mapping.getMethodsCondition().getMethods().contains(RequestMethod.POST)));
        assertRoute(RequestMethod.GET, "/api/v1/users");
        assertRoute(RequestMethod.PATCH, "/api/v1/users/{userId}");
        assertRoute(RequestMethod.PATCH, "/api/v1/users/{userId}/projects/{projectId}");
        assertRoute(RequestMethod.GET, "/api/v1/users/{userId}/projects/{projectId}/tasks");
        assertRoute(RequestMethod.POST, "/api/v1/users/{userId}/tasks/{taskId}/focus-sessions");
    }

    private void assertRoute(RequestMethod method, String path) {
        assertEquals(1, mappings.getHandlerMethods().keySet().stream()
                .filter(mapping -> mapping.getPatternValues().contains(path)
                        && mapping.getMethodsCondition().getMethods().contains(method)).count());
    }
}
