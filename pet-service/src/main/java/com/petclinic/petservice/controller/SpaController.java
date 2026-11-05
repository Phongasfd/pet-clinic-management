package com.petclinic.petservice.controller;

import com.petclinic.petservice.dto.SpaTicketDto;
import com.petclinic.petservice.entity.SpaTicket;
import com.petclinic.petservice.security.StaffOnly;
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
@StaffOnly
@Tag(name = "Spa API", description = "Spa and grooming tickets (staff only)")
public class SpaController {

    private final SpaService spaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a spa ticket")
    public SpaTicketDto.Response createSpaTicket(
            @RequestParam Long petId,
            @RequestParam List<String> services,
            @RequestParam(required = false) String coatCondition,
            @RequestParam(required = false) BigDecimal estimatedPrice) {
        return spaService.createSpaTicket(petId, services, coatCondition, estimatedPrice);
    }

    @PatchMapping("/{ticketId}/status")
    @Operation(summary = "Move a spa ticket forward: RECEIVED → IN_PROGRESS → COMPLETED")
    public SpaTicketDto.Response updateStatus(
            @PathVariable Long ticketId,
            @RequestParam SpaTicket.SpaStatus newStatus) {
        return spaService.updateStatus(ticketId, newStatus);
    }
}
