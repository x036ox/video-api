package com.artur.youtback.model.user;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.lang.Nullable;

public record UserCreateRequest(
        @NotEmpty String id,
        @NotEmpty String username,
        @NotEmpty String email,
        @Nullable String authorities,
        @Nullable String picture) {
}
