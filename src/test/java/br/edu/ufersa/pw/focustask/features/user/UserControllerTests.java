package br.edu.ufersa.pw.focustask.features.user;

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
import br.edu.ufersa.pw.focustask.features.user.dto.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean UserApplicationService service;

    @ParameterizedTest
    @MethodSource("validRequests")
    void validContractsDelegateAndReturnResponses(String method, String url, String body) throws Exception {
        var saved = new UserResponseDTO(7L, "Carol", "carol@example.com");
        switch (method) {
            case "POST" -> { when(service.create(any(UserCreateDTO.class))).thenReturn(saved); }
            case "PUT" -> when(service.update(eq(7L), any(UserUpdateDTO.class))).thenReturn(saved);
            case "PATCH" -> when(service.patch(eq(7L), any(UserPatchDTO.class))).thenReturn(saved);
            case "GET" -> {
                when(service.getAll()).thenReturn(List.of(saved));
                when(service.getById(7L)).thenReturn(saved);
            }
        }
        var request = request(HttpMethod.valueOf(method), url);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        var result = mvc.perform(request).andExpect(status().is(method.equals("POST") ? 201 : method.equals("DELETE") ? 204 : 200));
        if (method.equals("DELETE")) result.andExpect(content().string(""));
        else result.andExpect(jsonPath(method.equals("GET") && (url.equals("/api/v1/users")) ? "$[0].id" : "$.id").value(7));
        if (method.equals("POST")) result.andExpect(header().string("Location", "http://localhost/api/v1/users/7"));
        switch (method) {
            case "POST" -> { verify(service).create(new UserCreateDTO("Carol", "carol@example.com")); }
            case "PUT" -> { verify(service).update(7L, new UserUpdateDTO("Carol", "carol@example.com")); }
            case "PATCH" -> { verify(service).patch(7L, new UserPatchDTO(null, null)); }
            case "DELETE" -> { verify(service).delete(7L); }
            case "GET" -> { if (url.equals("/api/v1/users")) verify(service).getAll();
                else verify(service).getById(7L); }
        }
    }

    static Stream<Arguments> validRequests() {
        return Stream.of(
                Arguments.of("POST", "/api/v1/users", "{\"name\":\"Carol\",\"email\":\"carol@example.com\"}"),
                Arguments.of("PUT", "/api/v1/users/7", "{\"name\":\"Carol\",\"email\":\"carol@example.com\"}"),
                Arguments.of("PATCH", "/api/v1/users/7", "{}"),
                Arguments.of("PATCH", "/api/v1/users/7", "{\"name\":null}"),
                Arguments.of("GET", "/api/v1/users", null),
                Arguments.of("GET", "/api/v1/users/7", null),
                Arguments.of("DELETE", "/api/v1/users/7", null));
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
                Arguments.of("POST", "/api/v1/users", "{}"),
                Arguments.of("PUT", "/api/v1/users/7", "{}"),
                Arguments.of("PATCH", "/api/v1/users/7", "{\"email\":\"invalid\"}"));
    }

    @Test
    void missingUserReturns404() throws Exception {
        when(service.getById(99L)).thenThrow(new EntidadeNaoEncontradaException("User not found"));
        mvc.perform(get("/api/v1/users/99"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("User not found"));
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        when(service.create(any(UserCreateDTO.class))).thenThrow(new EmailAlreadyExistsException());
        mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Carol\",\"email\":\"carol@example.com\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Email already registered"));
    }

    @Test
    void validationKeepsMultipleMessagesPerFieldWithoutRejectedValues() throws Exception {
        mvc.perform(patch("/api/v1/users/7").locale(java.util.Locale.ENGLISH)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"email\":\"invalid-secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.errors.name", org.hamcrest.Matchers.hasItem("must not be blank")))
                .andExpect(jsonPath("$.errors.email").isArray())
                .andExpect(jsonPath("$.detail").value("One or more fields are invalid."))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("invalid-secret"))));
        verifyNoInteractions(service);
    }

}
