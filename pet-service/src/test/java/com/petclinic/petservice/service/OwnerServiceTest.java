package com.petclinic.petservice.service;

import com.petclinic.petservice.dto.OwnerDto;
import com.petclinic.petservice.entity.Owner;
import com.petclinic.petservice.exception.DuplicateResourceException;
import com.petclinic.petservice.outbox.OutboxService;
import com.petclinic.petservice.repository.OwnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OwnerServiceTest {

    private OwnerRepository ownerRepository;
    private OutboxService outboxService;
    private OwnerService ownerService;

    @BeforeEach
    void setUp() {
        ownerRepository = mock(OwnerRepository.class);
        outboxService = mock(OutboxService.class);
        ownerService = new OwnerService(ownerRepository, outboxService);
    }

    private OwnerDto.CreateRequest request() {
        OwnerDto.CreateRequest request = new OwnerDto.CreateRequest();
        request.setFullName("Nguyen Van A");
        request.setPhoneNumber("0900000000");
        request.setEmail("a@example.com");
        return request;
    }

    @Test
    void createOwner_queuesBillingAccountInsteadOfCallingGrpcInline() {
        when(ownerRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Owner owner = inv.getArgument(0);
            owner.setId(5L);
            return owner;
        });

        Owner saved = ownerService.createOwner(request());

        verify(outboxService).enqueueBillingAccount(saved);
        assertEquals(5L, saved.getId());
    }

    @Test
    void createOwner_duplicatePhone() {
        when(ownerRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk_phone"));

        assertThrows(DuplicateResourceException.class, () -> ownerService.createOwner(request()));
        verifyNoInteractions(outboxService);
    }

    @Test
    void createOwner_otherDbErrorsAreNotReportedAsDuplicate() {
        when(ownerRepository.saveAndFlush(any())).thenThrow(new IllegalStateException("connection refused"));

        assertThrows(IllegalStateException.class, () -> ownerService.createOwner(request()));
    }
}
