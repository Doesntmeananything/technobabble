-- :name clear-messages! :! :n
-- :doc DELETES ALL MESSAGES!
DELETE FROM messages;

-- :name wipe-database! :! :n
-- :doc DELETES ALL USERS
DELETE FROM users_roles;
DELETE FROM users;
