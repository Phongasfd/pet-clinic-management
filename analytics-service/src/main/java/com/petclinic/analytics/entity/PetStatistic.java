package com.petclinic.analytics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity lưu thống kê số lượng Pet theo species.
 */
@Entity
@Table(name = "pet_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Loài vật (DOG, CAT, OTHER).
     */
    @Column(nullable = false, unique = true)
    private String species;

    /**
     * Tổng số pet thuộc loài này đã được tạo.
     */
    @Column(nullable = false)
    private Long totalCount;

    /**
     * Thời điểm cập nhật lần cuối.
     */
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
