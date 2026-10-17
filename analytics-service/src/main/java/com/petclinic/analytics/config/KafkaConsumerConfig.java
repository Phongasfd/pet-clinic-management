package com.petclinic.analytics.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Cấu hình Kafka Consumer cho Analytics Service.
 *
 * <p>Class này thiết lập cơ chế xử lý lỗi khi consume message thất bại, bao gồm:
 * <ul>
 *   <li><b>Retry:</b> Thử lại tối đa 3 lần, mỗi lần cách nhau 1 giây.</li>
 *   <li><b>Dead Letter Topic (DLT):</b> Nếu vẫn thất bại sau 3 lần retry,
 *       message sẽ bị chuyển sang topic <b>{tên_topic}.DLT</b> để lưu trữ
 *       và debug sau mà không làm gián đoạn luồng chính.</li>
 * </ul>
 *
 * <p>Các cấu hình cơ bản (bootstrap-servers, group-id, deserializer) được
 * Spring Boot tự động đọc từ {@code application.yml}.
 */
@EnableKafka       // Kích hoạt xử lý annotation @KafkaListener trong toàn ứng dụng
@Configuration
public class KafkaConsumerConfig {

    /**
     * Bean xử lý lỗi mặc định cho tất cả KafkaListener trong ứng dụng.
     *
     * @param template KafkaOperations dùng để publish message lỗi sang DLT.
     * @return {@link CommonErrorHandler} đã cấu hình retry + DLT.
     */
    @Bean
    public CommonErrorHandler errorHandler(KafkaOperations<Object, Object> template) {
        // DeadLetterPublishingRecoverer: Sau khi hết số lần retry, chuyển message lỗi
        // sang topic "{tên_gốc}.DLT" và giữ nguyên partition tương ứng để dễ trace.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (r, e) -> new TopicPartition(r.topic() + ".DLT", r.partition()));

        // FixedBackOff(interval, maxAttempts):
        //   - 1000L  = delay 1 giây giữa mỗi lần retry
        //   - 3L     = tối đa 3 lần retry trước khi chuyển sang DLT
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }
}
