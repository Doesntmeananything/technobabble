(ns technobabble.routes.api.memory
  (:require [technobabble.db.memory :as memory]
            [ring.util.http-response :refer [ok unauthorized conflict created
                                             bad-request! not-found forbidden
                                             no-content]]
            [clojure.string :as string]))

(defn save-thought
  "Saves a new thought"
  [username thought refine-id]
  (let [trimmed (string/trim thought)]
    (if (not-empty trimmed)
      (let [record (memory/create! {:username  username
                                    :thought   trimmed
                                    :follow-id refine-id})]
        (created (str "/api/thoughts/" (:id record))
                 record))
      (bad-request! "Cannot add empty thoughts"))))

(defn get-thought
  "Gets a thought by id"
  [username id]
  (if-let [existing (memory/get-if-owner username id)]
    (ok existing)
    (not-found)))
