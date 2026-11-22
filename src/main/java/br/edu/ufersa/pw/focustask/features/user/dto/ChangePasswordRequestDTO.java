package br.edu.ufersa.pw.focustask.features.user.dto;

import br.edu.ufersa.pw.focustask.features.user.validation.Utf8ByteLength;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDTO(
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank @Utf8ByteLength(max = 72) String currentPassword,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank @Size(min = 6) @Utf8ByteLength(max = 72) String newPassword) {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Unknown authentication field");
    }

    @Override public String toString() { return "ChangePasswordRequestDTO[credentials redacted]"; }
}
