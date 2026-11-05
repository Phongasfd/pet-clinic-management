package com.petclinic.petservice.service;

import com.petclinic.petservice.dto.OwnerDto;
import com.petclinic.petservice.entity.Owner;
import com.petclinic.petservice.exception.DuplicateResourceException;
import com.petclinic.petservice.outbox.OutboxService;
import com.petclinic.petservice.repository.OwnerRepository;
import com.petclinic.petservice.security.AccessGuard;
import com.petclinic.petservice.security.Role;
import com.petclinic.petservice.security.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
        ownerService = new OwnerService(ownerRepository, outboxService, new AccessGuard());
        when(ownerRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Owner owner = inv.getArgument(0);
            owner.setId(5L);
            return owner;
        });
        TestUsers.as(Role.RECEPTIONIST, 1L);
    }

    @AfterEach
    void clear() {
        TestUsers.clear();
    }

    private OwnerDto.CreateRequest request() {
        OwnerDto.CreateRequest request = new OwnerDto.CreateRequest();
        request.setFullName("Nguyen Van A");
        request.setPhoneNumber("0900000000");
        request.setEmail("a@example.com");
        return request;
    }

    private Owner savedOwner() {
        ArgumentCaptor<Owner> captor = ArgumentCaptor.forClass(Owner.class);
        verify(ownerRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    @Test
    void createOwner_queuesBillingAccountInsteadOfCallingGrpcInline() {
        OwnerDto.Response saved = ownerService.createOwner(request());

        verify(outboxService).enqueueBillingAccount(savedOwner());
        assertEquals(5L, saved.getId());
    }

    @Test
    void customerProfileIsAlwaysLinkedToTheirOwnAccount() {
        TestUsers.as(Role.CUSTOMER, 42L);
        OwnerDto.CreateRequest request = request();
        request.setUserId(999L); // trying to create a profile for someone else's account

        OwnerDto.Response saved = ownerService.createOwner(request);

        assertEquals(42L, saved.getUserId());
        assertEquals(42L, savedOwner().getUserId());
    }

    @Test
    void staffCanRegisterWalkInOwnerOrLinkAnAccount() {
        assertNull(ownerService.createOwner(request()).getUserId());

        OwnerDto.CreateRequest linked = request();
        linked.setPhoneNumber("0900000001");
        linked.setUserId(77L);
        assertEquals(77L, ownerService.createOwner(linked).getUserId());
    }

    @Test
    void oneOwnerProfilePerAccount() {
        TestUsers.as(Role.CUSTOMER, 42L);
        when(ownerRepository.existsByUserId(42L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> ownerService.createOwner(request()));
        verify(ownerRepository, never()).saveAndFlush(any());
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
