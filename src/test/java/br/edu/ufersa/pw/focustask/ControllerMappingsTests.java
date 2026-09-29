package br.edu.ufersa.pw.focustask;

import br.edu.ufersa.pw.focustask.features.user.UserController;
import br.edu.ufersa.pw.focustask.features.user.AuthController;
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

@WebMvcTest({AuthController.class, UserController.class, ProjectController.class, TaskController.class, FocusSessionController.class})
class ControllerMappingsTests {
    @Autowired RequestMappingHandlerMapping mappings;
    @MockitoBean AuthController authController;
    @MockitoBean TaskController taskController;
    @MockitoBean UserController userController;
    @MockitoBean ProjectController projectController;
    @MockitoBean FocusSessionController focusSessionController;

    @Test
    void preservesAll28DistinctRoutesIncludingTheThreeRestoredOnes() {
        long featureMappings = mappings.getHandlerMethods().values().stream()
                .filter(method -> method.getBeanType().getPackageName().contains(".features.")).count();
        assertEquals(28, featureMappings);
        assertRoute(RequestMethod.POST, "/api/v1/projects/{projectId}/tasks");
        assertFalse(mappings.getHandlerMethods().keySet().stream()
                .anyMatch(mapping -> mapping.getPatternValues().contains("/api/v1/tasks")
                        && mapping.getMethodsCondition().getMethods().contains(RequestMethod.POST)));
        assertRoute(RequestMethod.GET, "/api/v1/users");
        assertRoute(RequestMethod.POST, "/api/v1/auth/register");
        assertRoute(RequestMethod.POST, "/api/v1/auth/login");
        assertFalse(mappings.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream()).anyMatch(path -> path.contains("{userId}")));
        assertRoute(RequestMethod.PATCH, "/api/v1/me");
        assertRoute(RequestMethod.PATCH, "/api/v1/projects/{projectId}");
        assertRoute(RequestMethod.GET, "/api/v1/projects/{projectId}/tasks");
        assertRoute(RequestMethod.POST, "/api/v1/tasks/{taskId}/focus-sessions");
    }

    private void assertRoute(RequestMethod method, String path) {
        assertEquals(1, mappings.getHandlerMethods().keySet().stream()
                .filter(mapping -> mapping.getPatternValues().contains(path)
                        && mapping.getMethodsCondition().getMethods().contains(method)).count());
    }
}
