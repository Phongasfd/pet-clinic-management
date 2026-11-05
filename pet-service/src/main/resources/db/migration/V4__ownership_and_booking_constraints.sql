-- Ownership: an owner profile created by a customer is linked to their auth-service user id,
-- so pet-service can check "is this your pet?". NULL for walk-in owners registered by staff.
ALTER TABLE owners ADD COLUMN user_id BIGINT;
ALTER TABLE owners ADD CONSTRAINT uq_owners_user_id UNIQUE (user_id);

-- Boarding: the last line of defence against double booking. BoardingService already locks
-- the cage row, but these make the invariant hold even for code paths that forget to:
-- a cage holds at most one active stay, and a pet is in at most one cage at a time.
CREATE UNIQUE INDEX uq_boarding_active_cage ON boarding_tickets (cage_id) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uq_boarding_active_pet ON boarding_tickets (pet_id) WHERE status = 'ACTIVE';
