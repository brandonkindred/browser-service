-- The browser session `environment` (TEST/DISCOVERY) was a Look-see relic that never
-- influenced driver, capability, or grid selection. Drop the now-unused column.
ALTER TABLE browser_sessions DROP COLUMN environment;
