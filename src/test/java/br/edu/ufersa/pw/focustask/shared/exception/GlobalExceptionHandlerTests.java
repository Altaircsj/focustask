package br.edu.ufersa.pw.focustask.shared.exception;

import br.edu.ufersa.pw.focustask.features.task.dto.TaskUpdateDTO;
import br.edu.ufersa.pw.focustask.features.user.EmailAlreadyExistsException;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GlobalExceptionHandlerTests.FailureController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTests.FailureController.class})
class GlobalExceptionHandlerTests {
    @Autowired MockMvc mvc;
    @Autowired GlobalExceptionHandler handler;
    @Autowired Environment environment;
    @MockitoBean FailureSource source;

    @ParameterizedTest
    @MethodSource("failures")
    void translatesFailuresWithoutExposingTechnicalDetails(Exception failure, int code, String title, String detail)
            throws Exception {
        when(source.failure()).thenReturn(failure);
        mvc.perform(get("/focustask/test-errors/1").contextPath("/focustask").queryParam("trace", "true"))
                .andExpect(status().is(code))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value(code))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value("/focustask/test-errors/1"))
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(content().string(not(containsString("TECHNICAL_SECRET"))))
                .andExpect(content().string(not(containsString("org.hibernate"))));
    }

    static Stream<Arguments> failures() {
        String internal = "An unexpected error occurred. Please try again later.";
        String secret = "TECHNICAL_SECRET SQL insert into users constraint uk_users_email org.hibernate.SomeException";
        return Stream.of(
                Arguments.of(new EntidadeNaoEncontradaException("User not found"), 404, "Not Found", "User not found"),
                Arguments.of(new EntidadeNaoEncontradaException("Project not found"), 404, "Not Found", "Project not found"),
                Arguments.of(new EntidadeNaoEncontradaException("Task not found"), 404, "Not Found", "Task not found"),
                Arguments.of(new EntidadeNaoEncontradaException("Focus session not found"), 404, "Not Found", "Focus session not found"),
                Arguments.of(new EmailAlreadyExistsException(), 409, "Conflict", "Email already registered"),
                Arguments.of(new DataIntegrityViolationException(secret, new IllegalStateException(secret)),
                        409, "Conflict", "The operation conflicts with existing data."),
                Arguments.of(new OperacaoInvalidaException("A completed session cannot be paused"),
                        422, "Unprocessable Content", "A completed session cannot be paused"),
                Arguments.of(new OperacaoInvalidaException("A completed session cannot be resumed"),
                        422, "Unprocessable Content", "A completed session cannot be resumed"),
                Arguments.of(new NegocioException("Business rule rejected") {}, 400, "Bad Request", "Business rule rejected"),
                Arguments.of(new IllegalStateException(secret), 500, "Internal Server Error", internal),
                Arguments.of(new IllegalArgumentException(secret), 500, "Internal Server Error", internal),
                Arguments.of(new HttpMessageNotWritableException(secret), 500, "Internal Server Error", internal),
                Arguments.of(new ConversionNotSupportedException(secret, Long.class, new IllegalStateException(secret)),
                        500, "Internal Server Error", internal));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{", "{\"status\":\"TECHNICAL_SECRET\"}", "{\"dueDate\":\"not-a-date\"}"})
    void unreadableBodiesReturnSafe400(String body) throws Exception {
        mvc.perform(post("/test-errors/body").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request body is missing or malformed, or contains invalid values."))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(content().string(not(containsString("TECHNICAL_SECRET"))));
        verifyNoInteractions(source);
    }

    @Test
    void invalidPathParameterDoesNotExposeValueOrJavaType() throws Exception {
        mvc.perform(get("/test-errors/TECHNICAL_SECRET"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A request parameter has an invalid value."))
                .andExpect(jsonPath("$.title").value("Bad Request"));
        verifyNoInteractions(source);
    }

    @Test
    void mvcErrorsKeepTheirStatusAndHeaders() throws Exception {
        mvc.perform(put("/test-errors/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405));
        mvc.perform(post("/test-errors/body").contentType(MediaType.TEXT_PLAIN).content("text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(415));
        mvc.perform(get("/no-such-resource"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        verifyNoInteractions(source);
    }

    @Test
    void alreadyCommittedResponseIsNotReplaced() throws Exception {
        var response = new MockHttpServletResponse();
        response.setCommitted(true);
        assertNull(handler.handleUnexpected(new IllegalStateException("test failure"),
                new ServletWebRequest(new MockHttpServletRequest(), response)));
        assertEquals("", response.getContentAsString());
    }

    @Test
    void bootFallbackExcludesInternalErrorAttributes() {
        for (String property : new String[]{"include-stacktrace", "include-message", "include-binding-errors"}) {
            assertEquals("never", environment.getProperty("spring.web.error." + property));
        }
        assertEquals("false", environment.getProperty("spring.web.error.include-exception"));
    }

    interface FailureSource { Exception failure(); }

    @TestComponent
    @RestController
    @RequestMapping("/test-errors")
    static class FailureController {
        private final FailureSource source;
        FailureController(FailureSource source) { this.source = source; }

        @GetMapping("/{id}")
        public void fail(@PathVariable Long id) throws Exception { throw source.failure(); }

        @PostMapping("/body")
        public void body(@RequestBody @Valid TaskUpdateDTO dto) { }
    }
}
