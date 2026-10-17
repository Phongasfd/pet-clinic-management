package com.petclinic.petservice.service;

import com.petclinic.petservice.dto.OwnerDto;
import com.petclinic.petservice.entity.Owner;
import com.petclinic.petservice.exception.DuplicateResourceException;
import com.petclinic.petservice.exception.ResourceNotFoundException;
import com.petclinic.petservice.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final BillingClientService billingClientService;

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
            savedOwner = ownerRepository.save(owner);
        } catch (Exception e) {
            // Có thể bắt DataIntegrityViolationException cụ thể hơn, nhưng tạm dùng Exception chung cho unique constraint
            throw new DuplicateResourceException("Số điện thoại đã tồn tại trong hệ thống: " + request.getPhoneNumber());
        }

        try {
            billingClientService.createBillingAccount(savedOwner.getId(), savedOwner.getFullName(), savedOwner.getEmail());
        } catch (Exception e) {
            log.warn("Không thể tạo tài khoản billing cho owner {}: {}. Sẽ không rollback.", savedOwner.getId(), e.getMessage());
            // Quyết định: ghi nhận log cảnh báo và không rollback Owner creation
        }
        
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
