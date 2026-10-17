package com.petclinic.analytics.service;

import com.petclinic.analytics.entity.PetStatistic;
import com.petclinic.analytics.repository.PetStatisticRepository;
import com.petclinic.events.PetEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticService {

    private final PetStatisticRepository petStatisticRepository;

    /**
     * Xử lý sự kiện PET_CREATED và cập nhật số liệu thống kê theo species.
     *
     * @param event Đối tượng PetEvent được parse từ Protobuf payload.
     */
    @Transactional
    public void handlePetCreatedEvent(PetEvent event) {
        String species = event.getSpecies();

        PetStatistic statistic = petStatisticRepository.findBySpecies(species)
                .orElseGet(() -> PetStatistic.builder()
                        .species(species)
                        .totalCount(0L)
                        .build());

        statistic.setTotalCount(statistic.getTotalCount() + 1);
        statistic.setLastUpdated(LocalDateTime.now());

        petStatisticRepository.save(statistic);

        log.info("Updated statistics for species=[{}], new totalCount=[{}]", species, statistic.getTotalCount());
    }

    /**
     * Trả về toàn bộ báo cáo thống kê.
     *
     * @return Danh sách thống kê theo từng loài.
     */
    public List<PetStatistic> getSummary() {
        return petStatisticRepository.findAll();
    }
}
