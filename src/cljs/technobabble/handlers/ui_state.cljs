(ns technobabble.handlers.ui-state
  (:require [ajax.core :refer [GET POST PUT]]
            [technobabble.handlers.auth :refer [clear-token-on-unauth]]
            [re-frame.core :refer [dispatch reg-sub reg-event-db subscribe dispatch-sync]]
            [taoensso.timbre :as timbre]))

(reg-event-db
 :state-credentials
 (fn [app-state [_ k v]]
   (assoc-in app-state [:credentials k] v)))

; Potential message handling

(reg-event-db
 :state-ui-section
 (fn [app-state [_ section]]
   (timbre/trace :state-ui-section section)
   (condp = section
    ;  Dispatch event to fetch latest messages
    ;  :chat (dispatch [:message-load])
     nil)
    ; Do not associate nil sections
   (if (some? section)
     (assoc-in app-state [:ui-state :section] section)
     app-state)))

(reg-event-db
 :state-message
 (fn [app-state [_ msg class]]
   (let [message {:text msg :class class}]
     (if (= class "alert-success")
       (js/setTimeout #(dispatch [:state-message-if-same message nil]) 3000))
     (assoc-in app-state [:ui-state :last-message] message))))

(reg-event-db
 :state-message-if-same
 (fn [app-state [_ compare-msg new-msg]]
   (if (= compare-msg (get-in app-state [:ui-state :last-message]))
     (assoc-in app-state [:ui-state :last-message] new-msg)
     app-state)))

(reg-event-db
 :state-error
 (fn [app-state [_ message result]]
   (timbre/error message result)
   (dispatch [:state-message (str message ": " result) "alert-danger"])
   (clear-token-on-unauth result)
   (assoc-in app-state [:ui-state :is-busy?] false)))