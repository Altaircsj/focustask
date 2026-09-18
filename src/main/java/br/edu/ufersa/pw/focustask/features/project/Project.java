package br.edu.ufersa.pw.focustask.features.project;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;

@Entity
@Table(name = "projects", indexes = @Index(name = "idx_projects_user", columnList = "user_id"))
class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    protected Project() {}

    Project(Long userId, String name, String description) {
        this.userId = Objects.requireNonNull(userId, "User ID is required");
        setName(name);
        this.description = description;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getDescription() { return description; }

    public void setName(String name) {
        if (name == null || name.isBlank() || name.length() > 255) {
            throw new IllegalArgumentException("Name must contain between 1 and 255 characters");
        }
        this.name = name;
    }

    public void setDescription(String description) { this.description = description; }
    ProjectDTO toDTO() { return new ProjectDTO(id, userId, name, description); }
}
