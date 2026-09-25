package com.dsatracker.service;

import com.dsatracker.dto.AuthResponse;
import com.dsatracker.dto.ChangePasswordRequest;
import com.dsatracker.dto.DeleteAccountRequest;
import com.dsatracker.dto.LoginRequest;
import com.dsatracker.dto.ProfileResponse;
import com.dsatracker.dto.RegisterRequest;
import com.dsatracker.dto.UpdateProfileRequest;
import com.dsatracker.model.User;
import com.dsatracker.repository.ContestProblemRepository;
import com.dsatracker.repository.ContestSessionRepository;
import com.dsatracker.repository.SubmissionRepository;
import com.dsatracker.repository.UserProgressRepository;
import com.dsatracker.repository.UserRepository;
import com.dsatracker.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestSessionRepository contestSessionRepository;
    private final SubmissionRepository submissionRepository;
    private final UserProgressRepository userProgressRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ContestProblemRepository contestProblemRepository,
            ContestSessionRepository contestSessionRepository,
            SubmissionRepository submissionRepository,
            UserProgressRepository userProgressRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.contestProblemRepository = contestProblemRepository;
        this.contestSessionRepository = contestSessionRepository;
        this.submissionRepository = submissionRepository;
        this.userProgressRepository = userProgressRepository;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (email.isEmpty() || !email.contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email address.");
        }
        if (request.password() == null || request.password().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with that email already exists.");
        }

        String displayName = request.displayName() == null || request.displayName().isBlank()
                ? email.substring(0, email.indexOf('@'))
                : request.displayName().trim();

        User user = new User();
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect email or password."));

        if (request.password() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect email or password.");
        }

        return toAuthResponse(user);
    }

    public ProfileResponse getProfile(User user) {
        return toProfileResponse(user);
    }

    public ProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        if (request.displayName() != null) {
            if (request.displayName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name can't be blank.");
            }
            user.setDisplayName(request.displayName().trim());
        }
        if (request.bio() != null) {
            String bio = request.bio().trim();
            if (bio.length() > 280) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bio must be 280 characters or fewer.");
            }
            user.setBio(bio.isEmpty() ? null : bio);
        }
        user = userRepository.save(user);
        return toProfileResponse(user);
    }

    public void changePassword(User user, ChangePasswordRequest request) {
        if (request.currentPassword() == null
                || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect.");
        }
        if (request.newPassword() == null || request.newPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 6 characters.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /**
     * Permanently deletes the caller's account and every row that belongs to them, in FK-safe
     * order (contest problems -> contest sessions -> submissions -> progress -> the user row
     * itself). Requires the current password as confirmation, exactly like {@link #changePassword}.
     */
    @Transactional
    public void deleteAccount(User user, DeleteAccountRequest request) {
        if (request.currentPassword() == null
                || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect.");
        }
        Long userId = user.getId();
        contestProblemRepository.deleteAllByContestSession_User_Id(userId);
        contestSessionRepository.deleteAllByUserId(userId);
        submissionRepository.deleteAllByUserId(userId);
        userProgressRepository.deleteAllByUserId(userId);
        userRepository.delete(user);
    }

    private ProfileResponse toProfileResponse(User user) {
        return new ProfileResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getBio(), user.getCreatedAt());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getDisplayName());
    }
}
