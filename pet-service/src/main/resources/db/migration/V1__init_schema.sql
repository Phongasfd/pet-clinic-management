CREATE TABLE owners (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255),
    address TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);
CREATE INDEX idx_owner_phone ON owners(phone_number);
CREATE INDEX idx_owner_email ON owners(email);

CREATE TABLE pets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    species VARCHAR(50) NOT NULL,
    breed VARCHAR(255),
    date_of_birth DATE,
    gender VARCHAR(50),
    is_neutered BOOLEAN,
    weight DOUBLE PRECISION,
    allergies TEXT,
    notes TEXT,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_pet_owner FOREIGN KEY (owner_id) REFERENCES owners(id) ON DELETE CASCADE
);
CREATE INDEX idx_pet_name ON pets(name);
CREATE INDEX idx_pet_owner_id ON pets(owner_id);

CREATE TABLE medical_records (
    id BIGSERIAL PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    symptoms TEXT,
    diagnosis TEXT,
    treatment TEXT,
    re_visit_date DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_medical_pet FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE
);
CREATE INDEX idx_medical_record_pet_id ON medical_records(pet_id);

CREATE TABLE vaccinations (
    id BIGSERIAL PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    vaccine_name VARCHAR(255) NOT NULL,
    administered_date DATE NOT NULL,
    next_due_date DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_vaccination_pet FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE
);
CREATE INDEX idx_vaccination_pet_id ON vaccinations(pet_id);

CREATE TABLE cages (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    daily_rate DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);
CREATE INDEX idx_cage_code ON cages(code);

CREATE TABLE spa_tickets (
    id BIGSERIAL PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    services JSONB,
    coat_condition VARCHAR(255),
    groomer_name VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    estimated_price DECIMAL(15, 2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_spa_pet FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE
);
CREATE INDEX idx_spa_ticket_pet_id ON spa_tickets(pet_id);

CREATE TABLE boarding_tickets (
    id BIGSERIAL PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    cage_id BIGINT NOT NULL,
    check_in_date TIMESTAMP NOT NULL,
    expected_check_out_date TIMESTAMP NOT NULL,
    actual_check_out_date TIMESTAMP,
    special_care_notes TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_boarding_pet FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE,
    CONSTRAINT fk_boarding_cage FOREIGN KEY (cage_id) REFERENCES cages(id) ON DELETE RESTRICT
);
CREATE INDEX idx_boarding_ticket_pet_id ON boarding_tickets(pet_id);
CREATE INDEX idx_boarding_ticket_cage_id ON boarding_tickets(cage_id);

CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    service_type VARCHAR(50) NOT NULL,
    appointment_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_appointment_pet FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE
);
CREATE INDEX idx_appointment_pet_id ON appointments(pet_id);
CREATE INDEX idx_appointment_date ON appointments(appointment_date);
