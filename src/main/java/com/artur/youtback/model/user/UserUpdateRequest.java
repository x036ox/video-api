package com.artur.youtback.model.user;

public record UserUpdateRequest(String email, String authorities, String picture) {
    @Override
    public String toString() {
        return "UserUpdateRequest{" +
                ", username='" + email + '\'' +
                ", picture=" + picture +
                '}';
    }
}
