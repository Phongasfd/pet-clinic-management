package com.petclinic.petservice.controller;

import com.petclinic.petservice.dto.OwnerDto;
import com.petclinic.petservice.security.StaffOnly;
import com.petclinic.petservice.service.OwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/owners")
@RequiredArgsConstructor
@Tag(name = "Owner API", description = "Pet owners")
public class OwnerController {

    private final OwnerService ownerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an owner: customers create their own profile, staff register walk-in owners")
    public OwnerDto.Response createOwner(@RequestBody @Valid OwnerDto.CreateRequest request) {
        return ownerService.createOwner(request);
    }

    @GetMapping("/me")
    @Operation(summary = "The owner profile linked to the caller's account")
    public OwnerDto.Response getMyOwner() {
        return ownerService.getMyOwner();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an owner by id (customers: own profile only)")
    public OwnerDto.Response getOwner(@PathVariable Long id) {
        return ownerService.getOwnerById(id);
    }

    @GetMapping
    @StaffOnly
    @Operation(summary = "Search owners by phone number, paged (staff only)")
    public Page<OwnerDto.Response> searchOwners(@RequestParam String phone, Pageable pageable) {
        return ownerService.searchByPhone(phone, pageable);
    }
}
