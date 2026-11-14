-- Apply once to an existing BakeNest database if reviews.created_at is missing.
-- Existing review dates remain NULL because their original dates are unknown.
ALTER TABLE reviews ADD COLUMN created_at DATETIME(6) NULL;
