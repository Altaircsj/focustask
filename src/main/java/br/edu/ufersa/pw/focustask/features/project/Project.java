package br.edu.ufersa.pw.focustask.features.project;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    @Column(nullable = false, length = 7)
    private String color = ProjectAppearance.DEFAULT_COLOR;

    @ElementCollection
    @CollectionTable(name = "project_tags", joinColumns = @JoinColumn(name = "project_id"))
    @OrderColumn(name = "tag_position")
    @Column(name = "tag_text", nullable = false, length = 30)
    private List<String> tags = new ArrayList<>();

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

    public String getColor() { return color; }
    public List<String> getTags() { return tags; }

    void updateAppearance(String color, List<String> tags) {
        if (color != null) this.color = color.toUpperCase(Locale.ROOT);
        if (tags != null) {
            this.tags.clear();
            tags.stream().map(ProjectAppearance::trimTag).forEach(this.tags::add);
        }
    }

    public void setName(String name) {
        if (name == null || name.isBlank() || name.length() > 255) {
            throw new IllegalArgumentException("Name must contain between 1 and 255 characters");
        }
        this.name = name;
    }

    public void setDescription(String description) { this.description = description; }
    ProjectDTO toDTO() { return new ProjectDTO(id, userId, name, description); }
}
