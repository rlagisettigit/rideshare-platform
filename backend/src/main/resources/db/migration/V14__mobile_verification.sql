-- FR-001 Registration: mobile numbers are verified via an OTP (delivered through MSG91) sent
-- right after signup; stays false until POST /api/v1/auth/mobile/verify succeeds.
ALTER TABLE users ADD COLUMN mobile_verified BOOLEAN NOT NULL DEFAULT FALSE;
