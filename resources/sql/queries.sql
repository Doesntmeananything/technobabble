-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, pass)
VALUES (:id, :pass)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET id = :id
WHERE id = :id

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE id = :id

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id

-- :name save-message! :! :n
-- creates a new message
INSERT INTO messages
(id, content, written_by, timestamp)
VALUES (:id, :content, :written_by, :timestamp)

-- :name get-messages :? :*
-- selects all available messages
SELECT * from messages 
ORDER BY timestamp
