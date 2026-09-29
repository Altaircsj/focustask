package br.edu.ufersa.pw.focustask.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;

/** Registered only by SecurityConfig, not as a second servlet-container filter. */
public class SecurityFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);
    private final TokenService tokens;
    private final UserDetailsService users;
    private final SecurityProblemHandler problems;

    public SecurityFilter(TokenService tokens, UserDetailsService users, SecurityProblemHandler problems) {
        this.tokens = tokens;
        this.users = users;
        this.problems = problems;
    }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return request.getMethod().equals("POST")
                && (path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/register"));
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain chain) throws ServletException, IOException {
        var headers = Collections.list(request.getHeaders("Authorization"));
        if (headers.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        try {
            if (headers.size() != 1 || !headers.getFirst().matches("(?i)Bearer [^\\s,]+")) {
                throw new BadCredentialsException("Invalid authorization header");
            }
            var identity = tokens.verify(headers.getFirst().substring(7));
            var principal = (AuthenticatedUser) users.loadUserByUsername(identity.email());
            if (!identity.id().equals(principal.getId()) || !identity.email().equals(principal.getUsername())) {
                throw new BadCredentialsException("Invalid token identity");
            }
            principal.eraseCredentials();
            var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            problems.commence(request, response, ex);
            return;
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            // No token, principal, password or request payload is logged.
            log.error("Unable to load authentication identity", ex);
            problems.write(request, response, HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }
        chain.doFilter(request, response);
    }
}
