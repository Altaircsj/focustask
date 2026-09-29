package br.edu.ufersa.pw.focustask.features.user.dto;

import br.edu.ufersa.pw.focustask.features.user.validation.Utf8ByteLength;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record LoginRequestDTO(
        @NotBlank @Email @Size(max = 254) String email,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank @Utf8ByteLength(max = 72) String password) {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Unknown authentication field");
    }
    @Override public String toString() { return "LoginRequestDTO[credentials redacted]"; }
}
