package com.artur.youtback.model.user;

public record UserUpdateRequest(String username, String authorities, String picture) {
    @Override
    public String toString() {
        return "UserUpdateRequest{" +
                ", username='" + username + '\'' +
                ", picture=" + picture +
                '}';
    }
}
