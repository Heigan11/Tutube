CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    age DOUBLE,
    fact_age DOUBLE,
    level INT DEFAULT 1,
    success_rate DOUBLE PRECISION DEFAULT 0.0,
    attempts_count INT DEFAULT 0
);