(ns technobabble.db.user
  (:require [clojure.string :refer [lower-case]]
            [buddy.hashers :as hashers]
            [technobabble.db.core :refer [*db*] :as db]))

(defn create!
  "Saves a new username, hashing the password as it does so"
  [^String username ^String password]
  (cond
    (empty? password) {:success? false :message "A non-empty password is required"}
    (empty? username) {:success? false :message "A non-empty username is required"}
    :else (try
            (let [encrypted (hashers/encrypt password)
                  record    {:username (lower-case username) :password encrypted}]
              (db/create-user! *db* record)
              (assoc record :success? true))
            (catch Exception e
              {:success? false :message (-> e .getCause .getMessage)}))))

(defn validate
  "Receives a username and password and returns true if they validate to an actual user"
  [^String username ^String password]
  (if (or (empty? username)
          (empty? password))
    false
    (let [record (db/get-user *db* {:username (lower-case username)})]
      (hashers/check password (:password record)))))