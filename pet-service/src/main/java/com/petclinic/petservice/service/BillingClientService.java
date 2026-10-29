package com.petclinic.petservice.service;

import com.petclinic.billing.grpc.BillingRequest;
import com.petclinic.billing.grpc.BillingResponse;
import com.petclinic.billing.grpc.BillingServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class BillingClientService {

    // Without a deadline a hung billing-service blocks the caller forever
    private static final long DEADLINE_SECONDS = 5;

    @GrpcClient("billing-service")
    private BillingServiceGrpc.BillingServiceBlockingStub billingStub;

    /** Called by OutboxRelay after the owner is committed; billing-service dedupes on customerId. */
    public BillingResponse createBillingAccount(BillingRequest request) {
        try {
            log.info("Calling billing-service to create account for customerId: {}", request.getCustomerId());
            return billingStub
                    .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .createBillingAccount(request);
        } catch (StatusRuntimeException e) {
            log.error("Failed to create billing account for customerId: {}, reason: {}",
                    request.getCustomerId(), e.getStatus());
            throw new RuntimeException("Billing Service is unavailable or failed to process request", e);
        }
    }
}
