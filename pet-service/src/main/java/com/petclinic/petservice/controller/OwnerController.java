package com.petclinic.petservice.controller;

import com.petclinic.petservice.dto.OwnerDto;
import com.petclinic.petservice.entity.Owner;
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
@Tag(name = "Owner API", description = "Quản lý chủ sở hữu thú cưng")
public class OwnerController {

    private final OwnerService ownerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Thêm mới chủ sở hữu")
    public Owner createOwner(@RequestBody @Valid OwnerDto.CreateRequest request) {
        return ownerService.createOwner(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin chủ sở hữu theo ID")
    public Owner getOwner(@PathVariable Long id) {
        return ownerService.getOwnerById(id);
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm chủ sở hữu theo số điện thoại (có phân trang)")
    public Page<Owner> searchOwners(@RequestParam String phone, Pageable pageable) {
        return ownerService.searchByPhone(phone, pageable);
    }
}
