-- The browser session `environment` (TEST/DISCOVERY) was a Look-see relic that never
-- influenced driver, capability, or grid selection, and application code no longer reads
-- or writes it (BrowserSessionEntity no longer maps the column).
--
-- This is the backward-compatible ("expand") half of an expand/contract change: relaxing
-- NOT NULL lets both revisions coexist during a rolling deploy — the previous revision can
-- still INSERT an `environment` value while the new revision omits it. The column itself is
-- dropped in a later release (a follow-up `DROP COLUMN environment`), once no old revision
-- that still writes it can be running.
ALTER TABLE browser_sessions ALTER COLUMN environment DROP NOT NULL;
