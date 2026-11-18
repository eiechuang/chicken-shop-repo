MERGE INTO users (username, password_hash, role)
KEY(username)
VALUES (
    'staff',
    '123',
    'PROVIDER'
);

MERGE INTO pickup_slots (slot_id, start_time, end_time)
KEY(slot_id)
VALUES
(1, '2026-10-08 17:00:00', '2026-10-08 17:15:00'),
(2, '2026-10-08 17:15:00', '2026-10-08 17:30:00'),
(3, '2026-10-08 17:30:00', '2026-10-08 17:45:00');