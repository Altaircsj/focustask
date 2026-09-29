package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.features.user.dto.*;
import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import br.edu.ufersa.pw.focustask.shared.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
import java.time.Clock;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class, UserController.class})
@Import({SecurityProblemHandler.class, SecurityConfig.class, TokenService.class, UserDetailsServiceImpl.class, AuthApplicationService.class,
        UserService.class, UserApplicationService.class, UserMapperImpl.class, AuthSecurityTests.TimeConfig.class})
@TestPropertySource(properties = "api.security.token.secret=auth-test-only-secret-with-at-least-32-bytes")
class AuthSecurityTests {
    @TestConfiguration static class TimeConfig { @Bean Clock clock() { return Clock.systemUTC(); } }
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @Autowired TokenService tokens;
    @Autowired UserDetailsServiceImpl details;
    @Autowired JsonMapper json;
    @MockitoBean UserRepository repository;
    @MockitoBean ProjectInternalApi projects;
    @MockitoBean TaskInternalApi tasks;
    @MockitoBean FocusSessionInternalApi sessions;
    private final Map<Long, User> stored = new LinkedHashMap<>();
    private User owner;

    @BeforeEach void users() {
        stored.clear();
        owner = account(1L, "owner@example.com");
        account(2L, "other@example.com");
        when(repository.findByEmail(anyString())).thenAnswer(c -> stored.values().stream()
                .filter(u -> u.getEmail().equals(c.getArgument(0))).findFirst());
        when(repository.findById(anyLong())).thenAnswer(c -> Optional.ofNullable(stored.get(c.getArgument(0))));
        when(repository.findAll()).thenAnswer(c -> new ArrayList<>(stored.values()));
        when(repository.existsByEmail(anyString())).thenAnswer(c -> stored.values().stream()
                .anyMatch(u -> u.getEmail().equals(c.getArgument(0))));
        when(repository.existsByEmailAndIdNot(anyString(), anyLong())).thenAnswer(c -> stored.values().stream()
                .anyMatch(u -> u.getEmail().equals(c.getArgument(0)) && !u.getId().equals(c.getArgument(1))));
        when(repository.save(any(User.class))).thenAnswer(c -> {
            User user = c.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 3L);
            stored.put(3L, user);
            return user;
        });
        doAnswer(c -> { stored.remove(c.getArgument(0)); return null; }).when(repository).deleteById(anyLong());
    }
    private User account(long id, String email) {
        User user = new User("Student", email, encoder.encode("secret12"));
        ReflectionTestUtils.setField(user, "id", id);
        stored.put(id, user);
        return user;
    }
    private String token() { return tokens.generate((AuthenticatedUser) details.loadUserByUsername(owner.getEmail())); }
    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", "secret12"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asString();
    }

    @Test void registerHashesExactlyOnceAndAlwaysCreatesUserWithoutReturningCredentials() throws Exception {
        mvc.perform(post("/api/v1/auth/register").header("Authorization", "Bearer stale")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"email\":\"NEW@example.com\",\"password\":\"secret12\"}"))
                .andExpect(status().isCreated()).andExpect(content().string(""))
                .andExpect(header().doesNotExist("Location"));
        User created = stored.get(3L);
        assertEquals("new@example.com", created.getEmail());
        assertEquals(UserRole.USER, created.getRole());
        assertNotEquals("secret12", created.getPasswordHash());
        assertTrue(encoder.matches("secret12", created.getPasswordHash()));
        verify(repository, times(1)).save(any());
    }

    @Test void publicRoleAndUnknownAuthenticationFieldsAreRejected() throws Exception {
        for (String field : List.of("role", "passwordHash", "id")) {
            mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New\",\"email\":\"new@example.com\",\"password\":\"secret12\",\"" + field + "\":\"ADMIN\"}"))
                    .andExpect(status().isBadRequest());
        }
        verify(repository, never()).save(any());
    }

    @Test void duplicateRegistrationIs409AndInvalidInputDoesNotPersist() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Duplicate\",\"email\":\"OWNER@example.com\",\"password\":\"secret12\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Email already registered"));
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.password").isArray());
        verify(repository, never()).save(any());
    }

    @Test void loginAuthenticatesAndSessionsDoNotReplaceBearer() throws Exception {
        String token = login("OWNER@example.com");
        var response = mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist()).andReturn();
        assertNull(response.getRequest().getSession(false));
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    @Test void unknownEmailAndWrongPasswordHaveTheSameSafe401() throws Exception {
        String expected = null;
        for (String email : List.of("owner@example.com", "absent@example.com")) {
            String response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", email, "password", "wrong-password"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(header().string("WWW-Authenticate", "Bearer"))
                    .andExpect(jsonPath("$.status").value(401)).andExpect(jsonPath("$.trace").doesNotExist())
                    .andReturn().getResponse().getContentAsString();
            assertFalse(response.contains(email));
            assertFalse(response.contains("wrong-password"));
            if (expected != null) assertEquals(expected, response);
            expected = response;
        }
    }

    @ParameterizedTest @ValueSource(strings = {"Bearer malformed", "Basic abc", "Bearer ", "Bearer a b", "Bearer a,b"})
    void invalidHeadersReturnSafe401(String header) throws Exception {
        mvc.perform(get("/focustask/api/v1/me").contextPath("/focustask").queryParam("trace", "true")
                        .header("Authorization", header))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.instance").value("/focustask/api/v1/me"))
                .andExpect(jsonPath("$.type").value("about:blank"));
    }

    @Test void rolesAreReadFromDatabaseAndUserCannotListAccounts() throws Exception {
        String token = token();
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Access is denied."));
        ReflectionTestUtils.setField(owner, "role", UserRole.ADMIN);
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        ReflectionTestUtils.setField(owner, "role", UserRole.USER);
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test void changingEmailPreservesCredentialsAndInvalidatesOldSubjectEvenWhenReassigned() throws Exception {
        String token = token();
        String hash = owner.getPasswordHash();
        mvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"new@example.com\"}"))
                .andExpect(status().isOk());
        assertEquals(hash, owner.getPasswordHash());
        assertEquals(UserRole.USER, owner.getRole());
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        account(4L, "owner@example.com");
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        String newToken = login("new@example.com");
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + newToken)).andExpect(status().isOk());
    }

    @Test void profileCannotChangeRoleHashOrIdAndDeletionInvalidatesToken() throws Exception {
        String token = token();
        String hash = owner.getPasswordHash();
        // Unknown profile fields cannot be mapped onto protected entity fields.
        var result = mvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Changed\",\"role\":\"ADMIN\",\"passwordHash\":\"attacker\",\"id\":2}"))
                .andReturn();
        assertTrue(result.getResponse().getStatus() == 200 || result.getResponse().getStatus() == 400);
        assertEquals(hash, owner.getPasswordHash());
        assertEquals(UserRole.USER, owner.getRole());
        assertEquals(1L, owner.getId());
        mvc.perform(delete("/api/v1/me").header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        verify(sessions).excluirPorUsuario(1L);
        verify(tasks).excluirPorProjetos(1L, List.of());
        verify(projects).excluirPorUsuario(1L);
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test void technicalAuthenticationFailuresAre500WithoutInternalMessages() throws Exception {
        String token = token();
        when(repository.findByEmail(anyString())).thenThrow(new DataAccessResourceFailureException("private SQL password_hash"));
        for (var request : List.of(get("/api/v1/me").header("Authorization", "Bearer " + token),
                post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"owner@example.com\",\"password\":\"secret12\"}"))) {
            mvc.perform(request).andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.detail").value("An unexpected error occurred. Please try again later."))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private SQL"))));
        }
    }

    @Test void oldCreationAndIndividualUserRoutesHaveNoAliases() throws Exception {
        String token = token();
        mvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + token)).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/v1/users/2").header("Authorization", "Bearer " + token)).andExpect(status().isNotFound());
    }
    @Test void expiredAndTamperedTokensAre401BeforeProfileExecution() throws Exception {
        String expired = com.auth0.jwt.JWT.create().withIssuer("focustask-api").withSubject(owner.getEmail())
                .withClaim("userId", owner.getId()).withIssuedAt(java.time.Instant.now().minusSeconds(1000))
                .withExpiresAt(java.time.Instant.now().minusSeconds(10))
                .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256("auth-test-only-secret-with-at-least-32-bytes"));
        String token = token();
        String[] pieces = token.split("\\.");
        String tampered = pieces[0] + "." + pieces[1] + "." + (pieces[2].startsWith("A") ? "B" : "A") + pieces[2].substring(1);
        for (String invalid : List.of(expired, tampered)) {
            mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + invalid))
                    .andExpect(status().isUnauthorized());
        }
        verify(repository, never()).findById(anyLong());
    }

}
