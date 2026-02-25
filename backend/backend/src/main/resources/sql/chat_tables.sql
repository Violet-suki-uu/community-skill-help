CREATE TABLE IF NOT EXISTS chat_conversation (
  id BIGINT PRIMARY KEY,
  skill_id BIGINT NOT NULL,
  buyer_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  last_message VARCHAR(500) NULL,
  last_message_type VARCHAR(32) NULL,
  last_message_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_skill_buyer_seller (skill_id, buyer_id, seller_id),
  KEY idx_buyer (buyer_id),
  KEY idx_seller (seller_id),
  KEY idx_last_time (last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT PRIMARY KEY,
  conversation_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  content VARCHAR(1000) NULL,
  message_type VARCHAR(32) NOT NULL,
  booking_address VARCHAR(255) NULL,
  booking_phone VARCHAR(32) NULL,
  note VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_conversation_time (conversation_id, created_at),
  KEY idx_sender (sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS reservation (
  id BIGINT PRIMARY KEY,
  skill_id BIGINT NOT NULL,
  buyer_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  address VARCHAR(255) NULL,
  phone VARCHAR(32) NULL,
  note VARCHAR(255) NULL,
  rating INT NULL,
  rating_comment VARCHAR(255) NULL,
  rated_at DATETIME NULL,
  conversation_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_reservation_skill (skill_id),
  KEY idx_reservation_buyer (buyer_id),
  KEY idx_reservation_seller (seller_id),
  KEY idx_reservation_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_rating := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'reservation'
    AND COLUMN_NAME = 'rating'
);
SET @sql_rating := IF(@has_rating = 0,
  'ALTER TABLE reservation ADD COLUMN rating INT NULL',
  'SELECT 1'
);
PREPARE stmt_rating FROM @sql_rating;
EXECUTE stmt_rating;
DEALLOCATE PREPARE stmt_rating;

SET @has_rating_comment := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'reservation'
    AND COLUMN_NAME = 'rating_comment'
);
SET @sql_rating_comment := IF(@has_rating_comment = 0,
  'ALTER TABLE reservation ADD COLUMN rating_comment VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE stmt_rating_comment FROM @sql_rating_comment;
EXECUTE stmt_rating_comment;
DEALLOCATE PREPARE stmt_rating_comment;

SET @has_rated_at := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'reservation'
    AND COLUMN_NAME = 'rated_at'
);
SET @sql_rated_at := IF(@has_rated_at = 0,
  'ALTER TABLE reservation ADD COLUMN rated_at DATETIME NULL',
  'SELECT 1'
);
PREPARE stmt_rated_at FROM @sql_rated_at;
EXECUTE stmt_rated_at;
DEALLOCATE PREPARE stmt_rated_at;

SET @has_skill_lng := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'skill'
    AND COLUMN_NAME = 'lng'
);
SET @sql_skill_lng := IF(@has_skill_lng = 0,
  'ALTER TABLE skill ADD COLUMN lng DECIMAL(10,6) NULL',
  'SELECT 1'
);
PREPARE stmt_skill_lng FROM @sql_skill_lng;
EXECUTE stmt_skill_lng;
DEALLOCATE PREPARE stmt_skill_lng;

SET @has_skill_lat := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'skill'
    AND COLUMN_NAME = 'lat'
);
SET @sql_skill_lat := IF(@has_skill_lat = 0,
  'ALTER TABLE skill ADD COLUMN lat DECIMAL(10,6) NULL',
  'SELECT 1'
);
PREPARE stmt_skill_lat FROM @sql_skill_lat;
EXECUTE stmt_skill_lat;
DEALLOCATE PREPARE stmt_skill_lat;

SET @has_skill_address := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'skill'
    AND COLUMN_NAME = 'address'
);
SET @sql_skill_address := IF(@has_skill_address = 0,
  'ALTER TABLE skill ADD COLUMN address VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE stmt_skill_address FROM @sql_skill_address;
EXECUTE stmt_skill_address;
DEALLOCATE PREPARE stmt_skill_address;

SET @has_skill_adcode := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'skill'
    AND COLUMN_NAME = 'adcode'
);
SET @sql_skill_adcode := IF(@has_skill_adcode = 0,
  'ALTER TABLE skill ADD COLUMN adcode VARCHAR(32) NULL',
  'SELECT 1'
);
PREPARE stmt_skill_adcode FROM @sql_skill_adcode;
EXECUTE stmt_skill_adcode;
DEALLOCATE PREPARE stmt_skill_adcode;

SET @has_skill_city_name := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'skill'
    AND COLUMN_NAME = 'city_name'
);
SET @sql_skill_city_name := IF(@has_skill_city_name = 0,
  'ALTER TABLE skill ADD COLUMN city_name VARCHAR(64) NULL',
  'SELECT 1'
);
PREPARE stmt_skill_city_name FROM @sql_skill_city_name;
EXECUTE stmt_skill_city_name;
DEALLOCATE PREPARE stmt_skill_city_name;

SET @has_skill_view_count := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'skill'
    AND COLUMN_NAME = 'view_count'
);
SET @sql_skill_view_count := IF(@has_skill_view_count = 0,
  'ALTER TABLE skill ADD COLUMN view_count INT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt_skill_view_count FROM @sql_skill_view_count;
EXECUTE stmt_skill_view_count;
DEALLOCATE PREPARE stmt_skill_view_count;

CREATE TABLE IF NOT EXISTS user_event (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  event_type VARCHAR(32) NOT NULL,
  skill_id BIGINT NULL,
  keyword VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_event_user_time (user_id, created_at),
  KEY idx_user_event_type (user_id, event_type),
  KEY idx_user_event_skill (skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
