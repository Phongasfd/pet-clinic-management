package com.petclinic.petservice.dto;

import com.petclinic.petservice.entity.BoardingTicket;
import lombok.*;

import java.time.LocalDateTime;

public class BoardingTicketDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long petId;
        private Long cageId;
        private String cageCode;
        private LocalDateTime checkInDate;
        private LocalDateTime expectedCheckOutDate;
        private LocalDateTime actualCheckOutDate;
        private String specialCareNotes;
        private BoardingTicket.BoardingStatus status;

        public static Response from(BoardingTicket ticket) {
            return Response.builder()
                    .id(ticket.getId())
                    .petId(ticket.getPet().getId())
                    .cageId(ticket.getCage().getId())
                    .cageCode(ticket.getCage().getCode())
                    .checkInDate(ticket.getCheckInDate())
                    .expectedCheckOutDate(ticket.getExpectedCheckOutDate())
                    .actualCheckOutDate(ticket.getActualCheckOutDate())
                    .specialCareNotes(ticket.getSpecialCareNotes())
                    .status(ticket.getStatus())
                    .build();
        }
    }
}
