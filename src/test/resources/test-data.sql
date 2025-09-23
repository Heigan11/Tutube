
--INSERT INTO users (user_name, password, first_name, last_name)
MERGE INTO users (user_name, password, first_name, last_name) KEY(user_name)
VALUES
('testUser', '$2a$10$X8fB3Q7TzJk1w2Vl5nM6EeZ8gC1dH2F4A7B0c3D9e5F8gH1iJ3k5L7M9', 'Test', 'User'),
('admin', '$2a$10$X8fB3Q7TzJk1w2Vl5nM6EeZ8gC1dH2F4A7B0c3D9e5F8gH1iJ3k5L7M9', 'Admin', 'User');