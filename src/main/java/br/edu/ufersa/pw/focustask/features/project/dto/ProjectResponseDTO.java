package br.edu.ufersa.pw.focustask.features.project.dto;

public record ProjectResponseDTO(
        Long id,
        Long userId,
        String name,
        String description
) {}
