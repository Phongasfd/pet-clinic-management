package com.petclinic.billingservice.service;

import com.petclinic.billing.grpc.BillingRequest;
import com.petclinic.billing.grpc.BillingResponse;
import com.petclinic.billingservice.entity.BillingAccount;
import com.petclinic.billingservice.repository.BillingAccountRepository;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BillingGrpcServiceTest {

    private BillingAccountRepository repository;
    private BillingGrpcService service;

    @BeforeEach
    void setUp() {
        repository = mock(BillingAccountRepository.class);
        service = new BillingGrpcService(repository);
    }

    private static class Recorder implements StreamObserver<BillingResponse> {
        final List<BillingResponse> responses = new ArrayList<>();
        Throwable error;

        @Override public void onNext(BillingResponse value) { responses.add(value); }
        @Override public void onError(Throwable t) { error = t; }
        @Override public void onCompleted() { }
    }

    private BillingAccount account(long id) {
        return BillingAccount.builder().id(id).customerId(5L).balance(BigDecimal.ZERO)
                .status(BillingAccount.Status.ACTIVE).build();
    }

    private Recorder call(String customerId) {
        Recorder recorder = new Recorder();
        service.createBillingAccount(BillingRequest.newBuilder().setCustomerId(customerId).build(), recorder);
        return recorder;
    }

    @Test
    void createsAccountForNewCustomer() {
        when(repository.findByCustomerId(5L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(account(100L));

        Recorder r = call("5");

        assertEquals("100", r.responses.get(0).getAccountId());
    }

    @Test
    void retryReturnsExistingAccountInsteadOfCreatingASecondOne() {
        when(repository.findByCustomerId(5L)).thenReturn(Optional.of(account(100L)));

        Recorder r = call("5");

        assertEquals("100", r.responses.get(0).getAccountId());
        verify(repository, never()).save(any());
    }

    @Test
    void concurrentInsertResolvesToTheWinner() {
        when(repository.findByCustomerId(5L)).thenReturn(Optional.empty(), Optional.of(account(100L)));
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("uk_customer"));

        Recorder r = call("5");

        assertNull(r.error);
        assertEquals("100", r.responses.get(0).getAccountId());
    }

    @Test
    void nonNumericCustomerIdIsInvalidArgument() {
        Recorder r = call("abc");

        assertInstanceOf(StatusRuntimeException.class, r.error);
        assertEquals(io.grpc.Status.Code.INVALID_ARGUMENT, ((StatusRuntimeException) r.error).getStatus().getCode());
    }
}
