package br.edu.ufersa.pw.focustask.features.focusSession;

import org.junit.jupiter.api.Test;
import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;
import java.util.List;
import br.edu.ufersa.pw.focustask.features.focusSession.dto.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FocusSessionController.class)
class FocusSessionControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean FocusSessionApplicationService service;

    @ParameterizedTest
    @MethodSource("validRequests")
    void validContractsDelegateAndReturnResponses(String method, String url, String body) throws Exception {
        var saved = new FocusSessionResponseDTO(8L, 7L, null, FocusSessionStatusDTO.RUNNING, java.time.Instant.EPOCH, null, null, 0);
        switch (method) {
            case "POST" -> { when(service.create(eq(7L), any(FocusSessionCreateDTO.class))).thenReturn(saved);
                when(service.createForTask(7L, 123L)).thenReturn(saved); }
            case "PUT" -> when(service.update(eq(7L), eq(8L), any(FocusSessionUpdateDTO.class))).thenReturn(saved);
            case "PATCH" -> when(service.patch(eq(7L), eq(8L), any(FocusSessionPatchDTO.class))).thenReturn(saved);
            case "GET" -> {
                when(service.getAll(7L)).thenReturn(List.of(saved));
                when(service.getById(7L, 8L)).thenReturn(saved);
                if (url.contains("/tasks/")) when(service.getByTask(7L, 123L)).thenReturn(List.of(saved));

            }
        }
        var request = request(HttpMethod.valueOf(method), url);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        var result = mvc.perform(request).andExpect(status().is(method.equals("POST") ? 201 : method.equals("DELETE") ? 204 : 200));
        if (method.equals("DELETE")) result.andExpect(content().string(""));
        else result.andExpect(jsonPath(method.equals("GET") && (url.equals("/api/v1/users/7/focus-sessions") || url.contains("/tasks/")) ? "$[0].id" : "$.id").value(8));
        if (method.equals("POST")) result.andExpect(header().string("Location", "http://localhost/api/v1/users/7/focus-sessions/8"));
        switch (method) {
            case "POST" -> { if (url.contains("/tasks/")) verify(service).createForTask(7L, 123L);
                else verify(service).create(7L, new FocusSessionCreateDTO(null)); }
            case "PUT" -> { verify(service).update(7L, 8L, new FocusSessionUpdateDTO(null, FocusSessionStatusDTO.RUNNING)); }
            case "PATCH" -> { verify(service).patch(7L, 8L, new FocusSessionPatchDTO(null, null)); }
            case "DELETE" -> { verify(service).delete(7L, 8L); }
            case "GET" -> { if (url.contains("/tasks/")) verify(service).getByTask(7L, 123L);
                else if (url.equals("/api/v1/users/7/focus-sessions")) verify(service).getAll(7L);
                else verify(service).getById(7L, 8L); }
        }
    }

    static Stream<Arguments> validRequests() {
        return Stream.of(
                Arguments.of("POST", "/api/v1/users/7/focus-sessions", "{}"),
                Arguments.of("PUT", "/api/v1/users/7/focus-sessions/8", "{\"status\":\"RUNNING\",\"taskId\":null}"),
                Arguments.of("PATCH", "/api/v1/users/7/focus-sessions/8", "{}"),
                Arguments.of("PATCH", "/api/v1/users/7/focus-sessions/8", "{\"taskId\":null,\"status\":null}"),
                Arguments.of("GET", "/api/v1/users/7/focus-sessions", null),
                Arguments.of("GET", "/api/v1/users/7/focus-sessions/8", null),
                Arguments.of("DELETE", "/api/v1/users/7/focus-sessions/8", null),
                Arguments.of("POST", "/api/v1/users/7/tasks/123/focus-sessions", null),
                Arguments.of("GET", "/api/v1/users/7/tasks/123/focus-sessions", null));
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void invalidBodiesAreRejectedBeforeExecution(String method, String url, String body) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), url)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isMap());
        verifyNoInteractions(service);
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("POST", "/api/v1/users/7/focus-sessions", "{\"taskId\":-1}"),
                Arguments.of("PUT", "/api/v1/users/7/focus-sessions/8", "{}"),
                Arguments.of("PATCH", "/api/v1/users/7/focus-sessions/8", "{\"taskId\":0}"));
    }

    @Test
    void missingSessionReturns404() throws Exception {
        when(service.getById(7L, 8L)).thenThrow(new EntidadeNaoEncontradaException("Focus session not found"));
        mvc.perform(get("/api/v1/users/7/focus-sessions/8"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Focus session not found"));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"PAUSED", "RUNNING"})
    void rejectedTransitionReturns422(String next) throws Exception {
        String detail = "A completed session cannot be " + (next.equals("PAUSED") ? "paused" : "resumed");
        when(service.patch(eq(7L), eq(8L), any(FocusSessionPatchDTO.class)))
                .thenThrow(new br.edu.ufersa.pw.focustask.shared.exception.OperacaoInvalidaException(detail));
        mvc.perform(patch("/api/v1/users/7/focus-sessions/8").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + next + "\"}"))
                .andExpect(status().is(422)).andExpect(jsonPath("$.detail").value(detail));
    }

    @Test
    void invalidStatusIsA400BeforeExecution() throws Exception {
        mvc.perform(patch("/api/v1/users/7/focus-sessions/8").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOT_A_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request body is missing or malformed, or contains invalid values."));
        verifyNoInteractions(service);
    }

}
