package com.petclinic.petservice.service;

import com.petclinic.proto.billing.BillingRequest;
import com.petclinic.proto.billing.BillingResponse;
import com.petclinic.proto.billing.BillingServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BillingClientService {

    @GrpcClient("billing-service")
    private BillingServiceGrpc.BillingServiceBlockingStub billingStub;

    public BillingResponse createBillingAccount(Long ownerId, String name, String email) {
        try {
            log.info("Calling billing-service to create account for ownerId: {}", ownerId);
            BillingRequest request = BillingRequest.newBuilder()
                    .setCustomerId(ownerId)
                    .build();
            return billingStub.createBillingAccount(request);
        } catch (StatusRuntimeException e) {
            log.error("Failed to create billing account for ownerId: {}, reason: {}", ownerId, e.getStatus().getDescription());
            throw new RuntimeException("Billing Service is unavailable or failed to process request", e);
        }
    }
}
