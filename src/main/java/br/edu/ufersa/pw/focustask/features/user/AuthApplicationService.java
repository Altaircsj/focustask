package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.features.user.dto.*;
import br.edu.ufersa.pw.focustask.shared.security.AuthenticatedUser;
import br.edu.ufersa.pw.focustask.shared.security.TokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuthApplicationService {
    private final UserRepository repository;
    private final UserService domain;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokens;

    AuthApplicationService(UserRepository repository, UserService domain, PasswordEncoder encoder,
                           AuthenticationManager authenticationManager, TokenService tokens) {
        this.repository = repository;
        this.domain = domain;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
    }

    @Transactional
    public void register(RegisterRequestDTO dto) {
        String email = User.normalizeEmail(dto.email());
        domain.validarCriacao(email);
        repository.save(new User(dto.name(), email, encoder.encode(dto.password())));
    }

    public TokenResponseDTO login(LoginRequestDTO dto) {
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(dto.email(), dto.password()));
        return new TokenResponseDTO(tokens.generate((AuthenticatedUser) authentication.getPrincipal()));
    }
}
