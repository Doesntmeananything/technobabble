(ns technobabble.db.memory
  (:require [technobabble.config :refer [env]]
            [clojure.string :as s]
            [technobabble.db.core :refer [*db*] :as db]
            [technobabble.misc.html :refer [remove-html clean-memory-text]]
            [clojure.java.jdbc :as jdbc]
            [clj-time.coerce :as c]
            [clj-time.core :as t])
  (:import (java.util Date UUID)))

(defn now [] (Date.))

(defn get-by-id
  "Loads a memory by its id and adds a status"
  [^UUID id]
  (db/get-thought-by-id *db* {:id id}))

(defn get-if-owner
  "Returns a thought if owned by a user, or nil otherwise"
  [username id]
  (let [existing (get-by-id id)]
    (when (= username (:username existing))
      existing)))

(defn create!
  "Saves a new memory, after removing HTML tags from the thought."
  [memory]
  (jdbc/with-db-transaction
    [trans-conn *db*]
    (let [refine-id (:follow-id memory)
          refined   (if refine-id (db/get-thought-by-id trans-conn {:id refine-id}))
          root-id   (or (:root-id refined) refine-id)
          item      (clean-memory-text
                     (assoc memory :created (now)
                            :username (s/lower-case (:username memory))
                            :follow-id refine-id
                            :root-id root-id))]
      (if refined
        (db/make-root! trans-conn {:id root-id}))
      (db/create-thought! trans-conn item))))

