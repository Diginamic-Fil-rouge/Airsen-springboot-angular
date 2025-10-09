-- Reset auto-increment IDs to start from 1
-- Run this ONLY after deleting all data from tables

-- WARNING: This will fail if there is existing data with IDs
-- Make sure to delete all rows first with TRUNCATE or DELETE

ALTER TABLE air_quality AUTO_INCREMENT = 1;
ALTER TABLE alerts AUTO_INCREMENT = 1;
ALTER TABLE alert_history AUTO_INCREMENT = 1;
ALTER TABLE communes AUTO_INCREMENT = 1;
ALTER TABLE departments AUTO_INCREMENT = 1;
ALTER TABLE export_requests AUTO_INCREMENT = 1;
ALTER TABLE forum_categories AUTO_INCREMENT = 1;
ALTER TABLE forum_messages AUTO_INCREMENT = 1;
ALTER TABLE forum_thread AUTO_INCREMENT = 1;
ALTER TABLE forum_votes AUTO_INCREMENT = 1;
ALTER TABLE notifications AUTO_INCREMENT = 1;
ALTER TABLE regions AUTO_INCREMENT = 1;
ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE weather_data AUTO_INCREMENT = 1;
