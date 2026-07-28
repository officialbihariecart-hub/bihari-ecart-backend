-- Migration script to add average_rating and review_count columns to products table
-- and backfill existing data from reviews table.

-- 1. Add new columns if they do not exist
ALTER TABLE products ADD COLUMN IF NOT EXISTS average_rating DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS review_count INTEGER DEFAULT 0;

-- 2. Update NULL values to default values
UPDATE products SET average_rating = 0.0 WHERE average_rating IS NULL;
UPDATE products SET review_count = 0 WHERE review_count IS NULL;

-- 3. Backfill existing review statistics into products table
WITH review_stats AS (
    SELECT 
        product_id,
        ROUND(AVG(rating)::numeric, 1) AS avg_rating,
        COUNT(id) AS total_count
    FROM reviews
    GROUP BY product_id
)
UPDATE products p
SET 
    average_rating = rs.avg_rating,
    review_count = rs.total_count
FROM review_stats rs
WHERE p.id = rs.product_id;
