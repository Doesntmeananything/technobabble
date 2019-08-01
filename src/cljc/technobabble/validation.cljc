(ns technobabble.validation
  (:require [struct.core :as st]
            [bouncer.core :as b]
            [bouncer.validators :as v]))

(defn registration-errors [{:keys [password-confirm] :as params}]
  (first
   (b/validate
    params
    :username v/required
    :password [v/required
               [v/min-count 7 :message "Password must contain at least 7 characters"]
               [= password-confirm :message "Re-entered password does not match"]])))

