package com.dsatracker.dto;

public record ChangePasswordRequest(String currentPassword, String newPassword) {
}
