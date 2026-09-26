package com.petclinic.petservice.controller;

import com.petclinic.petservice.dto.PetDto;
import com.petclinic.petservice.entity.Pet;
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
@Tag(name = "Pet API", description = "Quản lý thú cưng")
public class PetController {

    private final PetService petService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Thêm mới thú cưng")
    public Pet addPet(@RequestBody @Valid PetDto.CreateRequest request) {
        return petService.addPet(request);
    }

    @PatchMapping("/{id}/health-info")
    @Operation(summary = "Cập nhật nhanh cân nặng và tiền sử dị ứng của thú cưng")
    public Pet updatePetHealthInfo(
            @PathVariable Long id,
            @RequestParam(required = false) Double weight,
            @RequestParam(required = false) String allergies) {
        return petService.updatePetHealthInfo(id, weight, allergies);
    }
    
    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Lấy danh sách thú cưng theo ID chủ sở hữu")
    public List<Pet> getPetsByOwner(@PathVariable Long ownerId) {
        return petService.getPetsByOwner(ownerId);
    }
}
