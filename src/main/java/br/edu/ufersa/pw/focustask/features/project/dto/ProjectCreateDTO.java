package br.edu.ufersa.pw.focustask.features.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import br.edu.ufersa.pw.focustask.features.project.ProjectAppearance;
import br.edu.ufersa.pw.focustask.features.project.ValidProjectTags;
import java.util.List;
import jakarta.validation.constraints.Pattern;

public record ProjectCreateDTO(
        @NotBlank @Size(max = 255) String name,
        String description,
        @Pattern(regexp = ProjectAppearance.COLOR_PATTERN, message = "Selecione uma cor da paleta.") String color,
        @ValidProjectTags List<String> tags
) {
    public ProjectCreateDTO(String name, String description) { this(name, description, null, null); }
}
