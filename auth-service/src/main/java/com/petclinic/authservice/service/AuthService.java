package com.petclinic.authservice.service;

import com.petclinic.authservice.dto.AuthResponse;
import com.petclinic.authservice.dto.CreateStaffRequest;
import com.petclinic.authservice.dto.LoginRequest;
import com.petclinic.authservice.dto.RegisterRequest;
import com.petclinic.authservice.entity.Role;
import com.petclinic.authservice.entity.User;
import com.petclinic.authservice.repository.UserRepository;
import com.petclinic.authservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    // Staff roles (ADMIN, VET, RECEPTIONIST) must be granted by an operator, never self-assigned
    static final Role SELF_REGISTERED_ROLE = Role.CUSTOMER;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.expiration-ms}")
    private long jwtExpiration;

    public void register(RegisterRequest request) {
        createUser(request.getUsername(), request.getPassword(), request.getFullName(), SELF_REGISTERED_ROLE);
    }

    /**
     * Staff accounts are created by an admin. {@code callerRole} is the X-User-Role header the
     * gateway sets from the caller's verified JWT (client-sent copies are stripped there).
     */
    public void createStaff(CreateStaffRequest request, String callerRole) {
        if (!Role.ADMIN.name().equals(callerRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an admin can create staff accounts");
        }
        if (request.getRole() == Role.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customers sign up with /register");
        }
        createUser(request.getUsername(), request.getPassword(), request.getFullName(), request.getRole());
    }

    /** First admin, from the environment (see AdminBootstrap). Idempotent across restarts. */
    public boolean ensureAdmin(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            return false;
        }
        createUser(username, password, "Administrator", Role.ADMIN);
        return true;
    }

    private void createUser(String username, String password, String fullName, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .fullName(fullName)
                .role(role)
                .enabled(true)
                .build();

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        // Same 401 for unknown user and wrong password, so usernames can't be probed
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is disabled");
        }

        String jwtToken = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        return AuthResponse.builder()
                .token(jwtToken)
                .type("Bearer")
                .expiresIn(jwtExpiration)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    public boolean validateToken(String token) {
        return jwtService.isTokenValid(token);
    }
}
