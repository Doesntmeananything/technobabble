-- :name create-user! :! :n
-- :doc Creates a new user record. The password is expected to be bcrypt+sha512,
--  with maximum length of 162 characters, so don't use longer salts than
--  hashers' default.
INSERT INTO users (username, password) VALUES (:username, :password);

-- :name get-user :? :1
-- :doc Looks for a user record based on the username.
SELECT * FROM users WHERE username = :username;

-- :name create-message! :<! :1
-- :doc Creates a new message record
INSERT INTO messages (created, username, message)
VALUES (:created, :username, :message)
RETURNING *;


-- :name get-message-by-id :? :1
-- :doc Returns the messages matching an id (which should be only one)
SELECT username, message FROM messages
ORDER BY created DESC
LIMIT 5;
