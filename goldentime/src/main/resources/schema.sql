CREATE SCHEMA IF NOT EXISTS user_management;
CREATE SCHEMA IF NOT EXISTS goldentime;

-- user_management schema tables
CREATE TABLE IF NOT EXISTS user_management.TB_USER (
    user_id SERIAL PRIMARY KEY,
    login_id VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_management.TB_USER_VEHICLE (
    vehicle_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES user_management.TB_USER(user_id),
    car_number VARCHAR(20) NOT NULL UNIQUE,
    serial_number VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_management.TB_NOTICE (
    notice_id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    view_count INT NOT NULL DEFAULT 0,
    image_path VARCHAR(500) UNIQUE,
    important BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE user_management.TB_NOTICE ADD COLUMN IF NOT EXISTS important BOOLEAN NOT NULL DEFAULT FALSE;

-- goldentime schema tables
CREATE TABLE IF NOT EXISTS goldentime.TB_GT_EVENT (
    gt_id SERIAL PRIMARY KEY,
    vehicle_id INT NOT NULL REFERENCES user_management.TB_USER_VEHICLE(vehicle_id),
    video_path VARCHAR(500) UNIQUE,
    vt_id_path VARCHAR(500),
    sent_to_fire BOOLEAN,
    sent_to_safety BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS goldentime.TB_GT_OCR (
    gt_id INT PRIMARY KEY REFERENCES goldentime.TB_GT_EVENT(gt_id),
    detected_plate VARCHAR(20),
    confidence FLOAT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
