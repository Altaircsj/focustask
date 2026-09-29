package br.edu.ufersa.pw.focustask.features.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String name;

    @NotBlank
    @Email
    @Size(max = 254)
    @Column(nullable = false, length = 254)
    private String email;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @jakarta.validation.constraints.NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    protected User() {}

    User(String name, String email, String passwordHash) {
        setName(name);
        setEmail(email);
        if (passwordHash == null || passwordHash.isBlank() || passwordHash.length() > 255) {
            throw new IllegalArgumentException("Password hash is required and must fit the stored field");
        }
        this.passwordHash = passwordHash;
    }

    User copy() {
        User copy = new User(name, email, passwordHash);
        copy.role = role;
        return copy;
    }

    String getPasswordHash() { return passwordHash; }
    UserRole getRole() { return role; }
    static String normalizeEmail(String email) { return email.strip().toLowerCase(Locale.ROOT); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    public void setName(String name) {
        if (name == null || name.isBlank() || name.length() > 255) {
            throw new IllegalArgumentException("Name must contain between 1 and 255 characters");
        }
        this.name = name;
    }

    public void setEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        String normalized = normalizeEmail(email);
        if (normalized.length() > 254) {
            throw new IllegalArgumentException("Email must contain at most 254 characters");
        }
        this.email = normalized;
    }

    UserDTO toDTO() { return new UserDTO(id, name, email); }
}
