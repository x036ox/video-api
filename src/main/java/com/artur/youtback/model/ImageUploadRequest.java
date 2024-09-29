package com.artur.youtback.model;

import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

public record ImageUploadRequest(
        @Nullable String id,
        @NotNull MultipartFile image
) {
}
