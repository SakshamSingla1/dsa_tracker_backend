package com.dsatracker.security;

import com.dsatracker.model.User;
import com.dsatracker.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Long userId = jwtService.extractUserId(token);
                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    User user = userRepository.findById(userId).orElse(null);
                    // Disabled accounts are rejected on every request (not just at login) --
                    // re-checked fresh from the DB each time, so disabling someone's role/account
                    // takes effect immediately without needing a token-revocation list.
                    if (user != null && user.isEnabled()) {
                        var auth = new UsernamePasswordAuthenticationToken(user, null, authoritiesFor(user));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception ignored) {
                // invalid/expired token -> request proceeds unauthenticated and gets a 401 downstream
            }
        }

        filterChain.doFilter(request, response);
    }

    /** Permission codes computed fresh from the DB every request, never embedded in the JWT
     *  itself -- the token lives 30 days (app.jwt.expiration-ms), so a stale copy of a user's
     *  permissions would mean narrowing/revoking admin access has no effect until the token
     *  expires. Role.permissions is EAGER, so this is the same findById query widened, not an
     *  extra round trip. */
    private List<GrantedAuthority> authoritiesFor(User user) {
        if (user.getRole() == null) {
            return List.of();
        }
        return user.getRole().getPermissions().stream()
                .map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p.getCode()))
                .toList();
    }
}
