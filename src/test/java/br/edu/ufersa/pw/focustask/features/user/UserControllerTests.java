package br.edu.ufersa.pw.focustask.features.user;

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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean UserService service;

    @ParameterizedTest
    @MethodSource("validRequests")
    void validContractsKeepExecutionPending(String method, String url, String body) throws Exception {
        var request = request(HttpMethod.valueOf(method), url);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        mvc.perform(request).andExpect(status().isOk()).andExpect(content().string(""));
        verifyNoInteractions(service);
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
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("POST", "/api/v1/users", "{}"),
                Arguments.of("PUT", "/api/v1/users/7", "{}"),
                Arguments.of("PATCH", "/api/v1/users/7", "{\"email\":\"invalid\"}"));
    }
}
