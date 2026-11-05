package com.petclinic.petservice.dto;

import com.petclinic.petservice.entity.SpaTicket;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

public class SpaTicketDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long petId;
        private List<String> services;
        private String coatCondition;
        private String groomerName;
        private SpaTicket.SpaStatus status;
        private BigDecimal estimatedPrice;

        public static Response from(SpaTicket ticket) {
            return Response.builder()
                    .id(ticket.getId())
                    .petId(ticket.getPet().getId())
                    .services(ticket.getServices())
                    .coatCondition(ticket.getCoatCondition())
                    .groomerName(ticket.getGroomerName())
                    .status(ticket.getStatus())
                    .estimatedPrice(ticket.getEstimatedPrice())
                    .build();
        }
    }
}
