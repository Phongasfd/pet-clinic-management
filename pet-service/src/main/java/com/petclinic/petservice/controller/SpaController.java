package com.petclinic.petservice.controller;

import com.petclinic.petservice.entity.SpaTicket;
import com.petclinic.petservice.service.SpaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/spa-tickets")
@RequiredArgsConstructor
@Tag(name = "Spa API", description = "Quản lý vé dịch vụ Spa")
public class SpaController {

    private final SpaService spaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo vé Spa mới")
    public SpaTicket createSpaTicket(
            @RequestParam Long petId,
            @RequestParam List<String> services,
            @RequestParam(required = false) String coatCondition,
            @RequestParam(required = false) BigDecimal estimatedPrice) {
        return spaService.createSpaTicket(petId, services, coatCondition, estimatedPrice);
    }

    @PatchMapping("/{ticketId}/status")
    @Operation(summary = "Cập nhật trạng thái vé Spa")
    public SpaTicket updateStatus(
            @PathVariable Long ticketId,
            @RequestParam SpaTicket.SpaStatus newStatus) {
        return spaService.updateStatus(ticketId, newStatus);
    }
}
