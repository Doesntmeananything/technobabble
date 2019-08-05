(ns technobabble.routes.api.message
  (:require [technobabble.db.message :as message]
            [ring.util.http-response :refer [ok unauthorized conflict created
                                             bad-request! not-found forbidden
                                             no-content]]
            [clojure.string :as string]))

(defn save-message
  "Saves a new message"
  [username message refine-id]
  (let [trimmed (string/trim message)]
    (if (not-empty trimmed)
      (let [record (message/create! {:username  username
                                     :message   trimmed
                                     :follow-id refine-id})]
        (created (str "/api/messages/" (:id record))
                 record))
      (bad-request! "Cannot add empty messages"))))

(defn get-message
  "Gets latest messages"
  []
  (if-let [existing (message/get-messages)]
    (ok existing)
    (not-found)))
