(ns technobabble.components.registration
  (:require [reagent.core :refer [atom]]
            [reagent.session :as session]
            [technobabble.ajax :as ajax]
            [technobabble.components.common :as c]
            [technobabble.validation :refer [registration-errors]]))

(defn register! [fields errors]
  (reset! errors (registration-errors @fields))
  (when-not @errors
    (ajax/POST "/api/auth/register"
      {:params @fields
       :handler
       #(do
          (session/put! :identity (:username @fields))
          (reset! fields {})
          (session/remove! :modal))
       :error-handler
       #(reset! errors {:server-error (get-in % [:response :message])})})))

;START:delete-account
(defn delete-account! []
  (ajax/POST "/delete-account"
    {:handler #(do
                 (session/remove! :identity)
                 (session/put! :page :home))}))
;END:delete-account

;START:delete-account-modal
(defn delete-account-modal []
  (fn []
    [c/modal
     [:h2.alert.alert-danger "Delete Account!"]
     [:P "Are you sure you wish to delete the account and associated gallery?"]
     [:div
      [:button.btn.btn-primary
       {:on-click (fn []
                    (delete-account!)
                    (session/remove! :modal))}
       "Delete"]
      [:button.btn.btn-danger
       {:on-click (fn [] (session/remove! :modal))}
       "Cancel"]]]))
;END:delete-account-modal

(defn registration-form []
  (let [fields (atom {})
        error  (atom nil)]
    (fn []
      [c/modal
       [:div "Picture Gallery Registration"]
       [:div
        [:div.well.well-sm
         [:strong "✱ required field"]]
        [c/text-input "name" :username "enter a user name" fields]
        (when-let [error (first (:username @error))]
          [:div.alert.alert-danger error])
        [c/password-input "password" :password "enter a password" fields]
        (when-let [error (first (:password @error))]
          [:div.alert.alert-danger error])
        [c/password-input "password" :password-confirm "re-enter the password" fields]
        (when-let [error (:server-error @error)]
          [:div.alert.alert-danger error])]
       [:div
        [:button.btn.btn-primary
         {:on-click #(register! fields error)}
         "Register"]
        [:button.btn.btn-danger
         {:on-click #(session/remove! :modal)}
         "Cancel"]]])))

(defn registration-button []
  [:a.btn
   {:on-click #(session/put! :modal registration-form)}
   "register"])
