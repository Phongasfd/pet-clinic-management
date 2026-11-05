package com.petclinic.petservice.controller;

import com.petclinic.petservice.dto.PetDto;
import com.petclinic.petservice.security.StaffOnly;
import com.petclinic.petservice.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
@Tag(name = "Pet API", description = "Pets")
public class PetController {

    private final PetService petService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a pet (customers: to their own profile only)")
    public PetDto.Response addPet(@RequestBody @Valid PetDto.CreateRequest request) {
        return petService.addPet(request);
    }

    @PatchMapping("/{id}/health-info")
    @StaffOnly
    @Operation(summary = "Update a pet's weight and allergies (staff only)")
    public PetDto.Response updatePetHealthInfo(
            @PathVariable Long id,
            @RequestParam(required = false) Double weight,
            @RequestParam(required = false) String allergies) {
        return petService.updatePetHealthInfo(id, weight, allergies);
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "List an owner's pets (customers: own pets only)")
    public List<PetDto.Response> getPetsByOwner(@PathVariable Long ownerId) {
        return petService.getPetsByOwner(ownerId);
    }
}
