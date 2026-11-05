package com.petclinic.authservice.dto;

import com.petclinic.authservice.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** An admin creates a staff account (ADMIN, VET or RECEPTIONIST). */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateStaffRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Role is required")
    private Role role;
}
