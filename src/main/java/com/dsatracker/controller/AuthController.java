package com.dsatracker.controller;

import com.dsatracker.dto.AuthResponse;
import com.dsatracker.dto.ChangePasswordRequest;
import com.dsatracker.dto.DeleteAccountRequest;
import com.dsatracker.dto.LoginRequest;
import com.dsatracker.dto.ProfileResponse;
import com.dsatracker.dto.RegisterRequest;
import com.dsatracker.dto.UpdateProfileRequest;
import com.dsatracker.model.User;
import com.dsatracker.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/profile")
    public ProfileResponse profile(@AuthenticationPrincipal User user) {
        return authService.getProfile(user);
    }

    @PatchMapping("/profile")
    public ProfileResponse updateProfile(@AuthenticationPrincipal User user, @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(user, request);
    }

    @PostMapping("/change-password")
    public void changePassword(@AuthenticationPrincipal User user, @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user, request);
    }

    @DeleteMapping("/account")
    public void deleteAccount(@AuthenticationPrincipal User user, @RequestBody DeleteAccountRequest request) {
        authService.deleteAccount(user, request);
    }
}
