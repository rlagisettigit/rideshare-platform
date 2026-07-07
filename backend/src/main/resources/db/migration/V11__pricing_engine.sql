-- Configurable fare-calculation factors for the Pricing Engine (PricingEngine + 14 FareCalculators).
-- One generic table backs every calculator: a row is either global (vehicle_category NULL) or
-- scoped to one vehicle_category, and is looked up either by exact factor_key match (flat rate /
-- percent / named band such as a weather condition) or by a [range_start, range_end) window
-- (peak-hour minute-of-day, demand ratio band, toll distance band). See FactorSet for lookup rules.

CREATE TABLE pricing_factors (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    calculator       VARCHAR(40)  NOT NULL, -- BASE_FARE, DISTANCE, TIME, FUEL_ADJUSTMENT, TOLL, DEMAND,
                                             -- PEAK_HOUR, WEATHER, VEHICLE_MULTIPLIER, SEAT_PRICING,
                                             -- DISCOUNT, COMMISSION, TAX
    vehicle_category VARCHAR(30),           -- HATCHBACK, SEDAN, SUV, MUV, ... ; NULL = applies to all
    factor_key       VARCHAR(60)  NOT NULL, -- e.g. PER_KM_RATE, MULTIPLIER, RAIN, GENERAL_DISCOUNT_PERCENT
    factor_value     DECIMAL(10,4) NOT NULL,
    value_type       VARCHAR(12)  NOT NULL DEFAULT 'FLAT', -- FLAT, PERCENT, MULTIPLIER (informational; each
                                                            -- calculator documents how it interprets its own rows)
    range_start      DECIMAL(10,2),          -- generic lower bound, inclusive (minute-of-day, demand %, distance km)
    range_end        DECIMAL(10,2),          -- generic upper bound, exclusive
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    description      VARCHAR(255),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pf_lookup (calculator, vehicle_category, active)
);

-- BASE_FARE: flat starting fare per vehicle category (per-seat basis).
INSERT INTO pricing_factors (calculator, vehicle_category, factor_key, factor_value, value_type, description) VALUES
    ('BASE_FARE', NULL,         'BASE_FARE', 50.00, 'FLAT', 'Default base fare when vehicle category is unknown'),
    ('BASE_FARE', 'HATCHBACK',  'BASE_FARE', 40.00, 'FLAT', 'Base fare - hatchback'),
    ('BASE_FARE', 'SEDAN',      'BASE_FARE', 60.00, 'FLAT', 'Base fare - sedan'),
    ('BASE_FARE', 'SUV',        'BASE_FARE', 90.00, 'FLAT', 'Base fare - SUV'),
    ('BASE_FARE', 'MUV',        'BASE_FARE', 110.00, 'FLAT', 'Base fare - MUV/MPV');

-- DISTANCE: rate per kilometre.
INSERT INTO pricing_factors (calculator, vehicle_category, factor_key, factor_value, value_type, description) VALUES
    ('DISTANCE', NULL,         'PER_KM_RATE', 9.00, 'FLAT', 'Default per-km rate'),
    ('DISTANCE', 'HATCHBACK',  'PER_KM_RATE', 8.00, 'FLAT', 'Per-km rate - hatchback'),
    ('DISTANCE', 'SEDAN',      'PER_KM_RATE', 10.00, 'FLAT', 'Per-km rate - sedan'),
    ('DISTANCE', 'SUV',        'PER_KM_RATE', 13.00, 'FLAT', 'Per-km rate - SUV'),
    ('DISTANCE', 'MUV',        'PER_KM_RATE', 15.00, 'FLAT', 'Per-km rate - MUV/MPV');

-- TIME: rate per minute of estimated travel time (traffic/duration cost, vehicle-agnostic).
INSERT INTO pricing_factors (calculator, vehicle_category, factor_key, factor_value, value_type, description) VALUES
    ('TIME', NULL, 'PER_MINUTE_RATE', 1.50, 'FLAT', 'Per-minute rate applied to estimated trip duration');

-- FUEL_ADJUSTMENT: surcharge percent on the base+distance+time subtotal.
INSERT INTO pricing_factors (calculator, vehicle_category, factor_key, factor_value, value_type, description) VALUES
    ('FUEL_ADJUSTMENT', NULL,  'FUEL_SURCHARGE_PERCENT', 4.00, 'PERCENT', 'Default fuel price surcharge'),
    ('FUEL_ADJUSTMENT', 'SUV', 'FUEL_SURCHARGE_PERCENT', 6.00, 'PERCENT', 'Higher fuel surcharge for SUVs'),
    ('FUEL_ADJUSTMENT', 'MUV', 'FUEL_SURCHARGE_PERCENT', 6.00, 'PERCENT', 'Higher fuel surcharge for MUVs');

-- TOLL: flat estimated toll banded by trip distance (km), applies to all vehicle categories.
INSERT INTO pricing_factors (calculator, vehicle_category, factor_key, factor_value, value_type, range_start, range_end, description) VALUES
    ('TOLL', NULL, 'DISTANCE_BAND', 0.00,  'FLAT', 0,   15,     'No toll expected under 15km'),
    ('TOLL', NULL, 'DISTANCE_BAND', 40.00, 'FLAT', 15,  50,     'Estimated toll for 15-50km trips'),
    ('TOLL', NULL, 'DISTANCE_BAND', 90.00, 'FLAT', 50,  999999, 'Estimated toll for 50km+ trips');

-- DEMAND: surge multiplier banded by current ride occupancy ratio (0-100%), as a proxy for demand.
INSERT INTO pricing_factors (calculator, vehicle_category, factor_key, factor_value, value_type, range_start, range_end, description) VALUES
    ('DEMAND', NULL, 'OCCUPANCY_BAND', 1.00, 'MULTIPLIER', 0,   40,  'Low demand - no surge'),
    ('DEMAND', NULL, 'OCCUPANCY_BAND', 1.15, 'MULTIPLIER', 40,  75,  'Medium demand surge'),
    ('DEMAND', NULL, 'OCCUPANCY_BAND', 1.35, 'MULTIPLIER', 75,  100.01, 'High demand surge');

-- PEAK_HOUR: multiplier banded by minute-of-day (0-1440) of the ride's departure/pickup time.
INSERT INTO pricing_factors (calculator, vehicle_category, factor_key, factor_value, value_type, range_start, range_end, description) VALUES
    ('PEAK_HOUR', NULL, 'MORNING_PEAK', 1.20, 'MULTIPLIER', 420,  600,  'Morning peak 07:00-10:00'),
    ('PEAK_HOUR', NULL, 'EVENING_PEAK', 1.25, 'MULTIPLIER', 1020, 1200, 'Evening peak 17:00-20:00');

-- WEATHER: multiplier keyed by weather condition code (caller/admin supplied - no live weather feed integrated).
INSERT INTO pricing_factors (calculator, vehicle_category, factor_key, factor_value, value_type, description) VALUES
    ('WEATHER', NULL, 'CLEAR', 1.00, 'MULTIPLIER', 'No weather impact'),
    ('WEATHER', NULL, 'RAIN',  1.10, 'MULTIPLIER', 'Light/moderate rain surcharge'),
    ('WEATHER', NULL, 'STORM', 1.25, 'MULTIPLIER', 'Storm/heavy weather surcharge'),
    ('WEATHER', NULL, 'FOG',   1.08, 'MULTIPLIER', 'Fog/low-visibility surcharge');

-- VEHICLE_MULTIPLIER: category comfort/cost multiplier applied to the accumulated ride cost.
INSERT INTO pricing_factors (calculator, vehicle_category, factor_key, factor_value, value_type, description) VALUES
    ('VEHICLE_MULTIPLIER', NULL,        'MULTIPLIER', 1.00, 'MULTIPLIER', 'Default vehicle multiplier'),
    ('VEHICLE_MULTIPLIER', 'HATCHBACK', 'MULTIPLIER', 1.00, 'MULTIPLIER', 'Hatchback multiplier'),
    ('VEHICLE_MULTIPLIER', 'SEDAN',     'MULTIPLIER', 1.15, 'MULTIPLIER', 'Sedan multiplier'),
    ('VEHICLE_MULTIPLIER', 'SUV',       'MULTIPLIER', 1.35, 'MULTIPLIER', 'SUV multiplier'),
    ('VEHICLE_MULTIPLIER', 'MUV',       'MULTIPLIER', 1.45, 'MULTIPLIER', 'MUV/MPV multiplier');

-- SEAT_PRICING: each seat beyond the first is charged at this percent of the per-seat ride cost
-- (a small pooling discount for additional seats on the same booking).
INSERT INTO pricing_factors (calculator, vehicle_category, factor_key, factor_value, value_type, description) VALUES
    ('SEAT_PRICING', NULL, 'ADDITIONAL_SEAT_PERCENT', 90.00, 'PERCENT', 'Price for each seat after the first, as % of per-seat cost');

-- DISCOUNT: an admin-tunable general promo percent, plus an off-peak (late-night) percent banded by minute-of-day.
INSERT INTO pricing_factors (calculator, vehicle_category, factor_key, factor_value, value_type, description) VALUES
    ('DISCOUNT', NULL, 'GENERAL_DISCOUNT_PERCENT', 0.00, 'PERCENT', 'Platform-wide promotional discount, tune via admin API');
INSERT INTO pricing_factors (calculator, vehicle_category, factor_key, factor_value, value_type, range_start, range_end, description) VALUES
    ('DISCOUNT', NULL, 'OFF_PEAK_DISCOUNT_PERCENT', 10.00, 'PERCENT', 0, 300, 'Late-night (00:00-05:00) off-peak discount');

-- COMMISSION: platform commission percent deducted from the driver payout (not added to passenger fare).
INSERT INTO pricing_factors (calculator, vehicle_category, factor_key, factor_value, value_type, description) VALUES
    ('COMMISSION', NULL,        'COMMISSION_PERCENT', 15.00, 'PERCENT', 'Default platform commission'),
    ('COMMISSION', 'HATCHBACK', 'COMMISSION_PERCENT', 12.00, 'PERCENT', 'Commission - hatchback'),
    ('COMMISSION', 'SUV',       'COMMISSION_PERCENT', 18.00, 'PERCENT', 'Commission - SUV'),
    ('COMMISSION', 'MUV',       'COMMISSION_PERCENT', 18.00, 'PERCENT', 'Commission - MUV/MPV');

-- TAX: GST-style tax percent added on top of the discounted fare, charged to the passenger.
INSERT INTO pricing_factors (calculator, vehicle_category, factor_key, factor_value, value_type, description) VALUES
    ('TAX', NULL, 'TAX_PERCENT', 5.00, 'PERCENT', 'Default GST-style tax on fare');

-- Vehicles need a category for VEHICLE_MULTIPLIER/BASE_FARE/DISTANCE bands to select anything
-- other than the global default; auto-populate it from the catalog at registration time
-- (see VehicleService.register), matched on the already-unique (brand, model) pair.
