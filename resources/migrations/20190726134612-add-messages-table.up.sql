CREATE TABLE messages
(
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  username VARCHAR(30) NOT NULL,
  message TEXT NOT NULL,
  created TIMESTAMPTZ NOT NULL
);
