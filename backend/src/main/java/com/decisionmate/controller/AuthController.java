
package com.decisionmate.controller;

import com.decisionmate.model.AppUser;
import com.decisionmate.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
   private final SecurityContextRepository securityContextRepository;

   public AuthController(
        AppUserRepository users,
        PasswordEncoder passwordEncoder,
        SecurityContextRepository securityContextRepository
) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.securityContextRepository = securityContextRepository;
}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "An account with this email already exists"
            );
        }

        AppUser user = users.save(new AppUser(
            request.name().trim(),
            email,
            passwordEncoder.encode(request.password())
        ));

        return loginUser(user, httpRequest, httpResponse);
    }

    @PostMapping("/login")
    public Map<String, Object> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        AppUser user = users.findByEmail(email)
            .filter(u -> passwordEncoder.matches(
                request.password(), u.getPasswordHash()))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
            ));

        return loginUser(user, httpRequest, httpResponse);
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Please log in");
        }

        AppUser user = users.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "User not found"));

        return userResponse(user);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();
    }

    private Map<String, Object> loginUser(
            AppUser user,
            HttpServletRequest request,
            HttpServletResponse response) {

        // Prevent session fixation by rotating an existing session ID.
        HttpSession session = request.getSession(false);
        if (session == null) {
            session = request.getSession(true);
        } else {
            request.changeSessionId();
        }

        Authentication authentication =
            UsernamePasswordAuthenticationToken.authenticated(
                user.getEmail(),
                null,
                List.of()
            );

        SecurityContext context =
            SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Explicitly persist authentication for subsequent requests.
        System.out.println("LOGIN SESSION ID: " + request.getSession(false).getId());
System.out.println("LOGIN AUTH USER: " + SecurityContextHolder.getContext().getAuthentication().getName());
        return userResponse(user);
    }

    private Map<String, Object> userResponse(AppUser user) {
        return Map.of(
            "id", user.getId(),
            "name", user.getName(),
            "email", user.getEmail()
        );
    }

    public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(min = 8, max = 72) String password
    ) {}

    public record LoginRequest(
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank String password
    ) {}
}
