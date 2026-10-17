package com.petclinic.analytics.controller;

import com.petclinic.analytics.entity.PetStatistic;
import com.petclinic.analytics.service.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final StatisticService statisticService;

    /**
     * GET /api/v1/analytics/summary
     * <p>Trả về báo cáo thống kê nhanh: số lượng pet theo từng loài.</p>
     *
     * @return Danh sách PetStatistic.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        List<PetStatistic> statistics = statisticService.getSummary();

        long totalPets = statistics.stream()
                .mapToLong(PetStatistic::getTotalCount)
                .sum();

        Map<String, Object> response = Map.of(
                "totalPets", totalPets,
                "breakdown", statistics
        );

        return ResponseEntity.ok(response);
    }
}
