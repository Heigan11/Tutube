CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    age DOUBLE NOT NULL CHECK (age >= 0),
    fact_age DOUBLE DEFAULT NULL CHECK (fact_age >= 0),
    level INT DEFAULT 1 CHECK (level >= 1),
    success_rate DOUBLE PRECISION DEFAULT 0.0 CHECK (success_rate >= 0.0 AND success_rate <= 100.0),
    attempts_count INT DEFAULT 0 CHECK (attempts_count >= 0)
);