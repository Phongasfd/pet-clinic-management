package com.petclinic.billingservice.repository;

import com.petclinic.billingservice.entity.BillingAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingAccountRepository extends JpaRepository<BillingAccount, Long> {
}
