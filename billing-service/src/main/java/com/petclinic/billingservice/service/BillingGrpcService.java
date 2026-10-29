package com.petclinic.billingservice.service;

import com.petclinic.billingservice.entity.BillingAccount;
import com.petclinic.billingservice.repository.BillingAccountRepository;
import com.petclinic.billing.grpc.BillingRequest;
import com.petclinic.billing.grpc.BillingResponse;
import com.petclinic.billing.grpc.BillingServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import java.math.BigDecimal;

@GrpcService
public class BillingGrpcService extends BillingServiceGrpc.BillingServiceImplBase {

    private final BillingAccountRepository billingAccountRepository;

    @Autowired
    public BillingGrpcService(BillingAccountRepository billingAccountRepository) {
        this.billingAccountRepository = billingAccountRepository;
    }

    /**
     * Idempotent: pet-service delivers this call at-least-once (outbox relay retries),
     * so a second request for the same customer returns the existing account.
     */
    @Override
    public void createBillingAccount(BillingRequest request, StreamObserver<BillingResponse> responseObserver) {
        Long customerId;
        try {
            customerId = Long.parseLong(request.getCustomerId());
        } catch (NumberFormatException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("customer_id must be numeric")
                    .asRuntimeException());
            return;
        }

        try {
            BillingAccount existing = billingAccountRepository.findByCustomerId(customerId).orElse(null);
            if (existing != null) {
                reply(responseObserver, existing, "Billing account already exists");
                return;
            }

            BillingAccount savedAccount;
            try {
                savedAccount = billingAccountRepository.save(BillingAccount.builder()
                        .customerId(customerId)
                        .balance(BigDecimal.ZERO)
                        .status(BillingAccount.Status.ACTIVE)
                        .build());
            } catch (DataIntegrityViolationException race) {
                // a concurrent retry inserted it first (unique customer_id)
                savedAccount = billingAccountRepository.findByCustomerId(customerId).orElseThrow(() -> race);
                reply(responseObserver, savedAccount, "Billing account already exists");
                return;
            }

            reply(responseObserver, savedAccount, "Billing account created successfully");

        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to create billing account: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

    private void reply(StreamObserver<BillingResponse> responseObserver, BillingAccount account, String message) {
        responseObserver.onNext(BillingResponse.newBuilder()
                .setAccountId(String.valueOf(account.getId()))
                .setStatus(account.getStatus().name())
                .setMessage(message)
                .build());
        responseObserver.onCompleted();
    }
}
