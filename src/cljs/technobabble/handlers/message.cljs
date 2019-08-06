(ns technobabble.handlers.message
  (:require [ajax.core :refer [GET POST PUT PATCH DELETE]]
            [technobabble.handlers.auth :refer [clear-token-on-unauth]]
            [re-frame.core :refer [dispatch reg-sub reg-event-db subscribe dispatch-sync]]
            [taoensso.timbre :as timbre]
            [technobabble.helpers :as helpers]))

; In theory messages would have this stateful logic

;;; Handlers

(reg-event-db
 :messages-load
 (fn [app-state]
   (GET "/api/search" {:headers       {:authorization (str "Token " (get-in app-state [:credentials :token]))}
                       :handler       #(dispatch [:messages-load-success %])
                       :error-handler #(dispatch [:state-error "Error loading messages" %])})
   (-> app-state
       (assoc [:messages] :last-result))))

(reg-event-db
 :messages-load-success
 (fn [app-state [_ messages]]
   (-> app-state
       (assoc-in [:search-state :last-result] messages))))

(reg-event-db
 :message-save
 (fn [app-state _]
   (let [message (get-in app-state [:messages :current-message])]
     (POST "/api/thoughts" {:params        {:message message :id (get-in app-state [:message :id])}
                            :headers       {:authorization (str "Token " (get-in app-state [:credentials :token]))}
                            :handler       #(dispatch [:message-save-success % message])
                            :error-handler #(dispatch [:state-error "Error saving message" %])}))))

(reg-event-db
 :memory-save-success
 (fn [app-state [_ result msg]]
   (dispatch [:state-message (str "Saved: " msg) "alert-success"])
   (thread/reload-if-cached app-state (:root-id result))
   (-> app-state
       (assoc-in [:messages :current-message] ""))))
