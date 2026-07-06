CREATE TABLE recurring_rides (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id            VARCHAR(36) NOT NULL UNIQUE,
    driver_id            BIGINT NOT NULL,
    vehicle_id           BIGINT NOT NULL,
    origin_address       VARCHAR(255) NOT NULL,
    origin_lat           DOUBLE NOT NULL,
    origin_lng           DOUBLE NOT NULL,
    destination_address  VARCHAR(255) NOT NULL,
    destination_lat      DOUBLE NOT NULL,
    destination_lng      DOUBLE NOT NULL,
    days_of_week         VARCHAR(30) NOT NULL,
    departure_time       TIME NOT NULL,
    start_date           DATE NOT NULL,
    end_date             DATE NOT NULL,
    total_seats          INT NOT NULL,
    price_per_seat       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    luggage_allowed      BOOLEAN NOT NULL DEFAULT TRUE,
    smoking_allowed      BOOLEAN NOT NULL DEFAULT FALSE,
    women_only           BOOLEAN NOT NULL DEFAULT FALSE,
    pets_allowed         BOOLEAN NOT NULL DEFAULT FALSE,
    description          VARCHAR(500),
    max_detour_km        DECIMAL(6,2) NOT NULL DEFAULT 5.00,
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_recurring_ride_driver FOREIGN KEY (driver_id) REFERENCES drivers(id),
    CONSTRAINT fk_recurring_ride_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    INDEX idx_recurring_ride_driver (driver_id)
);

ALTER TABLE rides ADD COLUMN recurring_ride_id BIGINT NULL;
ALTER TABLE rides ADD CONSTRAINT fk_ride_recurring_ride FOREIGN KEY (recurring_ride_id) REFERENCES recurring_rides(id);
ALTER TABLE rides ADD INDEX idx_ride_recurring (recurring_ride_id);
