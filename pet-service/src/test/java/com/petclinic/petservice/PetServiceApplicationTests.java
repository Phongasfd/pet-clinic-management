package com.petclinic.petservice;

import com.petclinic.petservice.kafka.PetEventPublisher;
import com.petclinic.petservice.service.BillingClientService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class PetServiceApplicationTests {

    @MockBean
    private PetEventPublisher petEventPublisher;

    @MockBean
    private BillingClientService billingClientService;

    @Test
    void contextLoads() {
    }

}
