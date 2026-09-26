package com.petclinic.petservice.service;

import com.petclinic.petservice.entity.BoardingTicket;
import com.petclinic.petservice.entity.Cage;
import com.petclinic.petservice.entity.Pet;
import com.petclinic.petservice.exception.CageUnavailableException;
import com.petclinic.petservice.exception.ResourceNotFoundException;
import com.petclinic.petservice.repository.BoardingTicketRepository;
import com.petclinic.petservice.repository.CageRepository;
import com.petclinic.petservice.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BoardingService {

    private final BoardingTicketRepository boardingTicketRepository;
    private final CageRepository cageRepository;
    private final PetRepository petRepository;

    @Transactional
    public BoardingTicket checkIn(Long petId, Long cageId, LocalDateTime expectedCheckOutDate, String notes) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thú cưng ID: " + petId));
        
        Cage cage = cageRepository.findById(cageId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuồng ID: " + cageId));
                
        if (cage.getStatus() != Cage.CageStatus.AVAILABLE) {
            throw new CageUnavailableException("Chuồng " + cage.getCode() + " không ở trạng thái trống (AVAILABLE).");
        }
        
        // Cập nhật trạng thái chuồng thành đã chiếm dụng
        cage.setStatus(Cage.CageStatus.OCCUPIED);
        cageRepository.save(cage);
        
        // Tạo vé lưu chuồng
        BoardingTicket ticket = BoardingTicket.builder()
                .pet(pet)
                .cage(cage)
                .checkInDate(LocalDateTime.now())
                .expectedCheckOutDate(expectedCheckOutDate)
                .specialCareNotes(notes)
                .status(BoardingTicket.BoardingStatus.ACTIVE)
                .build();
                
        return boardingTicketRepository.save(ticket);
    }

    @Transactional
    public BoardingTicket checkOut(Long ticketId) {
        BoardingTicket ticket = boardingTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vé lưu chuồng ID: " + ticketId));
                
        if (ticket.getStatus() == BoardingTicket.BoardingStatus.COMPLETED) {
            throw new IllegalStateException("Vé này đã hoàn tất check-out trước đó.");
        }
        
        // Cập nhật trạng thái vé và ngày giờ check-out thực tế
        ticket.setStatus(BoardingTicket.BoardingStatus.COMPLETED);
        ticket.setActualCheckOutDate(LocalDateTime.now());
        
        // Giải phóng chuồng
        Cage cage = ticket.getCage();
        cage.setStatus(Cage.CageStatus.AVAILABLE);
        cageRepository.save(cage);
        
        return boardingTicketRepository.save(ticket);
    }
}
