CREATE TABLE messages
(
  id UUID PRIMARY KEY,
  username VARCHAR(30) NOT NULL,
  message TEXT NOT NULL,
  created TIMESTAMPTZ NOT NULL
);