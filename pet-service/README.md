# 🐾 Pet Service - Dịch Vụ Quản Lý Thú Cưng & Phòng Khám

`pet-service` là microservice trung tâm (Core Domain Service) trong hệ sinh thái **Pet Clinic Management System**. Service này chịu trách nhiệm quản lý toàn bộ nghiệp vụ cốt lõi liên quan đến chủ sở hữu, thú cưng, hồ sơ y tế, tiêm phòng, dịch vụ spa - làm đẹp, lưu chuồng (khách sạn thú cưng) và lịch hẹn thăm khám.

---

## 📌 Mục Lục
1. [Tổng Quan Kiến Trúc & Công Nghệ](#-tổng-quan-kiến-trúc--công-nghệ)
2. [Các Tính Năng & Phân Hệ Nghiệp Vụ](#-các-tính-năng--phân-hệ-nghiệp-vụ)
   - [1. Quản lý Chủ sở hữu (Owner Management)](#1-quản-lý-chủ-sở-hữu-owner-management)
   - [2. Quản lý Thú cưng (Pet Management)](#2-quản-lý-thú-cưng-pet-management)
   - [3. Hồ sơ Bệnh án & Tiêm chủng (Medical & Vaccination)](#3-hồ-sơ-bệnh-án--tiêm-chủng-medical--vaccination)
   - [4. Dịch vụ Khách sạn thú cưng (Boarding Service)](#4-dịch-vụ-khách-sạn-thú-cưng-boarding-service)
   - [5. Dịch vụ Spa & Làm đẹp (Spa / Grooming Service)](#5-dịch-vụ-spa--làm-đẹp-spa--grooming-service)
   - [6. Quản lý Lịch hẹn (Appointment Service)](#6-quản-lý-lịch-hẹn-appointment-service)
3. [Tích Hợp Liên Dịch Vụ (Inter-Service Integration)](#-tích-hợp-liên-dịch-vụ-inter-service-integration)
   - [gRPC Client -> Billing Service](#-grpc-client---billing-service)
   - [Kafka Event Publisher -> Analytics Service](#-kafka-event-publisher---analytics-service)
   - [Context Propagation & Security](#-context-propagation--security)
4. [Mô Hình Cơ Sở Dữ Liệu (Database Schema)](#-mô-hình-cơ-sở-dữ-liệu-database-schema)
5. [Tài Liệu Chi Tiết REST API](#-tài-liệu-chi-tiết-rest-api)
6. [Xử Lý Lỗi (Error Handling)](#-xử-lý-lỗi-error-handling)
7. [Hướng Dẫn Cấu Hình & Khởi Chạy](#-hướng-dẫn-cấu-hình--khởi-chạy)

---

## 🏛 Tổng Quan Kiến Trúc & Công Nghệ

- **Ngôn ngữ & Framework**: Java 17+, Spring Boot 3.x (Spring Web, Spring Data JPA, Spring Validation).
- **Cơ sở dữ liệu**: PostgreSQL 15, quản lý schema versioning bằng **Flyway**.
- **Giao tiếp đồng bộ (Synchronous)**: RESTful APIs (HTTP/JSON) kết hợp **gRPC** client giao tiếp nội bộ với `billing-service`.
- **Giao tiếp bất đồng bộ (Asynchronous / Event-driven)**: **Apache Kafka** (chế độ KRaft), tuần tự hóa message bằng **Google Protocol Buffers (Protobuf)** từ module `proto-common`.
- **Tài liệu API**: OpenAPI 3.0 / Swagger UI.
- **Port dịch vụ**: `8082` (Container) / Được định tuyến thông qua `api-gateway` (Port `8080`).

---

## 🚀 Các Tính Năng & Phân Hệ Nghiệp Vụ

### 1. Quản lý Chủ sở hữu (Owner Management)
- **Tạo mới hồ sơ khách hàng**: Đăng ký thông tin chủ thú cưng gồm tên, số điện thoại (ràng buộc duy nhất), email và địa chỉ. Khách tự tạo thì hồ sơ được gắn với tài khoản của họ (`owners.user_id`, mỗi tài khoản một hồ sơ); nhân viên tạo hộ khách vãng lai và có thể gắn tài khoản.
- **Tạo tài khoản Billing qua outbox**: Cùng transaction với chủ nuôi, service ghi một dòng `outbox_events` loại `BILLING_ACCOUNT_REQUESTED`. Sau khi commit, `OutboxRelay` gọi gRPC `CreateBillingAccount` sang `billing-service`; client nhận `201` ngay, không chờ billing.
- **Tìm kiếm & Tra cứu**:
  - `GET /owners/me`: hồ sơ của khách đang đăng nhập.
  - Tìm kiếm theo số điện thoại có phân trang (`Pageable`), chỉ nhân viên.
  - Lấy chi tiết chủ nuôi theo `ownerId` (khách chỉ xem được hồ sơ của mình).

### 2. Quản lý Thú cưng (Pet Management)
- **Đăng ký hồ sơ thú cưng**: Khởi tạo thông tin thú cưng liên kết trực tiếp với chủ nuôi (`ownerId`):
  - Chủng loại (`Species`): `DOG`, `CAT`, `OTHER`.
  - Giới tính (`Gender`): `MALE`, `FEMALE`, `UNKNOWN`.
  - Giống loài (`breed`), ngày sinh (`dateOfBirth`), trạng thái triệt sản (`isNeutered`), cân nặng (`weight`), tiền sử dị ứng (`allergies`) và ghi chú đặc biệt.
- **Phát sự kiện Kafka qua outbox**: Cùng transaction với thú cưng, service ghi sự kiện `PET_CREATED` (Protobuf, `event_id` sinh một lần) vào `outbox_events`; `OutboxRelay` gửi lên topic `pet-events` sau khi commit (message key = `petId` để giữ thứ tự theo từng thú cưng).
- **Cập nhật nhanh thông tin y tế**: Cho phép cập nhật linh hoạt cân nặng định kỳ và thông tin dị ứng mới phát hiện.
- **Truy vấn thú cưng theo chủ**: Liệt kê danh sách tất cả thú cưng của một chủ nuôi cụ thể.

### 3. Hồ sơ Bệnh án & Tiêm chủng (Medical & Vaccination)
- **Hồ sơ bệnh án (`MedicalRecord`)**:
  - Ghi nhận chi tiết từng lần khám bệnh: lý do khám (`reason`), triệu chứng lâm sàng (`symptoms`), chẩn đoán của bác sĩ thú y (`diagnosis`), hướng điều trị / đơn thuốc (`treatment`).
  - Hẹn ngày tái khám (`reVisitDate`).
- **Lịch sử & Kế hoạch tiêm chủng (`Vaccination`)**:
  - Lưu trữ nhật ký tiêm phòng: tên vắc-xin (`vaccineName`), ngày tiêm thực tế (`administeredDate`).
  - Ghi nhận ngày cần tiêm nhắc lại mũi tiếp theo (`nextDueDate`) để phục vụ thông báo và theo dõi định kỳ.

### 4. Dịch vụ Khách sạn thú cưng (Boarding Service)
- **Quản lý lồng/chuồng lưu trú (`Cage`)**:
  - Phân loại chuồng: `STANDARD`, `VIP`, `CAT_ROOM`.
  - Giá thuê theo ngày (`daily_rate`).
  - Quản lý trạng thái lồng: `AVAILABLE` (sẵn sàng), `OCCUPIED` (đang có thú cưng ở), `MAINTENANCE` (bảo trì/vệ sinh).
- **Quy trình Check-in**:
  - Kiểm tra điều kiện phòng trống.
  - Tự động đổi trạng thái chuồng sang `OCCUPIED`.
  - Tạo vé lưu chuồng (`BoardingTicket`) với trạng thái `ACTIVE`, lưu ngày check-in, dự kiến check-out (`expectedCheckOutDate`) và các hướng dẫn chăm sóc đặc biệt (`specialCareNotes`).
- **Quy trình Check-out**:
  - Đánh dấu hoàn tất vé lưu chuồng (`COMPLETED`), ghi nhận ngày giờ check-out thực tế (`actualCheckOutDate`).
  - Tự động giải phóng chuồng về trạng thái `AVAILABLE`.

### 5. Dịch vụ Spa & Làm đẹp (Spa / Grooming Service)
- **Tạo vé dịch vụ Spa (`SpaTicket`)**:
  - Lưu danh sách dịch vụ yêu cầu dưới dạng JSONB linh hoạt (`services`: tắm, sấy, cắt tỉa lông, vệ sinh tai, cắt móng...).
  - Ghi nhận tình trạng da/lông ban đầu (`coatCondition`), tên chuyên viên phục vụ (`groomerName`), và chi phí ước tính (`estimatedPrice`).
- **Theo dõi tiến độ dịch vụ**:
  - Vòng đời trạng thái phiếu Spa: `RECEIVED` (Tiếp nhận thú cưng) ➔ `IN_PROGRESS` (Đang thực hiện) ➔ `COMPLETED` (Hoàn tất dịch vụ).

### 6. Quản lý Lịch hẹn (Appointment Service)
- **Đặt lịch hẹn trước (`Appointment`)**:
  - Đặt lịch cho thú cưng theo loại hình dịch vụ:
    - `VET`: Khám chữa bệnh / tiêm phòng thú y.
    - `SPA`: Dịch vụ chăm sóc lông, tắm rửa, vệ sinh.
    - `HOTEL`: Đặt giữ chuồng lưu trú.
  - Thiết lập ngày giờ hẹn (`appointmentDate`) và ghi chú từ khách hàng.
- **Quản lý trạng thái lịch hẹn**:
  - `PENDING` (Chờ tiếp nhận) ➔ `CONFIRMED` (Đã xác nhận) ➔ `COMPLETED` (Hoàn thành) / `CANCELLED` (Đã hủy).

---

## 🔗 Tích Hợp Liên Dịch Vụ (Inter-Service Integration)

```
                            ┌────────────────┐
                            │  api-gateway   │ (Port 8080)
                            └───────┬────────┘
                                    │ HTTP Requests (Headers: X-User-Id, X-User-Role)
                                    ▼
                          ┌────────────────────┐
                          │    pet-service     │ (Port 8082)
                          └─────┬────────┬─────┘
                                │        │
                gRPC (Port 9090)│        │ Kafka (Port 9092)
     CreateBillingAccount(...)  │        │ Topic: pet-events (Protobuf)
                                ▼        ▼
                     ┌────────────────┐ ┌────────────────────┐
                     │billing-service │ │ analytics-service  │
                     └────────────────┘ └────────────────────┘
```

### 🔹 gRPC Client -> Billing Service
- Contract `billing_service.proto` trong `proto-common`.
- Không gọi trong request của người dùng: `OutboxRelay` (`@Scheduled` mỗi 1 giây) lấy các dòng `outbox_events` chưa xử lý bằng `SELECT ... FOR UPDATE SKIP LOCKED` (an toàn khi chạy nhiều instance) rồi gọi `BillingClientService.createBillingAccount`:
  - Target: `static://billing-service:9090` (môi trường container) hoặc cấu hình qua `GRPC_CLIENT_BILLING-SERVICE_ADDRESS`.
  - Deadline 5 giây: billing treo không giữ thread mãi.
  - **Khi billing lỗi**: dòng outbox giữ nguyên, `attempts++`, lưu `last_error`, thử lại với exponential backoff (2 giây, 4 giây, 8 giây… tối đa 5 phút); sau 10 lần thì dừng để người vận hành xem. Chủ nuôi đã được tạo từ trước nên người dùng không bị ảnh hưởng.
  - Giao ít nhất một lần, nên `billing-service` idempotent theo `customer_id`.

### 🔹 Kafka Event Publisher -> Analytics Service
- Cấu hình Producer qua `KafkaProducerConfig` (topic `pet-events` 3 partition) và `PetEventPublisher`.
- `OutboxRelay` gửi sự kiện và chờ broker xác nhận (`acks: all`, tối đa 10 giây) rồi mới đánh dấu dòng outbox đã xử lý; lỗi thì thử lại theo backoff như trên.
- **Protobuf Event Payload (`PetEvent`)**:
  - `eventId`: UUID duy nhất để hỗ trợ kiểm tra tính idempotent phía Consumer.
  - `eventType`: `"PET_CREATED"`.
  - `timestamp`: Epoch millisecond UTC.
  - `petId`, `petName`, `species`, `breed`, `ownerId`.
  - Key message là `String.valueOf(petId)` giúp đảm bảo các sự kiện của cùng 1 thú cưng luôn vào cùng Kafka partition.

### 🔹 Context Propagation & Security
- `GatewayHeaderAuthenticationFilter` (Spring Security) đọc các header Gateway đã gắn sau khi xác thực JWT:
  - `X-User-Id`
  - `X-User-Name`
  - `X-User-Role`
- Thiếu header hoặc header sai định dạng → `401`. SecurityContext được xoá sau mỗi request.
- **Phân quyền theo role** bằng `@PreAuthorize`: `@StaffOnly` (ADMIN, VET, RECEPTIONIST) cho boarding, spa, tìm owner theo SĐT, sửa health-info; `@ClinicalStaffOnly` (ADMIN, VET) cho bệnh án và tiêm chủng.
- **Phân quyền theo quyền sở hữu** bằng `AccessGuard`: CUSTOMER chỉ truy cập owner profile gắn với tài khoản của mình (`owners.user_id`) cùng pet và lịch hẹn của owner đó; truy cập dữ liệu người khác → `403`.
- Ma trận đầy đủ: xem mục *Authorization* trong README ở thư mục gốc.

### 🔹 Chống double-booking
- Check-in chuồng khoá dòng `cages` bằng `SELECT ... FOR UPDATE` trong suốt lúc kiểm tra và đổi trạng thái.
- Migration V4 thêm partial unique index: mỗi chuồng tối đa 1 lượt lưu `ACTIVE`, mỗi pet tối đa 1 chuồng `ACTIVE`. Trước khi tạo index, V4 đóng các lượt `ACTIVE` trùng mà code cũ để lại (giữ lượt mới nhất của mỗi pet và mỗi chuồng, trả chuồng trống về `AVAILABLE`); không có bước này, một dòng trùng làm Flyway fail và service không khởi động được.
- Đặt lịch hẹn khoá dòng `pets`, từ chối lịch hẹn thứ hai còn hiệu lực trong vòng 30 phút.
- Trạng thái lịch hẹn và vé spa là state machine; chuyển trạng thái sai → `409`.

---

## 🗄 Mô Hình Cơ Sở Dữ Liệu (Database Schema)

PostgreSQL schema do Flyway quản lý (`ddl-auto: validate` kiểm entity khớp schema lúc khởi động):

| Migration | Nội dung |
| :--- | :--- |
| `V1__init_schema.sql` | Các bảng nghiệp vụ bên dưới |
| `V2__seed_sample_data.sql` | Dữ liệu mẫu: 3 chủ nuôi, 5 thú cưng, 5 chuồng, lịch hẹn và vé spa |
| `V3__create_outbox_events.sql` | Bảng `outbox_events` cho Transactional Outbox |
| `V4__ownership_and_booking_constraints.sql` | `owners.user_id` (UNIQUE), dọn lượt lưu trú trùng, partial unique index cho `boarding_tickets` |

| Tên Bảng | Ý Nghĩa Nghiệp Vụ | Ràng Buộc / Indexes Chính |
| :--- | :--- | :--- |
| `owners` | Danh sách chủ sở hữu thú cưng | `phone_number` UNIQUE, `user_id` UNIQUE (tài khoản auth-service; NULL với khách vãng lai), Indexes: `idx_owner_phone`, `idx_owner_email` |
| `pets` | Danh sách thú cưng | FK `owner_id` -> `owners(id)` ON DELETE CASCADE, Indexes: `idx_pet_name`, `idx_pet_owner_id` |
| `medical_records` | Bệnh án khám chữa bệnh | FK `pet_id` -> `pets(id)` ON DELETE CASCADE, Index: `idx_medical_record_pet_id` |
| `vaccinations` | Lịch sử & mũi tiêm phòng | FK `pet_id` -> `pets(id)` ON DELETE CASCADE, Index: `idx_vaccination_pet_id` |
| `cages` | Danh mục phòng/chuồng lưu trú | `code` UNIQUE, Index: `idx_cage_code` |
| `boarding_tickets` | Vé lưu chuồng khách sạn | FK `pet_id` -> `pets(id)`, FK `cage_id` -> `cages(id)` ON DELETE RESTRICT; partial unique `(cage_id)` và `(pet_id)` `WHERE status = 'ACTIVE'` |
| `outbox_events` | Việc cần giao sau commit (gRPC billing, Kafka) | `processed_at`, `attempts`, `next_attempt_at`, `last_error`; relay lấy bằng `FOR UPDATE SKIP LOCKED` |
| `spa_tickets` | Vé dịch vụ spa làm đẹp | Cột `services` kiểu `JSONB`, FK `pet_id` -> `pets(id)` |
| `appointments` | Lịch hẹn dịch vụ | FK `pet_id` -> `pets(id)`, Indexes: `idx_appointment_pet_id`, `idx_appointment_date` |

*Lưu ý: Tất cả các bảng đều kế thừa từ `Auditable` (`created_at`, `updated_at`).*

---

## 📡 Tài Liệu Chi Tiết REST API

Prefix chung: `/api/v1`. Mọi request phải đi qua gateway (header `X-User-*`); thiếu header → `401`, sai role hoặc dữ liệu của người khác → `403`. Quyền từng endpoint: xem mục *Authorization* trong [README gốc](../README.md#authorization).

### 1. Owners API (`/api/v1/owners`)
| Method | Endpoint | Mô Tả | Request Body / Query Params |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/owners` | Đăng ký chủ nuôi mới (khách: hồ sơ của chính mình) | Body JSON: `fullName`, `phoneNumber`, `email`, `address`; nhân viên có thể gửi thêm `userId` |
| `GET` | `/api/v1/owners/me` | Hồ sơ của khách đang đăng nhập | — |
| `GET` | `/api/v1/owners/{id}` | Lấy chi tiết chủ nuôi | Path param: `id` |
| `GET` | `/api/v1/owners` | Tìm kiếm theo số điện thoại (phân trang, chỉ nhân viên) | Query param: `phone`, `page`, `size`, `sort` |

### 2. Pets API (`/api/v1/pets`)
| Method | Endpoint | Mô Tả | Request Body / Query Params |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/pets` | Thêm mới thú cưng | Body JSON: `name`, `species`, `breed`, `dateOfBirth`, `gender`, `isNeutered`, `weight`, `allergies`, `notes`, `ownerId` |
| `PATCH` | `/api/v1/pets/{id}/health-info` | Cập nhật cân nặng / dị ứng | Query params: `weight`, `allergies` |
| `GET` | `/api/v1/pets/owner/{ownerId}` | Lấy danh sách thú cưng của chủ | Path param: `ownerId` |

### 3. Medical & Vaccination API (`/api/v1/pets/{petId}`)
| Method | Endpoint | Mô Tả | Request Body / Query Params |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/pets/{petId}/medical-records` | Tạo hồ sơ bệnh án khám | Query params: `reason`, `symptoms`, `diagnosis`, `treatment`, `reVisitDate` |
| `POST` | `/api/v1/pets/{petId}/vaccinations` | Ghi nhận mũi tiêm chủng | Query params: `vaccineName`, `administeredDate`, `nextDueDate` |

### 4. Boarding API (`/api/v1/boarding`)
| Method | Endpoint | Mô Tả | Request Body / Query Params |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/boarding/check-in` | Check-in nhận thú cưng vào chuồng | Query params: `petId`, `cageId`, `expectedCheckOutDate`, `notes` |
| `POST` | `/api/v1/boarding/tickets/{ticketId}/check-out` | Check-out & trả lại chuồng trống | Path param: `ticketId` |

### 5. Spa API (`/api/v1/spa-tickets`)
| Method | Endpoint | Mô Tả | Request Body / Query Params |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/spa-tickets` | Tạo vé Spa mới | Query params: `petId`, `services` (danh sách), `coatCondition`, `estimatedPrice` |
| `PATCH` | `/api/v1/spa-tickets/{ticketId}/status` | Cập nhật tiến độ (`RECEIVED`, `IN_PROGRESS`, `COMPLETED`) | Path param: `ticketId`, Query param: `newStatus` |

### 6. Appointment API (`/api/v1/appointments`)
| Method | Endpoint | Mô Tả | Request Body / Query Params |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/appointments` | Đặt lịch hẹn mới | Body JSON: `petId`, `serviceType` (`VET`/`SPA`/`HOTEL`), `appointmentDate`, `notes` |
| `PATCH` | `/api/v1/appointments/{id}/status` | Cập nhật trạng thái lịch hẹn | Path param: `id`, Query param: `newStatus` |

---

## 🛡 Xử Lý Lỗi (Error Handling)

Tất cả các ngoại lệ nghiệp vụ và lỗi hệ thống được chuẩn hóa qua `GlobalExceptionHandler` trả về định dạng `ErrorResponse`:

```json
{
  "timestamp": "2026-10-17T09:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Chuồng VIP-01 không ở trạng thái trống (AVAILABLE).",
  "path": "/api/v1/boarding/check-in",
  "validationErrors": null
}
```

- `400 Bad Request`: Thiếu tham số bắt buộc hoặc vi phạm Hibernate Bean Validation. Trường `validationErrors` sẽ chứa chi tiết từng thuộc tính không hợp lệ.
- `401 Unauthorized`: Request không có header danh tính của gateway (gọi thẳng vào service, id không phải số, role lạ).
- `403 Forbidden`: Sai role cho endpoint, hoặc khách truy cập owner, pet, lịch hẹn của người khác.
- `404 Not Found`: Khi không tìm thấy ID của thú cưng, chủ nuôi, chuồng hoặc phiếu dịch vụ (`ResourceNotFoundException`).
- `409 Conflict`: Trùng dữ liệu (`DuplicateResourceException`: trùng SĐT chủ nuôi, tài khoản đã có hồ sơ), xung đột nghiệp vụ (`CageUnavailableException`: chuồng đã có thú cưng; pet đang lưu trú; lịch hẹn trong vòng 30 phút) hoặc chuyển trạng thái sai.

---

## 🛠 Hướng Dẫn Cấu Hình & Khởi Chạy

### 1. Yêu Cầu Môi Trường
- JDK 17 trở lên.
- Docker & Docker Compose.
- PostgreSQL 15, Apache Kafka Broker.

### 2. Các Biến Môi Trường Cấu Hình Chính
| Tên Biến | Mặc Định (Dev) | Ý Nghĩa |
| :--- | :--- | :--- |
| `DB_HOST` | `localhost` | Host cơ sở dữ liệu PostgreSQL (`postgres-db` trên Docker) |
| `DB_PORT` | `5432` | Port kết nối PostgreSQL |
| `DB_NAME` | `petclinic` / `pet_clinic_pets` | Tên Database |
| `DB_USERNAME` | `postgres` / `petclinic_user` | Tài khoản đăng nhập CSDL |
| `DB_PASSWORD` | `password` / `petclinic_password` | Mật khẩu CSDL |
| `GRPC_CLIENT_BILLING-SERVICE_ADDRESS` | `static://localhost:9090` | Địa chỉ gRPC server của Billing Service |
| `SPRING_KAFKA_PRODUCER_BOOTSTRAP-SERVERS` | `localhost:9092` | Địa chỉ Kafka Broker cho producer (`kafka:9092` trên Docker) |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Cho Kafka admin client, tạo topic `pet-events` 3 partition lúc khởi động. Thiếu biến này trong Docker thì admin gọi `localhost`, chờ khoảng 30 giây và topic bị producer tự tạo với 1 partition |

### 3. Chạy Bằng Docker Compose (Toàn Bộ Hệ Thống)
Tại thư mục gốc của repository:
```bash
docker compose up -d postgres-db kafka
docker compose up -d pet-service
```

### 4. Chạy Độc Lập Cho Local Development
1. Đảm bảo PostgreSQL và Kafka đã sẵn sàng (hoặc chạy qua container hạ tầng):
   ```bash
   docker compose up -d postgres-db kafka
   ```
2. Build module hợp đồng chung:
   ```bash
   cd proto-common
   mvn clean install
   ```
3. Chạy `pet-service`:
   ```bash
   cd ../pet-service
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
4. Truy cập giao diện Swagger UI để kiểm thử trực quan:
   ```
   http://localhost:8082/swagger-ui/index.html
   ```
