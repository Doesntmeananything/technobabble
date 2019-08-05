-- :name create-message! :<! :1
-- :doc Creates a new message record
INSERT INTO messages (created, username, message, "root-id", "follow-id")
VALUES (:created, :username, :message, :root-id, :follow-id);

-- :name get-messages :? :*
-- :doc Returns latest messages 
SELECT * FROM messages
ORDER BY created DESC
LIMIT 5;
