package br.edu.ufersa.pw.focustask;

import br.edu.ufersa.pw.focustask.shared.exception.GlobalExceptionHandler;
import br.edu.ufersa.pw.focustask.features.user.UserController;
import br.edu.ufersa.pw.focustask.features.project.ProjectController;
import br.edu.ufersa.pw.focustask.features.task.TaskController;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CrudMySqlTests extends MySqlIntegrationTest {
    @Autowired UserController users;
    @Autowired ProjectController projects;
    @Autowired TaskController tasks;
    @Autowired FocusSessionController sessions;
    @Autowired GlobalExceptionHandler handler;
    private MockMvc mvc;
    @Autowired org.springframework.web.context.WebApplicationContext context;
    @Autowired tools.jackson.databind.json.JsonMapper json;
    private void authenticate(long userId) {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(get("/").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(
                        AuthenticatedMvcTest.principal(userId)))).build();
    }

    @BeforeEach
    void configureMvc() { mvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build(); }

    @Test
    void crudExecutesAgainstRealApplicationServicesAndMysql() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Carol\",\"email\":\"carol@example.com\",\"password\":\"secret12\"}"))
                .andExpect(status().isCreated()).andExpect(content().string(""));
        jdbc.update("update users set role='ADMIN' where email=?", "carol@example.com");
        String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"carol@example.com\",\"password\":\"secret12\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(login).get("token").asString();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(get("/").header("Authorization", "Bearer " + token)).build();
        String user = "/api/v1/me";
        mvc.perform(get("/api/v1/users")).andExpect(jsonPath("$[0].name").value("Carol"));
        mvc.perform(get(user)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Carol"));
        mvc.perform(put(user).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Changed\",\"email\":\"CAROL@example.com\"}")).andExpect(status().isOk());
        mvc.perform(patch(user).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Patched\"}"))
                .andExpect(status().isOk());
        mvc.perform(get(user)).andExpect(jsonPath("$.name").value("Patched"));

        String project = create("/api/v1/projects", "{\"name\":\"Project\",\"description\":\"Keep\"}");
        mvc.perform(get("/api/v1/projects")).andExpect(jsonPath("$[0].name").value("Project"));
        mvc.perform(get(project)).andExpect(status().isOk());
        mvc.perform(patch(project).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.description").value("Keep"));
        mvc.perform(put(project).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Updated\"}"))
                .andExpect(status().isOk());
        mvc.perform(get(project)).andExpect(jsonPath("$.description").isEmpty());

        long projectId = id(project);
        String task = create("/api/v1/projects/" + projectId + "/tasks", "{\"title\":\"Task\"}");
        mvc.perform(get("/api/v1/tasks")).andExpect(jsonPath("$[0].id").value(id(task)));
        mvc.perform(get(project + "/tasks")).andExpect(jsonPath("$[0].id").value(id(task)));
        mvc.perform(get(task)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("TODO"));
        mvc.perform(put(task).contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\":" + projectId + ",\"title\":\"Edited\",\"status\":\"DONE\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch(task).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"TODO\"}"))
                .andExpect(status().isOk());
        mvc.perform(get(task)).andExpect(jsonPath("$.status").value("TODO")).andExpect(jsonPath("$.title").value("Edited"));

        String standalone = create("/api/v1/focus-sessions", "{}");
        mvc.perform(get("/api/v1/focus-sessions")).andExpect(jsonPath("$[0].id").value(id(standalone)));
        mvc.perform(get(standalone)).andExpect(jsonPath("$.taskId").isEmpty());
        mvc.perform(put(standalone).contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskId\":" + id(task) + ",\"status\":\"PAUSED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pausedAt").isNotEmpty());
        mvc.perform(patch(standalone).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RUNNING\"}"))
                .andExpect(status().isOk());
        mvc.perform(get(standalone)).andExpect(jsonPath("$.taskId").value(id(task))).andExpect(jsonPath("$.pausedAt").isEmpty());
        String linked = create(task + "/focus-sessions", null);
        mvc.perform(get(task + "/focus-sessions")).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(delete(standalone)).andExpect(status().isNoContent());
        mvc.perform(delete(task)).andExpect(status().isNoContent());
        mvc.perform(get(linked)).andExpect(jsonPath("$.taskId").isEmpty());
        mvc.perform(delete(project)).andExpect(status().isNoContent());
        mvc.perform(delete(user)).andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("select count(*) from users", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from focus_sessions", Integer.class));
    }

    @Test
    void rejectsForeignContextsWith404AndPreservesStoredData() throws Exception {
        long owner = user("owner@example.com");
        long stranger = user("stranger@example.com");
        long project = project(owner);
        long task = task(project);
        long session = session(owner, task);
        authenticate(stranger);
        mvc.perform(get("/api/v1/projects/" + project))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Project not found"));
        mvc.perform(delete("/api/v1/tasks/" + task))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Task not found"));
        mvc.perform(patch("/api/v1/focus-sessions/" + session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Focus session not found"));
        assertEquals(task, jdbc.queryForObject("select task_id from focus_sessions where id=?", Long.class, session));
        assertEquals("RUNNING", jdbc.queryForObject("select status from focus_sessions where id=?", String.class, session));
    }

    private String create(String path, String body) throws Exception {
        var request = post(path);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        String location = mvc.perform(request).andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getHeader("Location");
        assertNotNull(location);
        return URI.create(location).getPath();
    }

    private long id(String path) { return Long.parseLong(path.substring(path.lastIndexOf('/') + 1)); }

    @Test
    void duplicateEmailReturns409WithoutInsertingOrChangingUser() throws Exception {
        long owner = user("owner@example.com");
        long other = user("other@example.com");
        authenticate(other);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Duplicate\",\"email\":\"OWNER@example.com\",\"password\":\"secret12\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Email already registered"));
        mvc.perform(patch("/api/v1/me").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"OWNER@example.com\"}"))
                .andExpect(status().isConflict());
        assertEquals(2, jdbc.queryForObject("select count(*) from users", Integer.class));
        assertEquals("other@example.com", jdbc.queryForObject("select email from users where id=?", String.class, other));
        assertEquals("owner@example.com", jdbc.queryForObject("select email from users where id=?", String.class, owner));
    }

    @Test
    void completedSessionRejectsBothTransitionsWith422WithoutChangingStateOrLink() throws Exception {
        long owner = user("owner@example.com");
        long project = project(owner);
        long originalTask = task(project);
        long otherTask = task(project);
        long session = session(owner, originalTask);
        authenticate(owner);
        String path = "/api/v1/focus-sessions/" + session;
        mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());
        var before = jdbc.queryForMap("select * from focus_sessions where id=?", session);
        for (String next : new String[]{"PAUSED", "RUNNING"}) {
            String operation = next.equals("PAUSED") ? "paused" : "resumed";
            mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"taskId\":" + otherTask + ",\"status\":\"" + next + "\"}"))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.detail").value("A completed session cannot be " + operation));
            assertEquals(before, jdbc.queryForMap("select * from focus_sessions where id=?", session));
        }
    }

}
