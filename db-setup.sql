CREATE USER technobabble WITH PASSWORD 'testdb';
CREATE DATABASE technobabble_dev WITH OWNER technobabble;
CREATE DATABASE technobabble_test WITH OWNER technobabble;
\c technobabble_dev
CREATE EXTENSION "uuid-ossp";
\c technobabble_test
CREATE EXTENSION "uuid-ossp";

