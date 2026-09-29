package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.shared.security.AuthenticatedUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository repository;
    UserDetailsServiceImpl(UserRepository repository) { this.repository = repository; }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        User user = repository.findByEmail(User.normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        var authorities = user.getRole() == UserRole.ADMIN
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getPasswordHash(), authorities);
    }
}
