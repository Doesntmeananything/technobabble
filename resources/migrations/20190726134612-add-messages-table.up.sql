CREATE TABLE messages
(id VARCHAR(20) PRIMARY KEY,
 content VARCHAR(200),
 written_by VARCHAR(20) REFERENCES users (id),
 timestamp TIMESTAMP);
