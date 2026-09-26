package com.petclinic.petservice.controller;

import com.petclinic.petservice.entity.BoardingTicket;
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
@Tag(name = "Boarding API", description = "Quản lý lưu chuồng (Khách sạn thú cưng)")
public class BoardingController {

    private final BoardingService boardingService;

    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Check-in lưu chuồng")
    public BoardingTicket checkIn(
            @RequestParam Long petId,
            @RequestParam Long cageId,
            @RequestParam LocalDateTime expectedCheckOutDate,
            @RequestParam(required = false) String notes) {
        return boardingService.checkIn(petId, cageId, expectedCheckOutDate, notes);
    }

    @PostMapping("/tickets/{ticketId}/check-out")
    @Operation(summary = "Check-out hoàn tất lưu chuồng")
    public BoardingTicket checkOut(@PathVariable Long ticketId) {
        return boardingService.checkOut(ticketId);
    }
}
