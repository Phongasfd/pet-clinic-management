package com.petclinic.billingservice.service;

import com.petclinic.billingservice.entity.BillingAccount;
import com.petclinic.billingservice.repository.BillingAccountRepository;
import com.petclinic.proto.billing.BillingRequest;
import com.petclinic.proto.billing.BillingResponse;
import com.petclinic.proto.billing.BillingServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;

@GrpcService
public class BillingGrpcService extends BillingServiceGrpc.BillingServiceImplBase {

    private final BillingAccountRepository billingAccountRepository;

    @Autowired
    public BillingGrpcService(BillingAccountRepository billingAccountRepository) {
        this.billingAccountRepository = billingAccountRepository;
    }

    @Override
    public void createBillingAccount(BillingRequest request, StreamObserver<BillingResponse> responseObserver) {
        try {
            BillingAccount newAccount = BillingAccount.builder()
                    .customerId(request.getCustomerId())
                    .balance(BigDecimal.ZERO)
                    .status(BillingAccount.Status.ACTIVE)
                    .build();

            BillingAccount savedAccount = billingAccountRepository.save(newAccount);

            BillingResponse response = BillingResponse.newBuilder()
                    .setAccountId(savedAccount.getId())
                    .setCustomerId(savedAccount.getCustomerId())
                    .setStatus("ACTIVE")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to create billing account: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }
}
