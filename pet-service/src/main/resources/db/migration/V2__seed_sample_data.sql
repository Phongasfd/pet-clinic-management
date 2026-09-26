-- 3 Owners
INSERT INTO owners (full_name, phone_number, email, address) VALUES
('Nguyễn Văn A', '0901234567', 'nguyenvana@gmail.com', 'Quận 1, TP.HCM'),
('Trần Thị B', '0912345678', 'tranthib@gmail.com', 'Quận 3, TP.HCM'),
('Lê Hoàng C', '0987654321', 'lehoangc@gmail.com', 'Quận 7, TP.HCM');

-- 5 Pets
INSERT INTO pets (name, species, breed, date_of_birth, gender, is_neutered, weight, allergies, owner_id) VALUES
('Milo', 'DOG', 'Poodle', '2020-05-10', 'MALE', true, 4.5, 'Không', 1),
('LuLu', 'DOG', 'Corgi', '2021-08-15', 'FEMALE', false, 12.0, 'Dị ứng gà', 1),
('Mimi', 'CAT', 'British Shorthair', '2019-12-01', 'FEMALE', true, 5.2, 'Không', 2),
('Bim', 'CAT', 'Mèo ta', '2022-01-20', 'MALE', false, 3.8, 'Không', 2),
('Rex', 'DOG', 'Golden Retriever', '2018-07-30', 'MALE', true, 30.5, 'Không', 3);

-- Cages
INSERT INTO cages (code, type, status, daily_rate) VALUES
('VIP-01', 'VIP', 'AVAILABLE', 500000),
('VIP-02', 'VIP', 'AVAILABLE', 500000),
('STD-01', 'STANDARD', 'AVAILABLE', 200000),
('STD-02', 'STANDARD', 'AVAILABLE', 200000),
('CAT-01', 'CAT_ROOM', 'AVAILABLE', 150000);

-- Appointments
INSERT INTO appointments (pet_id, service_type, appointment_date, status, notes) VALUES
(1, 'SPA', '2026-10-01 10:00:00', 'PENDING', 'Cắt tỉa lông gọn gàng'),
(3, 'VET', '2026-10-02 14:30:00', 'CONFIRMED', 'Kiểm tra sức khỏe định kỳ');

-- Spa Tickets
INSERT INTO spa_tickets (pet_id, services, coat_condition, groomer_name, status, estimated_price) VALUES
(2, '["Tắm", "Cắt tỉa", "Vệ sinh tai"]', 'Hơi xơ', 'Lan', 'RECEIVED', 350000);
