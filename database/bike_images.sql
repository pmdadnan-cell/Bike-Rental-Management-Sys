USE bikevault;

ALTER TABLE bikes
    ADD COLUMN IF NOT EXISTS image_path VARCHAR(500) NULL;

UPDATE bikes SET image_path = '/images/bikes/classic350.png' WHERE LOWER(model) LIKE '%classic%';
UPDATE bikes SET image_path = '/images/bikes/hunter350.png' WHERE LOWER(model) LIKE '%hunter%';
UPDATE bikes SET image_path = '/images/bikes/himalayan.png' WHERE LOWER(model) LIKE '%himalayan%' OR LOWER(model) LIKE '%xpulse%';
UPDATE bikes SET image_path = '/images/bikes/meteor350.png' WHERE LOWER(model) LIKE '%meteor%';
UPDATE bikes SET image_path = '/images/bikes/mt15.png' WHERE LOWER(REPLACE(model, '-', '')) LIKE '%mt15%' OR LOWER(model) LIKE '%hornet%' OR LOWER(model) = 'fz';
UPDATE bikes SET image_path = '/images/bikes/r15.png' WHERE LOWER(REPLACE(model, ' ', '')) LIKE '%r15%' OR LOWER(model) LIKE '%rc 200%';
UPDATE bikes SET image_path = '/images/bikes/duke390.png' WHERE LOWER(model) LIKE '%duke 390%';
UPDATE bikes SET image_path = '/images/bikes/duke200.png' WHERE LOWER(model) LIKE '%duke%' AND (image_path IS NULL OR image_path = '');
UPDATE bikes SET image_path = '/images/bikes/apache.png' WHERE LOWER(model) LIKE '%apache%' OR LOWER(model) LIKE '%gixxer%';
UPDATE bikes SET image_path = '/images/bikes/ather450x.png' WHERE LOWER(model) LIKE '%450x%' OR LOWER(model) LIKE '%iqube%';
UPDATE bikes SET image_path = '/images/bikes/activa.png' WHERE LOWER(model) LIKE '%activa%';
UPDATE bikes SET image_path = '/images/bikes/access.png' WHERE LOWER(model) LIKE '%access%';
UPDATE bikes SET image_path = '/images/bikes/aerox.png' WHERE LOWER(model) LIKE '%aerox%';
UPDATE bikes SET image_path = '/images/bikes/ola.png' WHERE LOWER(model) LIKE '%s1%';
UPDATE bikes SET image_path = '/images/bikes/ninja300.png' WHERE LOWER(model) LIKE '%ninja%';
UPDATE bikes SET image_path = '/images/bikes/cb350.png' WHERE LOWER(model) LIKE '%cb350%';
UPDATE bikes SET image_path = '/images/bikes/g310gs.png' WHERE LOWER(model) LIKE '%310 gs%';
UPDATE bikes SET image_path = '/images/bikes/z650.png' WHERE LOWER(model) LIKE '%z650%' OR LOWER(model) LIKE '%dominar%' OR LOWER(model) LIKE '%speed%' OR LOWER(model) LIKE '%scrambler%' OR LOWER(model) LIKE '%310 r%';
UPDATE bikes SET image_path = '/images/bikes/pulsar.png' WHERE LOWER(model) LIKE '%pulsar%' OR LOWER(model) LIKE '%splendor%' OR LOWER(model) LIKE '%raider%' OR LOWER(model) LIKE '%sp 125%';
UPDATE bikes SET image_path = '/images/bikes/mt15.png' WHERE image_path IS NULL OR image_path = '';
