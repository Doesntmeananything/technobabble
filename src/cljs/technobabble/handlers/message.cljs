(ns technobabble.handlers.message
  (:require [ajax.core :refer [GET POST PUT PATCH DELETE]]
            [technobabble.handlers.auth :refer [clear-token-on-unauth]]
            [re-frame.core :refer [dispatch reg-sub reg-event-db subscribe dispatch-sync]]
            [taoensso.timbre :as timbre]
            [technobabble.helpers :as helpers]))

;;; Handlers

(reg-event-db
 :message-archive
 (fn [app-state [_ message archived?]]
   (let [url (str "/api/thoughts/" (:id message) "/archive")]
     (PUT url {:params        {:archived? archived?}
               :headers       {:authorization (str "Token " (get-in app-state [:credentials :token]))}
               :handler       #(dispatch [:message-archive-success message])
               :error-handler #(dispatch [:state-error "Error changing archive state" %])}))
   (assoc-in app-state [:ui-state :is-busy?] true)))

(reg-event-db
 :memories-load
 (fn [app-state [_ page-index]]
   (let [q         (get-in app-state [:ui-state :current-query])
         last-q    (get-in app-state [:search-state :query])
         all?      (or (get-in app-state [:ui-state :query-all?]) false)
         last-all? (get-in app-state [:search-state :all?])
         same?     (and (= q last-q)
                        (= all? last-all?))
         list      (if same?
                     (get-in app-state [:search-state :list])
                     [])
         p         (or page-index (get-in app-state [:ui-state :results-page]))]
     (if (or (not same?)
             (> p (or (get-in app-state [:search-state :page-index]) -1)))
       (do (GET "/api/search" {:params        {:q q :page p :all? all?}
                               :headers       {:authorization (str "Token " (get-in app-state [:credentials :token]))}
                               :handler       #(dispatch [:memories-load-success %])
                               :error-handler #(dispatch [:state-error "Error remembering" %])})
           (-> app-state
               (assoc-in [:ui-state :is-searching?] true)
               (assoc :search-state {:query       q
                                     :page-index  p
                                     :all?        all?
                                     :list        list
                                     :last-result (get-in app-state [:search-state :last-result])})))
       app-state))))

(reg-event-db
 :memories-load-next
 (fn [app-state [_]]
   (dispatch [:memories-load (inc (get-in app-state [:search-state :page-index]))])
   app-state))

(reg-event-db
 :memories-load-success
 (fn [app-state [_ memories]]
   (-> app-state
       (assoc-in [:search-state :list] (concat (get-in app-state [:search-state :list]) (helpers/add-html-to-thoughts (:results memories))))
       (assoc-in [:search-state :last-result] memories)
       (assoc-in [:ui-state :is-searching?] false))))

(reg-event-db
 :message-edit-set
 (fn [app-state [_ thought]]
   (if (empty? thought)
     (dispatch [:state-note :edit-note nil]))
   (assoc-in app-state [:note :edit-message] thought)))

(reg-event-db
 :message-edit-save
 (fn [app-state _]
   (let [note   (get-in app-state [:note :edit-note])
         message (get-in app-state [:note :edit-message])
         url    (str "/api/thoughts/" (:id message))]
     (PATCH url {:params        {:thought note}
                 :headers       {:authorization (str "Token " (get-in app-state [:credentials :token]))}
                 :handler       #(dispatch [:message-edit-save-success message note])
                 :error-handler #(dispatch [:state-error "Error editing message" %])}))
   (assoc-in app-state [:ui-state :is-busy?] true)))

(reg-event-db
 :message-forget
 (fn [app-state [_ message]]
   (let [url (str "/api/thoughts/" (:id message))]
     (DELETE url {:headers       {:authorization (str "Token " (get-in app-state [:credentials :token]))}
                  :handler       #(dispatch [:message-forget-success message %])
                  :error-handler #(dispatch [:state-error "Error forgetting thought" %])}))
   app-state))

(reg-event-db
 :message-save
 (fn [app-state _]
   (let [note (get-in app-state [:note :current-note])]
     (POST "/api/thoughts" {:params        {:thought note :follow-id (get-in app-state [:note :focus :id])}
                            :headers       {:authorization (str "Token " (get-in app-state [:credentials :token]))}
                            :handler       #(dispatch [:message-save-success % note])
                            :error-handler #(dispatch [:state-error "Error saving thought" %])}))
   (assoc-in app-state [:ui-state :is-busy?] true)))
