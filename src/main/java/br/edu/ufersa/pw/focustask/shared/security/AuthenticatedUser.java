package br.edu.ufersa.pw.focustask.shared.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

/** A security adapter, never a second persisted identity or an HTTP response. */
public final class AuthenticatedUser implements UserDetails, CredentialsContainer {
    private final Long id;
    private final String email;
    private String passwordHash;
    private final List<GrantedAuthority> authorities;

    public AuthenticatedUser(Long id, String email, String passwordHash,
                             Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.authorities = List.copyOf(authorities);
    }
    public Long getId() { return id; }
    @Override public String getUsername() { return email; }
    @JsonIgnore @Override public String getPassword() { return passwordHash; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
    @Override public void eraseCredentials() { passwordHash = null; }
    @Override public String toString() { return "AuthenticatedUser[credentials redacted]"; }
}
