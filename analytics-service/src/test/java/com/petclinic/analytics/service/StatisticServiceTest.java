package com.petclinic.analytics.service;

import com.petclinic.analytics.entity.PetStatistic;
import com.petclinic.analytics.repository.PetStatisticRepository;
import com.petclinic.analytics.repository.ProcessedEventRepository;
import com.petclinic.events.PetEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StatisticServiceTest {

    private PetStatisticRepository statisticRepository;
    private ProcessedEventRepository processedEventRepository;
    private StatisticService service;

    private final PetEvent event = PetEvent.newBuilder()
            .setEventId("evt-1").setEventType("PET_CREATED").setSpecies("DOG").build();

    @BeforeEach
    void setUp() {
        statisticRepository = mock(PetStatisticRepository.class);
        processedEventRepository = mock(ProcessedEventRepository.class);
        service = new StatisticService(statisticRepository, processedEventRepository);
    }

    @Test
    void countsANewEventOnce() {
        when(processedEventRepository.existsById("evt-1")).thenReturn(false);
        when(statisticRepository.findBySpecies("DOG")).thenReturn(Optional.empty());

        service.handlePetCreatedEvent(event);

        ArgumentCaptor<PetStatistic> saved = ArgumentCaptor.forClass(PetStatistic.class);
        verify(statisticRepository).save(saved.capture());
        assertEquals(1L, saved.getValue().getTotalCount());
        verify(processedEventRepository).save(any());
    }

    @Test
    void redeliveredEventIsIgnored() {
        when(processedEventRepository.existsById("evt-1")).thenReturn(true);

        service.handlePetCreatedEvent(event);

        verify(statisticRepository, never()).save(any());
    }
}
