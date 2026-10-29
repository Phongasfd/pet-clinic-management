package com.petclinic.petservice.service;

import com.petclinic.petservice.dto.OwnerDto;
import com.petclinic.petservice.entity.Owner;
import com.petclinic.petservice.exception.DuplicateResourceException;
import com.petclinic.petservice.exception.ResourceNotFoundException;
import com.petclinic.petservice.outbox.OutboxService;
import com.petclinic.petservice.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final OutboxService outboxService;

    @Transactional
    public Owner createOwner(OwnerDto.CreateRequest request) {
        Owner owner = Owner.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .address(request.getAddress())
                .build();
        
        Owner savedOwner;
        try {
            savedOwner = ownerRepository.saveAndFlush(owner);
        } catch (DataIntegrityViolationException e) {
            // unique(phone_number) — other failures (DB down, ...) are not "duplicate"
            throw new DuplicateResourceException("Số điện thoại đã tồn tại trong hệ thống: " + request.getPhoneNumber());
        }

        // Billing account is created via the outbox, after this transaction commits.
        // Calling gRPC here created accounts for owners that were later rolled back,
        // and silently lost the account when billing-service was down.
        outboxService.enqueueBillingAccount(savedOwner);

        return savedOwner;
    }

    public Owner getOwnerById(Long id) {
        return ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chủ sở hữu với ID: " + id));
    }

    public Page<Owner> searchByPhone(String phone, Pageable pageable) {
        return ownerRepository.findByPhoneNumberContaining(phone, pageable);
    }
}
