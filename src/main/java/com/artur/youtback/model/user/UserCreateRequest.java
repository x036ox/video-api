package com.artur.youtback.model.user;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

public record UserCreateRequest(
        @NotEmpty String id,
        @NotEmpty String username,
        @NotEmpty String authorities,
        @Nullable MultipartFile picture) {
}
