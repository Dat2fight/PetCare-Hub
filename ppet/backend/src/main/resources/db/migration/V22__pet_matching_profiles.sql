-- Existing pets stay unassessed. No guessed traits or automatic backfill.
ALTER TABLE dbo.pets ADD activity_level VARCHAR(10) NULL,
    suitable_for_apartment BIT NULL,
    good_with_children BIT NULL,
    minimum_daily_care_hours DECIMAL(4,2) NULL;
GO
ALTER TABLE dbo.pets ADD CONSTRAINT CK_pet_matching_activity
    CHECK (activity_level IS NULL OR activity_level IN ('LOW','MEDIUM','HIGH'));
ALTER TABLE dbo.pets ADD CONSTRAINT CK_pet_matching_care_hours
    CHECK (minimum_daily_care_hours IS NULL OR (minimum_daily_care_hours > 0 AND minimum_daily_care_hours <= 24));
