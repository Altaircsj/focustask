package br.edu.ufersa.pw.focustask.features.user.dto;

public record TokenResponseDTO(String token) {
    @Override public String toString() { return "TokenResponseDTO[token redacted]"; }
}
