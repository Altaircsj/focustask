package br.edu.ufersa.pw.focustask;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import java.sql.Statement;
import org.springframework.jdbc.support.GeneratedKeyHolder;

@org.springframework.test.context.ActiveProfiles("dev")
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class MySqlIntegrationTest {
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("delete from focus_sessions");
        jdbc.update("delete from tasks");
        jdbc.update("delete from projects");
        jdbc.update("delete from users");
    }

    /** MySQL CHECK violations use vendor error 3819 / SQL state HY000.
     * Spring may expose them as UncategorizedSQLException rather than integrity exceptions. */
    protected void assertCheckConstraintViolation(String constraint, Runnable operation) {
        var failure = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataAccessException.class, operation::run);
        Throwable cause = failure.getMostSpecificCause();
        var sql = org.junit.jupiter.api.Assertions.assertInstanceOf(java.sql.SQLException.class, cause);
        org.junit.jupiter.api.Assertions.assertEquals(3819, sql.getErrorCode());
        org.junit.jupiter.api.Assertions.assertTrue(sql.getMessage().contains(constraint), sql.getMessage());
    }

    protected long user(String email) {
        return insert("insert into users(name,email) values (?,?)", "Student", email);
    }

    protected long project(long userId) {
        return insert("insert into projects(user_id,name) values (?,?)", userId, "Project");
    }

    protected long task(long projectId) {
        return insert("insert into tasks(project_id,title) values (?,?)", projectId, "Task");
    }

    protected long session(long userId, Long taskId) {
        return insert("insert into focus_sessions(user_id,task_id,started_at) values (?,?,?)",
                userId, taskId, java.sql.Timestamp.valueOf("2026-09-17 12:00:00"));
    }

    private long insert(String sql, Object... args) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
            return statement;
        }, key);
        return java.util.Objects.requireNonNull(key.getKey()).longValue();
    }
}
