package br.edu.ufersa.pw.focustask;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Testcontainers(disabledWithoutDocker = true)
class PostgresDeploymentTests {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("FOCUSTASK_JWT_SECRET", () -> "postgres-test-only-secret-at-least-32-bytes");
        properties.add("DB_URL", POSTGRES::getJdbcUrl);
        properties.add("DB_USERNAME", POSTGRES::getUsername);
        properties.add("DB_PASSWORD", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired tools.jackson.databind.json.JsonMapper json;
    private String bearer;

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder auth(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) {
        return request.header("Authorization", bearer);
    }

    @Test
    void productionMigrationSupportsCrudErrorsAndSessionHistory() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Demo\",\"email\":\"postgres@example.com\",\"password\":\"secret12\"}"))
                .andExpect(status().isCreated());
        String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"postgres@example.com\",\"password\":\"secret12\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        bearer = "Bearer " + json.readTree(login).get("token").asString();
        mvc.perform(auth(get("/api/v1/users"))).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/v1/me"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertTrue(jdbc.queryForObject("select password_hash from users", String.class).startsWith("$2"));
        String user = "/api/v1/me";
        String project = create("/api/v1/projects", "{\"name\":\"Demo project\"}");
        String task = create("/api/v1/projects/" + id(project) + "/tasks", "{\"title\":\"Demo task\"}");
        mvc.perform(auth(patch(task)).contentType(MediaType.APPLICATION_JSON).content("{\"priority\":\"HIGH\"}"))
                .andExpect(status().isOk());
        mvc.perform(auth(get(task))).andExpect(status().isOk()).andExpect(jsonPath("$.priority").value("HIGH"));
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Duplicate\",\"email\":\"POSTGRES@example.com\",\"password\":\"secret12\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(auth(post("/api/v1/projects/" + id(project) + "/tasks")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.title").isArray());
        String session = create(task + "/focus-sessions", null);
        mvc.perform(auth(patch(session)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PAUSED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pausedAt").isNotEmpty());
        mvc.perform(auth(patch(session)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.endedAt").isNotEmpty());
        mvc.perform(auth(patch(session)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RUNNING\"}"))
                .andExpect(status().is(422));
        mvc.perform(auth(delete(task))).andExpect(status().isNoContent());
        mvc.perform(auth(get(task))).andExpect(status().isNotFound());
        mvc.perform(auth(get(session))).andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").isEmpty()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(auth(delete(project))).andExpect(status().isNoContent());
        mvc.perform(auth(delete(user))).andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("select count(*) from focus_sessions", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from users", Integer.class));
    }

    private String create(String path, String body) throws Exception {
        var request = auth(post(path));
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        String location = mvc.perform(request).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber()).andReturn().getResponse().getHeader("Location");
        assertNotNull(location);
        return URI.create(location).getPath();
    }

    private long id(String path) { return Long.parseLong(path.substring(path.lastIndexOf('/') + 1)); }
}
