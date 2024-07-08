package com.artur.youtback.model.user;

import org.springframework.web.multipart.MultipartFile;

public record UserUpdateRequest(String username, String authorities, MultipartFile picture) {
    @Override
    public String toString() {
        return "UserUpdateRequest{" +
                ", username='" + username + '\'' +
                ", picture=" + picture +
                '}';
    }
}
