MERGE INTO users (user_name, password, first_name, last_name) KEY(user_name)
VALUES
('test@example.com', '$2a$10$B4g6ADFynZyeFhUCAAWXS.d52Qkx1dhqhDze4fAgRozLV/i1cRfMO', 'Test', 'User'),
('testUser@test.com', '$2a$10$B4g6ADFynZyeFhUCAAWXS.d52Qkx1dhqhDze4fAgRozLV/i1cRfMO', 'Test', 'User');