package com.petclinic.petservice.controller;

import com.petclinic.petservice.dto.BoardingTicketDto;
import com.petclinic.petservice.security.StaffOnly;
import com.petclinic.petservice.service.BoardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/boarding")
@RequiredArgsConstructor
@StaffOnly
@Tag(name = "Boarding API", description = "Pet hotel: cage check-in and check-out (staff only)")
public class BoardingController {

    private final BoardingService boardingService;

    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Check a pet into a cage (409 if the cage is taken or the pet is already boarding)")
    public BoardingTicketDto.Response checkIn(
            @RequestParam Long petId,
            @RequestParam Long cageId,
            @RequestParam LocalDateTime expectedCheckOutDate,
            @RequestParam(required = false) String notes) {
        return boardingService.checkIn(petId, cageId, expectedCheckOutDate, notes);
    }

    @PostMapping("/tickets/{ticketId}/check-out")
    @Operation(summary = "Check a pet out and free its cage")
    public BoardingTicketDto.Response checkOut(@PathVariable Long ticketId) {
        return boardingService.checkOut(ticketId);
    }
}
