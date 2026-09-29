package br.edu.ufersa.pw.focustask.features.project;

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
import br.edu.ufersa.pw.focustask.features.project.dto.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
class ProjectControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean ProjectApplicationService service;

    @ParameterizedTest
    @MethodSource("validRequests")
    void validContractsDelegateAndReturnResponses(String method, String url, String body) throws Exception {
        var saved = new ProjectResponseDTO(42L, 7L, "Study", null);
        switch (method) {
            case "POST" -> { when(service.create(eq(7L), any(ProjectCreateDTO.class))).thenReturn(saved); }
            case "PUT" -> when(service.update(eq(7L), eq(42L), any(ProjectUpdateDTO.class))).thenReturn(saved);
            case "PATCH" -> when(service.patch(eq(7L), eq(42L), any(ProjectPatchDTO.class))).thenReturn(saved);
            case "GET" -> {
                when(service.getAll(7L)).thenReturn(List.of(saved));
                when(service.getById(7L, 42L)).thenReturn(saved);
            }
        }
        var request = request(HttpMethod.valueOf(method), url);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        var result = mvc.perform(request).andExpect(status().is(method.equals("POST") ? 201 : method.equals("DELETE") ? 204 : 200));
        if (method.equals("DELETE")) result.andExpect(content().string(""));
        else result.andExpect(jsonPath(method.equals("GET") && (url.equals("/api/v1/users/7/projects")) ? "$[0].id" : "$.id").value(42));
        if (method.equals("POST")) result.andExpect(header().string("Location", "http://localhost/api/v1/users/7/projects/42"));
        switch (method) {
            case "POST" -> { verify(service).create(7L, new ProjectCreateDTO("Study", null)); }
            case "PUT" -> { verify(service).update(7L, 42L, new ProjectUpdateDTO("Study", null)); }
            case "PATCH" -> { verify(service).patch(7L, 42L, new ProjectPatchDTO(null, null)); }
            case "DELETE" -> { verify(service).delete(7L, 42L); }
            case "GET" -> { if (url.equals("/api/v1/users/7/projects")) verify(service).getAll(7L);
                else verify(service).getById(7L, 42L); }
        }
    }

    static Stream<Arguments> validRequests() {
        return Stream.of(
                Arguments.of("POST", "/api/v1/users/7/projects", "{\"name\":\"Study\"}"),
                Arguments.of("PUT", "/api/v1/users/7/projects/42", "{\"name\":\"Study\"}"),
                Arguments.of("PATCH", "/api/v1/users/7/projects/42", "{}"),
                Arguments.of("PATCH", "/api/v1/users/7/projects/42", "{\"name\":null}"),
                Arguments.of("GET", "/api/v1/users/7/projects", null),
                Arguments.of("GET", "/api/v1/users/7/projects/42", null),
                Arguments.of("DELETE", "/api/v1/users/7/projects/42", null));
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
                Arguments.of("POST", "/api/v1/users/7/projects", "{}"),
                Arguments.of("PUT", "/api/v1/users/7/projects/42", "{}"),
                Arguments.of("PATCH", "/api/v1/users/7/projects/42", "{\"name\":\" \"}"));
    }

    @Test
    void unavailableProjectReturnsContextual404() throws Exception {
        when(service.getById(7L, 42L)).thenThrow(new EntidadeNaoEncontradaException("Project not found"));
        mvc.perform(get("/api/v1/users/7/projects/42"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Project not found"));
    }

}
