(ns technobabble.routes.api.auth
  (:require [mylib.auth :as auth]
            [technobabble.config :refer [env]]
            [technobabble.db.user :as user]
            [ring.util.http-response :refer [ok unauthorized conflict created]]))

(defn login
  [id pass]
  (if (user/validate id pass)
    (ok (auth/create-auth-token (:auth-conf env) id))
    (unauthorized "Authentication error")))

(defn signup!
  [id pass]
  (let [result (user/create! id pass)]
    (if (:success? result)
      (created "/" (auth/create-auth-token (:auth-conf env) id))
      (conflict "Invalid username/password combination"))))
