package com.dsatracker.service;

import com.dsatracker.dto.AuthResponse;
import com.dsatracker.dto.ChangePasswordRequest;
import com.dsatracker.dto.DeleteAccountRequest;
import com.dsatracker.dto.LoginRequest;
import com.dsatracker.dto.RegisterRequest;
import com.dsatracker.model.User;
import com.dsatracker.repository.ContestProblemRepository;
import com.dsatracker.repository.ContestSessionRepository;
import com.dsatracker.repository.SubmissionRepository;
import com.dsatracker.repository.UserProgressRepository;
import com.dsatracker.repository.UserRepository;
import com.dsatracker.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock ContestProblemRepository contestProblemRepository;
    @Mock ContestSessionRepository contestSessionRepository;
    @Mock SubmissionRepository submissionRepository;
    @Mock UserProgressRepository userProgressRepository;
    @Mock JwtService jwtService;

    // Real BCrypt, not mocked -- change/delete-password tests need genuine
    // encode/matches behavior, not a stubbed always-true/false.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, passwordEncoder, jwtService,
                contestProblemRepository, contestSessionRepository, submissionRepository, userProgressRepository
        );
    }

    private User userWithPassword(String rawPassword) {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return user;
    }

    @Test
    void register_succeedsWithValidInput() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });
        when(jwtService.generateToken(42L, "test@example.com")).thenReturn("signed-token");

        AuthResponse response = authService.register(new RegisterRequest("test@example.com", "password123", "Test User"));

        assertThat(response.token()).isEqualTo("signed-token");
        assertThat(response.userId()).isEqualTo(42L);
        assertThat(response.displayName()).isEqualTo("Test User");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        // Password must never be stored in plaintext.
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void register_defaultsDisplayNameFromEmailWhenBlank() {
        when(userRepository.existsByEmail("nobody@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(), anyString())).thenReturn("token");

        AuthResponse response = authService.register(new RegisterRequest("nobody@example.com", "password123", "  "));

        assertThat(response.displayName()).isEqualTo("nobody");
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("taken@example.com", "password123", "Name")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void register_rejectsInvalidEmail() {
        assertThatThrownBy(() -> authService.register(new RegisterRequest("not-an-email", "password123", "Name")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("valid email");
    }

    @Test
    void register_rejectsShortPassword() {
        assertThatThrownBy(() -> authService.register(new RegisterRequest("test@example.com", "short", "Name")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("6 characters");
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = userWithPassword("correct-password");
        when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "wrong-password")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Incorrect email or password");
    }

    @Test
    void login_succeedsWithCorrectPassword() {
        User user = userWithPassword("correct-password");
        when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(user));
        when(jwtService.generateToken(1L, "test@example.com")).thenReturn("signed-token");

        AuthResponse response = authService.login(new LoginRequest("test@example.com", "correct-password"));

        assertThat(response.token()).isEqualTo("signed-token");
    }

    @Test
    void login_rejectsUnknownEmail() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "whatever")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Incorrect email or password");
    }

    @Test
    void changePassword_rejectsWrongCurrentPassword() {
        User user = userWithPassword("old-password");

        assertThatThrownBy(() -> authService.changePassword(user, new ChangePasswordRequest("not-the-old-password", "new-password-123")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void changePassword_succeedsAndNewPasswordActuallyVerifiesAfterward() {
        User user = userWithPassword("old-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.changePassword(user, new ChangePasswordRequest("old-password", "brand-new-password"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(passwordEncoder.matches("brand-new-password", saved.getValue().getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("old-password", saved.getValue().getPasswordHash())).isFalse();
    }

    @Test
    void changePassword_rejectsShortNewPassword() {
        User user = userWithPassword("old-password");

        assertThatThrownBy(() -> authService.changePassword(user, new ChangePasswordRequest("old-password", "short")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("6 characters");
    }

    @Test
    void deleteAccount_rejectsWrongCurrentPassword() {
        User user = userWithPassword("old-password");

        assertThatThrownBy(() -> authService.deleteAccount(user, new DeleteAccountRequest("not-the-old-password")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void deleteAccount_deletesEveryOwnedRowInFkSafeOrderThenTheUser() {
        User user = userWithPassword("old-password");
        user.setId(7L);

        authService.deleteAccount(user, new DeleteAccountRequest("old-password"));

        verify(contestProblemRepository).deleteAllByContestSession_User_Id(7L);
        verify(contestSessionRepository).deleteAllByUserId(7L);
        verify(submissionRepository).deleteAllByUserId(7L);
        verify(userProgressRepository).deleteAllByUserId(7L);
        verify(userRepository).delete(user);
    }
}
