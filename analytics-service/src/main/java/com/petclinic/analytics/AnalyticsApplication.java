package com.petclinic.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động của Analytics Service.
 *
 * <p>Service này chịu trách nhiệm:
 * <ul>
 *   <li>Consume sự kiện từ Apache Kafka topic <b>pet-events</b>.</li>
 *   <li>Phân tích và tổng hợp dữ liệu thống kê (số lượng pet theo loài, ...).</li>
 *   <li>Cung cấp API báo cáo thống kê qua REST endpoint {@code /api/v1/analytics}.</li>
 * </ul>
 */
@SpringBootApplication
public class AnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsApplication.class, args);
    }
}
