-- ============================================================
-- V1__init.sql
-- Ride-Sharing Platform - Initial Schema
-- ============================================================

-- ---------------------------------------------------------------
-- USERS
-- ---------------------------------------------------------------
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id       VARCHAR(36)  NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL,
    email           VARCHAR(160) NOT NULL,
    mobile          VARCHAR(20)  NOT NULL,
    password_hash   VARCHAR(255),
    gender          VARCHAR(20),
    dob             DATE,
    profile_photo_url VARCHAR(500),
    preferred_language VARCHAR(10) DEFAULT 'en',
    home_lat        DOUBLE,
    home_lng        DOUBLE,
    office_lat      DOUBLE,
    office_lng      DOUBLE,
    role_passenger  BOOLEAN NOT NULL DEFAULT TRUE,
    role_driver     BOOLEAN NOT NULL DEFAULT FALSE,
    identity_provider VARCHAR(20) DEFAULT 'LOCAL', -- LOCAL, GOOGLE, APPLE
    average_rating  DECIMAL(3,2) DEFAULT 0.00,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, DELETED
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_users_email (email),
    UNIQUE KEY uq_users_mobile (mobile),
    INDEX idx_users_mobile (mobile),
    INDEX idx_users_email (email)
);

CREATE TABLE emergency_contacts (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    name        VARCHAR(120) NOT NULL,
    mobile      VARCHAR(20) NOT NULL,
    relation    VARCHAR(50),
    CONSTRAINT fk_ec_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------
-- AUTH
-- ---------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_rt_user (user_id)
);

CREATE TABLE otp_codes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    mobile      VARCHAR(20) NOT NULL,
    code_hash   VARCHAR(255) NOT NULL,
    purpose     VARCHAR(30) NOT NULL, -- LOGIN, REGISTER, RESET
    expires_at  TIMESTAMP NOT NULL,
    consumed    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_otp_mobile (mobile)
);

-- ---------------------------------------------------------------
-- DRIVER
-- ---------------------------------------------------------------
CREATE TABLE drivers (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE,
    license_number      VARCHAR(60) NOT NULL,
    license_doc_url     VARCHAR(500),
    government_id_type  VARCHAR(30),
    government_id_doc_url VARCHAR(500),
    address_proof_doc_url VARCHAR(500),
    selfie_doc_url      VARCHAR(500),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, VERIFIED, REJECTED
    rejection_reason    VARCHAR(255),
    is_online           BOOLEAN NOT NULL DEFAULT FALSE,
    last_online_at      TIMESTAMP NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_driver_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_driver_status (status),
    INDEX idx_driver_user (user_id)
);

-- ---------------------------------------------------------------
-- VEHICLE
-- ---------------------------------------------------------------
CREATE TABLE vehicles (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    driver_id           BIGINT NOT NULL,
    vehicle_number      VARCHAR(20) NOT NULL,
    brand               VARCHAR(60),
    model                VARCHAR(60),
    fuel_type           VARCHAR(20), -- PETROL, DIESEL, CNG, ELECTRIC, HYBRID
    transmission        VARCHAR(20), -- MANUAL, AUTOMATIC
    color               VARCHAR(30),
    seating_capacity    INT NOT NULL,
    insurance_expiry    DATE,
    registration_expiry DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicle_driver FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE CASCADE,
    UNIQUE KEY uq_vehicle_number (vehicle_number),
    INDEX idx_vehicle_driver (driver_id)
);

-- ---------------------------------------------------------------
-- RIDE
-- ---------------------------------------------------------------
CREATE TABLE rides (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id           VARCHAR(36) NOT NULL UNIQUE,
    driver_id           BIGINT NOT NULL,
    vehicle_id          BIGINT NOT NULL,
    origin_address       VARCHAR(255) NOT NULL,
    origin_lat          DOUBLE NOT NULL,
    origin_lng          DOUBLE NOT NULL,
    destination_address VARCHAR(255) NOT NULL,
    destination_lat     DOUBLE NOT NULL,
    destination_lng     DOUBLE NOT NULL,
    departure_date      DATE NOT NULL,
    departure_time      TIME NOT NULL,
    departure_at        TIMESTAMP NOT NULL,
    total_seats         INT NOT NULL,
    available_seats     INT NOT NULL,
    price_per_seat      DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    luggage_allowed     BOOLEAN NOT NULL DEFAULT TRUE,
    smoking_allowed     BOOLEAN NOT NULL DEFAULT FALSE,
    music_preference    VARCHAR(50),
    women_only          BOOLEAN NOT NULL DEFAULT FALSE,
    pets_allowed        BOOLEAN NOT NULL DEFAULT FALSE,
    description          VARCHAR(500),
    max_detour_km       DECIMAL(6,2) NOT NULL DEFAULT 5.00,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, ACTIVE, ACCEPTED, CANCELLED, FINISHED
    route_generated      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ride_driver FOREIGN KEY (driver_id) REFERENCES drivers(id),
    CONSTRAINT fk_ride_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    INDEX idx_ride_status (status),
    INDEX idx_ride_departure (departure_at),
    INDEX idx_ride_driver (driver_id)
);

-- ---------------------------------------------------------------
-- ROUTE (decoded polyline -> ordered waypoints + H3 cells)
-- ---------------------------------------------------------------
CREATE TABLE ride_routes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    ride_id         BIGINT NOT NULL UNIQUE,
    provider        VARCHAR(30) NOT NULL,        -- MAPPLS, GOOGLE, OSRM
    encoded_polyline TEXT,
    distance_meters  INT,
    duration_seconds INT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_route_ride FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE
);

CREATE TABLE ride_route_points (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    ride_id         BIGINT NOT NULL,
    sequence_no     INT NOT NULL,
    lat             DOUBLE NOT NULL,
    lng             DOUBLE NOT NULL,
    h3_cell         VARCHAR(20) NOT NULL,
    cumulative_distance_m INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_rrp_ride FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE,
    INDEX idx_rrp_ride_seq (ride_id, sequence_no),
    INDEX idx_rrp_h3 (h3_cell)
);

-- ---------------------------------------------------------------
-- BOOKING
-- ---------------------------------------------------------------
CREATE TABLE bookings (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id           VARCHAR(36) NOT NULL UNIQUE,
    ride_id             BIGINT NOT NULL,
    passenger_id        BIGINT NOT NULL,
    pickup_lat          DOUBLE NOT NULL,
    pickup_lng          DOUBLE NOT NULL,
    pickup_address       VARCHAR(255),
    pickup_sequence_no   INT NOT NULL,
    drop_lat            DOUBLE NOT NULL,
    drop_lng            DOUBLE NOT NULL,
    drop_address         VARCHAR(255),
    drop_sequence_no     INT NOT NULL,
    seats_booked         INT NOT NULL,
    fare                 DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, CONFIRMED, REJECTED, CANCELLED, COMPLETED
    cancelled_by         VARCHAR(20),  -- PASSENGER, DRIVER, SYSTEM, ADMIN
    cancellation_reason  VARCHAR(255),
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_ride FOREIGN KEY (ride_id) REFERENCES rides(id),
    CONSTRAINT fk_booking_passenger FOREIGN KEY (passenger_id) REFERENCES users(id),
    INDEX idx_booking_status (status),
    INDEX idx_booking_ride (ride_id),
    INDEX idx_booking_passenger (passenger_id),
    UNIQUE KEY uq_no_duplicate_booking (ride_id, passenger_id, status)
);

-- ---------------------------------------------------------------
-- PAYMENT / WALLET / COUPON
-- ---------------------------------------------------------------
CREATE TABLE payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id      BIGINT NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    amount          DECIMAL(10,2) NOT NULL,
    currency        VARCHAR(5) NOT NULL DEFAULT 'INR',
    provider        VARCHAR(30),
    provider_ref    VARCHAR(120),
    status          VARCHAR(20) NOT NULL DEFAULT 'INITIATED', -- INITIATED, SUCCESS, FAILED, REFUNDED
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE TABLE wallets (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL UNIQUE,
    balance     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE wallet_transactions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id   BIGINT NOT NULL,
    amount      DECIMAL(10,2) NOT NULL,
    type        VARCHAR(20) NOT NULL, -- CREDIT, DEBIT
    reason      VARCHAR(120),
    reference   VARCHAR(120),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wt_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id)
);

CREATE TABLE coupons (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(30) NOT NULL UNIQUE,
    discount_type   VARCHAR(10) NOT NULL, -- FLAT, PERCENT
    discount_value  DECIMAL(10,2) NOT NULL,
    max_discount    DECIMAL(10,2),
    valid_from      TIMESTAMP,
    valid_to        TIMESTAMP,
    usage_limit     INT,
    used_count      INT NOT NULL DEFAULT 0,
    active          BOOLEAN NOT NULL DEFAULT TRUE
);

-- ---------------------------------------------------------------
-- NOTIFICATION
-- ---------------------------------------------------------------
CREATE TABLE notifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    channel     VARCHAR(20) NOT NULL, -- PUSH, SMS, EMAIL, IN_APP
    event_type  VARCHAR(50) NOT NULL,
    title       VARCHAR(120),
    body        VARCHAR(500),
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, FAILED, READ
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_notification_user (user_id)
);

-- ---------------------------------------------------------------
-- CHAT
-- ---------------------------------------------------------------
CREATE TABLE chat_threads (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    ride_id     BIGINT NOT NULL,
    passenger_id BIGINT NOT NULL,
    driver_id   BIGINT NOT NULL,
    expires_at  TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_thread_ride FOREIGN KEY (ride_id) REFERENCES rides(id),
    UNIQUE KEY uq_thread (ride_id, passenger_id)
);

CREATE TABLE chat_messages (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    thread_id   BIGINT NOT NULL,
    sender_id   BIGINT NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT', -- TEXT, IMAGE, LOCATION, RIDE_DETAILS
    content      TEXT,
    read_at      TIMESTAMP NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_thread FOREIGN KEY (thread_id) REFERENCES chat_threads(id) ON DELETE CASCADE,
    INDEX idx_message_thread (thread_id)
);

-- ---------------------------------------------------------------
-- REVIEW
-- ---------------------------------------------------------------
CREATE TABLE reviews (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    ride_id         BIGINT NOT NULL,
    reviewer_id     BIGINT NOT NULL,
    reviewee_id     BIGINT NOT NULL,
    rating          INT NOT NULL,  -- 1-5
    comment         VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_ride FOREIGN KEY (ride_id) REFERENCES rides(id),
    UNIQUE KEY uq_review (ride_id, reviewer_id, reviewee_id),
    INDEX idx_review_reviewee (reviewee_id)
);

-- ---------------------------------------------------------------
-- AUDIT
-- ---------------------------------------------------------------
CREATE TABLE audit_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    correlation_id  VARCHAR(60),
    user_id         BIGINT,
    action          VARCHAR(60) NOT NULL, -- LOGIN, LOGOUT, RIDE_PUBLISH, BOOKING, PAYMENT, REFUND, ADMIN_ACTION, CONFIG_CHANGE
    entity_type     VARCHAR(60),
    entity_id       VARCHAR(60),
    details_json    JSON,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_created (created_at)
);

-- ---------------------------------------------------------------
-- SUPPORT / ADMIN CONFIG
-- ---------------------------------------------------------------
CREATE TABLE support_tickets (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    subject     VARCHAR(200) NOT NULL,
    description TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'OPEN', -- OPEN, IN_PROGRESS, RESOLVED, CLOSED
    priority    VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    assigned_to BIGINT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE system_configuration (
    config_key   VARCHAR(100) PRIMARY KEY,
    config_value VARCHAR(500) NOT NULL,
    description  VARCHAR(255),
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO system_configuration (config_key, config_value, description) VALUES
    ('search.max_pickup_radius_km', '2', 'Maximum pickup radius for ride search'),
    ('search.max_detour_km', '5', 'Maximum detour allowed for a ride'),
    ('search.departure_window_hours', '6', 'Departure window for search results'),
    ('booking.refund_policy', 'PARTIAL_TILL_2H', 'Refund policy identifier'),
    ('chat.block_after_ride_hours', '24', 'Hours after ride completion before chat is blocked');
