package com.petclinic.petservice.service;

import com.petclinic.petservice.dto.OwnerDto;
import com.petclinic.petservice.entity.Owner;
import com.petclinic.petservice.exception.DuplicateResourceException;
import com.petclinic.petservice.exception.ResourceNotFoundException;
import com.petclinic.petservice.outbox.OutboxService;
import com.petclinic.petservice.repository.OwnerRepository;
import com.petclinic.petservice.security.AccessGuard;
import com.petclinic.petservice.security.CurrentUser;
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
    private final AccessGuard accessGuard;

    /**
     * A customer creates their own profile (always linked to their account, one per account).
     * Staff register walk-in owners and may link them to an existing customer account.
     */
    @Transactional
    public OwnerDto.Response createOwner(OwnerDto.CreateRequest request) {
        CurrentUser user = accessGuard.currentUser();
        Long linkedUserId = user.isStaff() ? request.getUserId() : user.userId();

        if (linkedUserId != null && ownerRepository.existsByUserId(linkedUserId)) {
            throw new DuplicateResourceException("Account " + linkedUserId + " already has an owner profile");
        }

        Owner owner = Owner.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .address(request.getAddress())
                .userId(linkedUserId)
                .build();

        Owner savedOwner;
        try {
            savedOwner = ownerRepository.saveAndFlush(owner);
        } catch (DataIntegrityViolationException e) {
            // unique(phone_number), or unique(user_id) lost to a concurrent request.
            // Other failures (DB down, ...) are not "duplicate" and propagate as they are.
            throw new DuplicateResourceException(
                    "An owner with phone number " + request.getPhoneNumber() + " or this account already exists");
        }

        // Billing account is created via the outbox, after this transaction commits.
        // Calling gRPC here created accounts for owners that were later rolled back,
        // and silently lost the account when billing-service was down.
        outboxService.enqueueBillingAccount(savedOwner);

        return OwnerDto.Response.from(savedOwner);
    }

    @Transactional(readOnly = true)
    public OwnerDto.Response getOwnerById(Long id) {
        Owner owner = ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + id));
        accessGuard.checkOwnerAccess(owner);
        return OwnerDto.Response.from(owner);
    }

    /** The owner profile linked to the caller's account. */
    @Transactional(readOnly = true)
    public OwnerDto.Response getMyOwner() {
        Long userId = accessGuard.currentUser().userId();
        return ownerRepository.findByUserId(userId)
                .map(OwnerDto.Response::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No owner profile for this account yet; create one with POST /api/v1/owners"));
    }

    /** Staff only (enforced on the controller): phone search would leak other customers' data. */
    @Transactional(readOnly = true)
    public Page<OwnerDto.Response> searchByPhone(String phone, Pageable pageable) {
        return ownerRepository.findByPhoneNumberContaining(phone, pageable).map(OwnerDto.Response::from);
    }
}
