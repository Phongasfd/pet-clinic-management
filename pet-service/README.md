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
- **Tạo mới hồ sơ khách hàng**: Đăng ký thông tin chủ thú cưng gồm tên, số điện thoại (ràng buộc duy nhất), email và địa chỉ.
- **Tự động liên kết tài khoản Billing**: Khi tạo mới chủ nuôi thành công, hệ thống tự động phát lời gọi gRPC `CreateBillingAccount` sang `billing-service` để tạo tài khoản thanh toán / ví trả sau.
- **Tìm kiếm & Tra cứu**:
  - Tìm kiếm theo số điện thoại có hỗ trợ phân trang (`Pageable`).
  - Lấy chi tiết thông tin chủ sở hữu theo `ownerId`.

### 2. Quản lý Thú cưng (Pet Management)
- **Đăng ký hồ sơ thú cưng**: Khởi tạo thông tin thú cưng liên kết trực tiếp với chủ nuôi (`ownerId`):
  - Chủng loại (`Species`): `DOG`, `CAT`, `OTHER`.
  - Giới tính (`Gender`): `MALE`, `FEMALE`, `UNKNOWN`.
  - Giống loài (`breed`), ngày sinh (`dateOfBirth`), trạng thái triệt sản (`isNeutered`), cân nặng (`weight`), tiền sử dị ứng (`allergies`) và ghi chú đặc biệt.
- **Phát sự kiện Kafka (Event Publishing)**: Sau khi tạo mới thành công, service publish sự kiện `PET_CREATED` tuần tự hóa bằng Protobuf vào Kafka topic `pet-events` (Message key = `petId` để đảm bảo thứ tự xử lý).
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
- Giao tiếp đồng bộ hiệu năng cao thông qua contract `billing_service.proto` trong `proto-common`.
- Khi một chủ nuôi mới được tạo, `BillingClientService` kích hoạt `createBillingAccount`:
  - Target: `static://billing-service:9090` (môi trường container) hoặc cấu hình qua `GRPC_CLIENT_BILLING-SERVICE_ADDRESS`.
  - **Khả năng chịu lỗi (Fault Resilience)**: Nếu `billing-service` tạm thời mất kết nối, hệ thống ghi log cảnh báo (`log.warn`) và không làm rollback giao dịch tạo chủ sở hữu, đảm bảo trải nghiệm người dùng không bị gián đoạn.

### 🔹 Kafka Event Publisher -> Analytics Service
- Cấu hình Producer qua `KafkaProducerConfig` và `PetEventPublisher`.
- Sự kiện được gửi bất đồng bộ (`CompletableFuture`) với độ tin cậy cao (`acks: all`, `retries: 3`).
- **Protobuf Event Payload (`PetEvent`)**:
  - `eventId`: UUID duy nhất để hỗ trợ kiểm tra tính idempotent phía Consumer.
  - `eventType`: `"PET_CREATED"`.
  - `timestamp`: Epoch millisecond UTC.
  - `petId`, `petName`, `species`, `breed`, `ownerId`.
  - Key message là `String.valueOf(petId)` giúp đảm bảo các sự kiện của cùng 1 thú cưng luôn vào cùng Kafka partition.

### 🔹 Context Propagation & Security
- `UserContextInterceptor` chặn tất cả các request đến từ API Gateway.
- Tự động trích xuất các header nhận thực đã được Gateway phân giải:
  - `X-User-Id`
  - `X-User-Name`
  - `X-User-Role`
- Lưu trữ vào `UserContextHolder` sử dụng `ThreadLocal`, cho phép các service layer bên trong truy vấn danh tính người gọi mà không cần phụ thuộc trực tiếp vào servlet context. Tự động giải phóng bộ nhớ sau mỗi request (`afterCompletion`).

---

## 🗄 Mô Hình Cơ Sở Dữ Liệu (Database Schema)

PostgreSQL schema được quản lý tự động bởi Flyway (`db/migration/V1__init_schema.sql`):

| Tên Bảng | Ý Nghĩa Nghiệp Vụ | Ràng Buộc / Indexes Chính |
| :--- | :--- | :--- |
| `owners` | Danh sách chủ sở hữu thú cưng | `phone_number` UNIQUE, Indexes: `idx_owner_phone`, `idx_owner_email` |
| `pets` | Danh sách thú cưng | FK `owner_id` -> `owners(id)` ON DELETE CASCADE, Indexes: `idx_pet_name`, `idx_pet_owner_id` |
| `medical_records` | Bệnh án khám chữa bệnh | FK `pet_id` -> `pets(id)` ON DELETE CASCADE, Index: `idx_medical_record_pet_id` |
| `vaccinations` | Lịch sử & mũi tiêm phòng | FK `pet_id` -> `pets(id)` ON DELETE CASCADE, Index: `idx_vaccination_pet_id` |
| `cages` | Danh mục phòng/chuồng lưu trú | `code` UNIQUE, Index: `idx_cage_code` |
| `boarding_tickets` | Vé lưu chuồng khách sạn | FK `pet_id` -> `pets(id)`, FK `cage_id` -> `cages(id)` ON DELETE RESTRICT |
| `spa_tickets` | Vé dịch vụ spa làm đẹp | Cột `services` kiểu `JSONB`, FK `pet_id` -> `pets(id)` |
| `appointments` | Lịch hẹn dịch vụ | FK `pet_id` -> `pets(id)`, Indexes: `idx_appointment_pet_id`, `idx_appointment_date` |

*Lưu ý: Tất cả các bảng đều kế thừa từ `Auditable` (`created_at`, `updated_at`).*

---

## 📡 Tài Liệu Chi Tiết REST API

Prefix chung: `/api/v1`

### 1. Owners API (`/api/v1/owners`)
| Method | Endpoint | Mô Tả | Request Body / Query Params |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/owners` | Đăng ký chủ nuôi mới | Body JSON: `fullName`, `phoneNumber`, `email`, `address` |
| `GET` | `/api/v1/owners/{id}` | Lấy chi tiết chủ nuôi | Path param: `id` |
| `GET` | `/api/v1/owners` | Tìm kiếm theo số điện thoại (phân trang) | Query param: `phone`, `page`, `size`, `sort` |

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
- `404 Not Found`: Khi không tìm thấy ID của thú cưng, chủ nuôi, chuồng hoặc phiếu dịch vụ (`ResourceNotFoundException`).
- `409 Conflict`: Vi phạm ràng buộc dữ liệu trùng lặp (`DuplicateResourceException` - trùng SĐT chủ nuôi) hoặc xung đột nghiệp vụ (`CageUnavailableException` - chuồng đã có thú cưng).

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
| `SPRING_KAFKA_PRODUCER_BOOTSTRAP-SERVERS` | `localhost:9092` | Địa chỉ Kafka Broker (`kafka:9092` trên Docker) |

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
