package com.petclinic.petservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Component chịu trách nhiệm publish các sự kiện Pet lên Apache Kafka.
 *
 * <p>Payload là {@code PetEvent} đã được serialize bằng Protobuf (xem {@code OutboxService}),
 * đảm bảo định dạng nhị phân gọn nhẹ và schema được ràng buộc chặt chẽ
 * thông qua file hợp đồng {@code pet_events.proto} trong module {@code proto-common}.
 *
 * <p>Chỉ được gọi bởi {@code OutboxRelay} — sau khi transaction DB đã commit. Việc gửi là
 * đồng bộ (chờ broker ack) để relay biết chắc message đã lên Kafka trước khi đánh dấu
 * outbox row là đã xử lý; nếu lỗi thì exception được ném ra và relay sẽ retry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PetEventPublisher {

    private static final long SEND_TIMEOUT_SECONDS = 10;

    /**
     * KafkaTemplate được Spring Boot tự động cấu hình dựa trên {@code application.yml}.
     * Key là String (petId), Value là byte[] (Protobuf serialized).
     */
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    /** Tên topic đọc từ config, tránh hardcode để dễ thay đổi theo môi trường. */
    @Value("${app.kafka.topics.pet-events}")
    private String petEventsTopic;

    /**
     * Gửi một PetEvent đã serialize lên Kafka và chờ broker xác nhận (ack).
     *
     * <p>Message key là {@code petId}. Nhờ cơ chế partition-by-key của Kafka, tất cả message
     * của cùng một pet sẽ luôn nằm trên cùng một partition, đảm bảo thứ tự xử lý.
     *
     * @param messageKey petId dạng String
     * @param payload    PetEvent đã serialize bằng Protobuf
     * @throws Exception nếu broker không ack trong thời gian chờ — relay sẽ retry
     */
    public void publish(String messageKey, byte[] payload) throws Exception {
        SendResult<String, byte[]> result = kafkaTemplate.send(petEventsTopic, messageKey, payload)
                .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        log.info("Sent PetEvent key=[{}] with offset=[{}]", messageKey, result.getRecordMetadata().offset());
    }
}
