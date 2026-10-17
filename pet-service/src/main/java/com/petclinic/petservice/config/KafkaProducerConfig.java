package com.petclinic.petservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Cấu hình Kafka Producer cho Pet Service.
 *
 * <p>Các tham số kết nối cốt lõi (bootstrap-servers, serializer, acks, retries)
 * được Spring Boot tự động đọc từ {@code application.yml} (prefix {@code spring.kafka.producer}).
 * Class này chỉ chịu trách nhiệm khai báo thêm các Bean cần thiết như topic.
 */
@Configuration
public class KafkaProducerConfig {

    /**
     * Tên topic được inject từ property {@code app.kafka.topics.pet-events} trong application.yml.
     * Việc đặt tên topic vào config (thay vì hardcode) giúp dễ dàng thay đổi
     * giữa các môi trường (dev, staging, production).
     */
    @Value("${app.kafka.topics.pet-events}")
    private String petEventsTopic;

    /**
     * Tự động tạo Kafka Topic nếu chưa tồn tại trên broker.
     *
     * <p>Cấu hình:
     * <ul>
     *   <li><b>partitions = 3:</b> Cho phép 3 consumer trong cùng một group
     *       xử lý song song, tăng throughput.</li>
     *   <li><b>replicas = 1:</b> Phù hợp cho môi trường dev/single-broker.
     *       Tăng lên 3 cho môi trường production để đảm bảo fault-tolerance.</li>
     * </ul>
     *
     * @return Bean {@link NewTopic} được Spring Kafka sử dụng để tạo topic khi startup.
     */
    @Bean
    public NewTopic petEventsTopic() {
        return TopicBuilder.name(petEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
