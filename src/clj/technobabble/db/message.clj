(ns technobabble.db.message
  (:require [technobabble.config :refer [env]]
            [clojure.string :as s]
            [technobabble.db.core :refer [*db*] :as db]
            [technobabble.misc.html :refer [remove-html clean-message-text]]
            [clojure.java.jdbc :as jdbc]
            [clj-time.coerce :as c]
            [clj-time.core :as t])
  (:import (java.util Date UUID)))

(defn now [] (Date.))

(defn get-messages
  "Loads the latest messages"
  []
  (db/get-messages *db*))

(defn create!
  "Saves a new message, after removing HTML tags from the thought."
  [message]
  (jdbc/with-db-transaction
    [trans-conn *db*]
    (let [item      (clean-message-text
                     (assoc message :created (now)
                            :username (s/lower-case (:username message))))]
      (db/create-message! trans-conn item))))

