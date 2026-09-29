package br.edu.ufersa.pw.focustask;

import br.edu.ufersa.pw.focustask.shared.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/** Controller contract tests use the real chain and an explicitly supplied typed principal.
 * JWT authentication itself is covered by AuthSecurityTests without mocked tokens. */
@Import({SecurityConfig.class, SecurityProblemHandler.class})
public abstract class AuthenticatedMvcTest {
    @Autowired protected MockMvc mvc;
    @Autowired WebApplicationContext context;
    @MockitoBean TokenService tokenService;
    @MockitoBean UserDetailsService userDetailsService;
    public static AuthenticatedUser principal(long id) {
        return new AuthenticatedUser(id, "test@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
    @BeforeEach void authenticateControllerRequests() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())
                .defaultRequest(get("/").with(user(principal(7L)))).build();
    }
}
