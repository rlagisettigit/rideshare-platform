-- All 7 DayOfWeek names joined with commas ("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY")
-- is 56 characters - VARCHAR(30) from V7 was too narrow for anything past a handful of days.
ALTER TABLE recurring_rides MODIFY COLUMN days_of_week VARCHAR(60) NOT NULL;
