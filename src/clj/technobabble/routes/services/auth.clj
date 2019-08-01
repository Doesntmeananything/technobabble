(ns technobabble.routes.services.auth
  (:require [technobabble.db.core :as db]
            [technobabble.validation :refer [registration-errors]]
            [ring.util.http-response :as response]
            [buddy.hashers :as hashers]
            [clojure.tools.logging :as log]))

(defn handle-registration-error [e]
  (if (-> e (.getNextException)
          (.getMessage)
          (.startsWith "ERROR: duplicate key value"))
    (response/precondition-failed
     {:result :error
      :message "user with the selected ID already exists"})
    (do
      (log/error e)
      (response/internal-server-error
       {:result :error
        :message "server error occurred while adding the user"}))))

(defn register! [{:keys [session]} user]
  (if (registration-errors user)
    (response/precondition-failed {:result :error})
    (try
      (db/create-user!
       (-> user
           (dissoc :pass-confirm)
           (update :password hashers/encrypt)))
      (-> {:result :ok}
          (response/ok)
          (assoc :session (assoc session :identity (:username user))))
      (catch Exception e
        (handle-registration-error e)))))