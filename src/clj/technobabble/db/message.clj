(ns technobabble.db.message
  (:require [clojure.string :as s]
            [technobabble.db.core :refer [*db*] :as db]
            [clojure.java.jdbc :as jdbc])
  (:import (java.util Date UUID)))

(defn now [] (Date.))

(defn get-messages
  "Loads most recent messages"
  []
  (db/get-messages))

(defn create-message!
  "Saves a new message"
  [message]
  (jdbc/with-db-transaction
    [trans-conn *db*]
    (let [item
          (assoc message :created (now)
                 :username (s/lower-case (:username message)))]
      (db/create-message! trans-conn item))))
