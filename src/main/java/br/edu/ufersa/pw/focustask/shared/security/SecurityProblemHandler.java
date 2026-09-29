package br.edu.ufersa.pw.focustask.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.net.URI;

@Component
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final JsonMapper mapper;
    public SecurityProblemHandler(JsonMapper mapper) { this.mapper = mapper; }

    public static ProblemDetail problem(HttpStatus status) {
        String detail = switch (status) {
            case UNAUTHORIZED -> "Authentication is required or credentials are invalid.";
            case FORBIDDEN -> "Access is denied.";
            default -> "An unexpected error occurred. Please try again later.";
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        return problem;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN);
    }

    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status) throws IOException {
        if (response.isCommitted()) return;
        ProblemDetail problem = problem(status);
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        if (status == HttpStatus.UNAUTHORIZED) response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        mapper.writeValue(response.getOutputStream(), problem);
    }
}
