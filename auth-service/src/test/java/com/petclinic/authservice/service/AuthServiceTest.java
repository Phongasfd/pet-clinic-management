package com.petclinic.authservice.service;

import com.petclinic.authservice.dto.CreateStaffRequest;
import com.petclinic.authservice.dto.LoginRequest;
import com.petclinic.authservice.dto.RegisterRequest;
import com.petclinic.authservice.entity.Role;
import com.petclinic.authservice.entity.User;
import com.petclinic.authservice.repository.UserRepository;
import com.petclinic.authservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private JwtService jwtService;
    private AuthService authService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(userRepository, encoder, jwtService);
    }

    @Test
    void register_alwaysCreatesCustomer() {
        RegisterRequest request = RegisterRequest.builder()
                .username("mallory").password("pw").fullName("Mallory").build();

        authService.register(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals(Role.CUSTOMER, saved.getValue().getRole());
    }

    @Test
    void register_duplicateUsernameIs409() {
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> authService.register(
                RegisterRequest.builder().username("bob").password("pw").fullName("Bob").build()));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }

    @Test
    void login_wrongPasswordIs401() {
        User user = User.builder().id(1L).username("bob").password(encoder.encode("right"))
                .role(Role.CUSTOMER).enabled(true).build();
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> authService.login(
                LoginRequest.builder().username("bob").password("wrong").build()));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
    }

    @Test
    void login_putsUserIdInToken() {
        User user = User.builder().id(7L).username("bob").password(encoder.encode("right"))
                .role(Role.VET).enabled(true).build();
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(7L, "bob", "VET")).thenReturn("jwt");

        assertEquals("jwt", authService.login(LoginRequest.builder().username("bob").password("right").build()).getToken());
    }

    private static CreateStaffRequest staff(Role role) {
        return CreateStaffRequest.builder().username("vet1").password("pw").fullName("Dr. Vet").role(role).build();
    }

    @Test
    void createStaff_onlyAdminsMayCreateStaff() {
        for (String caller : new String[]{null, "CUSTOMER", "VET", "RECEPTIONIST"}) {
            ResponseStatusException e = assertThrows(ResponseStatusException.class,
                    () -> authService.createStaff(staff(Role.VET), caller));
            assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        }
        verify(userRepository, never()).save(any());
    }

    @Test
    void createStaff_adminCreatesAccountWithTheRequestedRole() {
        authService.createStaff(staff(Role.VET), "ADMIN");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals(Role.VET, saved.getValue().getRole());
        assertTrue(encoder.matches("pw", saved.getValue().getPassword()));
    }

    @Test
    void createStaff_customersUseRegisterInstead() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> authService.createStaff(staff(Role.CUSTOMER), "ADMIN"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    void ensureAdmin_isIdempotent() {
        // first startup: checked by ensureAdmin and again by createUser; second startup: exists
        when(userRepository.existsByUsername("admin")).thenReturn(false, false, true);

        assertTrue(authService.ensureAdmin("admin", "secret"));
        assertFalse(authService.ensureAdmin("admin", "secret"));
        verify(userRepository, times(1)).save(any());
    }
}
