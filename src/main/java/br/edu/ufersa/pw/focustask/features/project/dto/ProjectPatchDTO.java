package br.edu.ufersa.pw.focustask.features.project.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import br.edu.ufersa.pw.focustask.features.project.ProjectAppearance;
import br.edu.ufersa.pw.focustask.features.project.ValidProjectTags;
import java.util.List;

public record ProjectPatchDTO(
        @Size(min = 1, max = 255)
        @Pattern(regexp = "(?s).*[^\\p{javaWhitespace}].*", message = "must not be blank") String name,
        String description,
        @Pattern(regexp = ProjectAppearance.COLOR_PATTERN, message = "Selecione uma cor da paleta.") String color,
        @ValidProjectTags List<String> tags
) {
    public ProjectPatchDTO(String name, String description) { this(name, description, null, null); }
}
