-- Ownership: an owner profile created by a customer is linked to their auth-service user id,
-- so pet-service can check "is this your pet?". NULL for walk-in owners registered by staff.
ALTER TABLE owners ADD COLUMN user_id BIGINT;
ALTER TABLE owners ADD CONSTRAINT uq_owners_user_id UNIQUE (user_id);

-- Boarding: the last line of defence against double booking. BoardingService already locks
-- the cage row, but these make the invariant hold even for code paths that forget to:
-- a cage holds at most one active stay, and a pet is in at most one cage at a time.
--
-- Data written by earlier releases can violate both: the old check-in neither locked the cage nor
-- checked whether the pet was already boarding. A single duplicate makes CREATE UNIQUE INDEX fail,
-- Flyway rolls V4 back and pet-service exits on every start. So first keep the newest ACTIVE stay
-- per pet, then per cage, close the others, and free cages that no longer hold an active stay.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY pet_id ORDER BY check_in_date DESC, id DESC) AS rn
    FROM boarding_tickets
    WHERE status = 'ACTIVE'
)
UPDATE boarding_tickets t
SET status = 'COMPLETED',
    actual_check_out_date = COALESCE(t.actual_check_out_date, NOW()),
    updated_at = NOW()
FROM ranked r
WHERE t.id = r.id AND r.rn > 1;

WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY cage_id ORDER BY check_in_date DESC, id DESC) AS rn
    FROM boarding_tickets
    WHERE status = 'ACTIVE'
)
UPDATE boarding_tickets t
SET status = 'COMPLETED',
    actual_check_out_date = COALESCE(t.actual_check_out_date, NOW()),
    updated_at = NOW()
FROM ranked r
WHERE t.id = r.id AND r.rn > 1;

UPDATE cages c
SET status = 'AVAILABLE', updated_at = NOW()
WHERE c.status = 'OCCUPIED'
  AND NOT EXISTS (SELECT 1 FROM boarding_tickets t WHERE t.cage_id = c.id AND t.status = 'ACTIVE');

CREATE UNIQUE INDEX uq_boarding_active_cage ON boarding_tickets (cage_id) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uq_boarding_active_pet ON boarding_tickets (pet_id) WHERE status = 'ACTIVE';
