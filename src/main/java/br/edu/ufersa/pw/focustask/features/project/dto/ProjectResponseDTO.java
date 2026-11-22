package br.edu.ufersa.pw.focustask.features.project.dto;

import java.util.List;

public record ProjectResponseDTO(
        Long id,
        Long userId,
        String name,
        String description,
        String color,
        List<String> tags
) {}
