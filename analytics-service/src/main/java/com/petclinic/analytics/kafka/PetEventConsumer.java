package com.petclinic.analytics.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.petclinic.analytics.service.StatisticService;
import com.petclinic.events.PetEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer chịu trách nhiệm lắng nghe và xử lý các sự kiện Pet.
 *
 * <p>Luồng xử lý:
 * <ol>
 *   <li>Nhận raw {@code byte[]} payload từ Kafka topic.</li>
 *   <li>Deserialize payload thành đối tượng {@link PetEvent} bằng Protobuf.</li>
 *   <li>Phân loại sự kiện theo {@code eventType} và điều hướng tới service xử lý.</li>
 * </ol>
 *
 * <p>Nếu Protobuf không thể parse payload (dữ liệu hỏng), một
 * {@link RuntimeException} sẽ được ném ra để {@code DefaultErrorHandler}
 * bắt, thực hiện retry và cuối cùng chuyển sang Dead Letter Topic (DLT).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PetEventConsumer {

    private final StatisticService statisticService;

    /**
     * Lắng nghe message từ Kafka topic {@code pet-events}.
     *
     * <p>Sử dụng {@code ConsumerRecord} thay vì nhận thẳng {@code byte[]}
     * để có thể truy cập metadata (partition, offset, key) phục vụ việc debug và log.
     *
     * @param record Bản ghi Kafka thô, bao gồm key (petId) và value (Protobuf bytes).
     */
    @KafkaListener(topics = "${app.kafka.topics.pet-events}", groupId = "analytics-group")
    public void consumePetEvent(ConsumerRecord<String, byte[]> record) {
        // Ghi log metadata để hỗ trợ trace message trong hệ thống phân tán
        log.info("Received Kafka record: topic=[{}], partition=[{}], offset=[{}], key=[{}]",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            // Deserialize mảng byte thành đối tượng PetEvent sử dụng Protobuf
            PetEvent event = PetEvent.parseFrom(record.value());

            log.info("Parsed PetEvent: id=[{}], type=[{}], petName=[{}], ownerId=[{}]",
                    event.getEventId(), event.getEventType(), event.getPetName(), event.getOwnerId());

            // Phân loại và xử lý theo từng loại sự kiện
            if ("PET_CREATED".equals(event.getEventType())) {
                // Cập nhật thống kê số lượng pet mới tạo theo loài
                statisticService.handlePetCreatedEvent(event);
            } else {
                // Các event type chưa được hỗ trợ sẽ bị bỏ qua và ghi cảnh báo
                log.warn("Unhandled event type: [{}] — skipping", event.getEventType());
            }

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse PetEvent from bytes. Record key=[{}], error=[{}]",
                    record.key(), e.getMessage());
            // Ném RuntimeException để DefaultErrorHandler bắt và thực hiện retry.
            // Sau khi hết retry, message sẽ được chuyển sang Dead Letter Topic (pet-events.DLT).
            throw new RuntimeException("Invalid Protobuf payload for key=" + record.key(), e);
        }
    }
}
