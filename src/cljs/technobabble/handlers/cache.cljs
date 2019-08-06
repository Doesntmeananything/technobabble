(ns technobabble.handlers.cache
  (:require [ajax.core :refer [GET POST PUT PATCH]]
            [re-frame.core :refer [dispatch reg-sub reg-event-db subscribe dispatch-sync]]
            [taoensso.timbre :as timbre]))

(reg-event-db
 :cache-add-message
 (fn [app-state [_ message]]
    ;; Looks for the id associated with the message, and add it
   (let [fn-add-message (fn [e] (if (= (:message-id e) (:id message))
                                  (assoc e :message-record message)
                                  e))]
     (assoc-in app-state [:cache :messages]
               (map fn-add-message (get-in app-state [:cache :messages]))))))