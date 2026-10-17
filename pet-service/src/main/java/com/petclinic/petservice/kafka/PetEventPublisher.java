package com.petclinic.petservice.kafka;

import com.petclinic.events.PetEvent;
import com.petclinic.petservice.entity.Pet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Component chịu trách nhiệm publish các sự kiện Pet lên Apache Kafka.
 *
 * <p>Sử dụng Protobuf để serialize message thành {@code byte[]} trước khi gửi,
 * đảm bảo định dạng nhị phân gọn nhẹ và schema được ràng buộc chặt chẽ
 * thông qua file hợp đồng {@code pet_events.proto} trong module {@code proto-common}.
 *
 * <p>Message được gửi bất đồng bộ. Kết quả thành công/thất bại được ghi log
 * thông qua callback {@link CompletableFuture#whenComplete}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PetEventPublisher {

    /**
     * KafkaTemplate được Spring Boot tự động cấu hình dựa trên {@code application.yml}.
     * Key là String (petId), Value là byte[] (Protobuf serialized).
     */
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    /** Tên topic đọc từ config, tránh hardcode để dễ thay đổi theo môi trường. */
    @Value("${app.kafka.topics.pet-events}")
    private String petEventsTopic;

    /**
     * Tạo và gửi sự kiện {@code PET_CREATED} lên Kafka sau khi Pet được tạo thành công.
     *
     * <p>Message key được đặt là {@code petId} (dạng String). Nhờ cơ chế
     * partition-by-key của Kafka, tất cả message của cùng một pet sẽ luôn
     * nằm trên cùng một partition, đảm bảo thứ tự xử lý (ordering guarantee).
     *
     * @param pet Entity Pet vừa được lưu vào database.
     */
    public void publishPetCreatedEvent(Pet pet) {
        // Xây dựng đối tượng PetEvent từ Protobuf Builder
        PetEvent event = PetEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())   // ID duy nhất để idempotency check
                .setEventType("PET_CREATED")
                .setTimestamp(Instant.now().toEpochMilli()) // Epoch milliseconds (UTC)
                .setPetId(pet.getId())
                .setPetName(pet.getName())
                .setSpecies(pet.getSpecies().name())
                .setBreed(pet.getBreed() != null ? pet.getBreed() : "") // breed có thể null
                .setOwnerId(pet.getOwner().getId())
                // amount = 0.0 mặc định (không có doanh thu ở bước tạo pet)
                .build();

        // Serialize thành mảng byte nhị phân bằng Protobuf
        byte[] payload = event.toByteArray();

        // Dùng petId làm message key để đảm bảo ordering theo từng pet
        String messageKey = String.valueOf(pet.getId());

        // Gửi bất đồng bộ — không block luồng chính của PetService
        CompletableFuture<SendResult<String, byte[]>> future =
                kafkaTemplate.send(petEventsTopic, messageKey, payload);

        // Đăng ký callback để ghi log kết quả sau khi Kafka broker xác nhận (ack)
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent PetEvent=[{}] with offset=[{}]",
                        event.getEventId(), result.getRecordMetadata().offset());
            } else {
                // Ghi lỗi nhưng không throw — không muốn rollback transaction DB chỉ vì Kafka lỗi
                log.error("Unable to send PetEvent=[{}] due to: {}",
                        event.getEventId(), ex.getMessage());
            }
        });
    }
}
