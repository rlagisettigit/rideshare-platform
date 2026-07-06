CREATE TABLE vehicle_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    brand VARCHAR(60) NOT NULL,
    model VARCHAR(60) NOT NULL,
    category VARCHAR(20) NOT NULL,
    seats INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_vehicle_catalog_brand_model UNIQUE (brand, model)
);

ALTER TABLE vehicles ADD COLUMN category VARCHAR(20) NULL;

INSERT INTO vehicle_catalog (brand, model, category, seats) VALUES
-- Hatchback
('Maruti Suzuki', 'Swift', 'HATCHBACK', 5),
('Maruti Suzuki', 'Baleno', 'HATCHBACK', 5),
('Maruti Suzuki', 'WagonR', 'HATCHBACK', 5),
('Maruti Suzuki', 'Celerio', 'HATCHBACK', 5),
('Maruti Suzuki', 'Ignis', 'HATCHBACK', 5),
('Hyundai', 'Grand i10 Nios', 'HATCHBACK', 5),
('Hyundai', 'i20', 'HATCHBACK', 5),
('Tata', 'Tiago', 'HATCHBACK', 5),
('Tata', 'Altroz', 'HATCHBACK', 5),
('Honda', 'Jazz', 'HATCHBACK', 5),
('Toyota', 'Glanza', 'HATCHBACK', 5),
-- Sedan
('Maruti Suzuki', 'Dzire', 'SEDAN', 5),
('Honda', 'City', 'SEDAN', 5),
('Honda', 'Amaze', 'SEDAN', 5),
('Hyundai', 'Verna', 'SEDAN', 5),
('Hyundai', 'Aura', 'SEDAN', 5),
('Skoda', 'Slavia', 'SEDAN', 5),
('Volkswagen', 'Virtus', 'SEDAN', 5),
('Toyota', 'Etios', 'SEDAN', 5),
('Tata', 'Tigor', 'SEDAN', 5),
-- SUV
('Mahindra', 'XUV700', 'SUV', 7),
('Mahindra', 'Scorpio', 'SUV', 7),
('Mahindra', 'Scorpio-N', 'SUV', 7),
('Mahindra', 'Bolero', 'SUV', 8),
('Tata', 'Nexon', 'SUV', 5),
('Tata', 'Harrier', 'SUV', 5),
('Tata', 'Safari', 'SUV', 7),
('Tata', 'Punch', 'SUV', 5),
('Hyundai', 'Creta', 'SUV', 5),
('Hyundai', 'Venue', 'SUV', 5),
('Kia', 'Seltos', 'SUV', 5),
('Kia', 'Sonet', 'SUV', 5),
('Toyota', 'Fortuner', 'SUV', 7),
('Toyota', 'Urban Cruiser Hyryder', 'SUV', 5),
('Maruti Suzuki', 'Brezza', 'SUV', 5),
('Maruti Suzuki', 'Grand Vitara', 'SUV', 5),
-- MUV / MPV
('Toyota', 'Innova Crysta', 'MUV', 7),
('Toyota', 'Innova Hycross', 'MUV', 7),
('Maruti Suzuki', 'Ertiga', 'MUV', 7),
('Maruti Suzuki', 'XL6', 'MUV', 6),
('Kia', 'Carens', 'MUV', 6),
('Mahindra', 'Marazzo', 'MUV', 7),
-- Van
('Maruti Suzuki', 'Eeco', 'VAN', 7);
