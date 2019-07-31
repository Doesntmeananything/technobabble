(ns technobabble.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [technobabble.ajax :as ajax]
   [technobabble.events]
   [technobabble.websockets :as ws]
   [reitit.core :as reitit]
   [clojure.string :as string])
  (:import goog.History))

(defonce app-state (r/atom {:text "Default text"
                            :active-panel :login
                            :user "Default user"}))

(defonce users (r/atom {}))
(defonce messages (r/atom []))

(defn login-view []
  (let [value (r/atom nil)]
    (fn []
      [:div {:class "login-container"}
       [:div {:class "login"}
        [:form
         {:on-submit (fn [x]
                       (.preventDefault x)
                       (swap! app-state assoc :user @value)
                       (swap! app-state assoc :active-panel :chat)
                       (ws/send-transit-msg!
                        {:user @value}))}
         [:input {:type "text"
                  :class "input is-primary"
                  :value @value
                  :placeholder "Pick a username"
                  :on-change #(reset! value (-> % .-target .-value))}]
         [:br]
         [:button {:type "submit"
                   :class "button is-primary"} "Start chatting"]]]])))

(defn message-list []
  [:div {:class "message-list"}
   [:ul
    (map-indexed
     (fn [id message]
       ^{:key id}
       [:li message])
     @messages)]])

(defn message-input []
  (r/with-let [value (r/atom nil)]
    [:input.form-control
     {:type        "text"
      :class       "text-input"
      :placeholder "type in a message"
      :value       @value
      :on-change   #(reset! value (-> % .-target .-value))
      :on-key-down #(when (= (.-keyCode %) 13)
                      (ws/send-transit-msg!
                       {:message @value :user (:user @app-state)})
                      (reset! value nil))}]))

(defn sidebar []
  [:div {:class "sidebar"}
   [:h5 "Active Users:"]
   (into [:ul]
         (for [[k v] @users]
           ^{:key k} [:li v]))])

(defn chat-view []
  [:div {:class "chat-view"}
   [message-list]
   [message-input]
   [:div {:class "header"}
    [:h3 "chat room"]]
   [sidebar]])

(defn app-container
  []
  (case (:active-panel @app-state)
    :login [login-view]
    :chat [chat-view]))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:page])) :is-active)}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "technobabble"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(swap! expanded? not)
        :class (when @expanded? :is-active)}
       [:span] [:span] [:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "#/" "Home" :home]]]]))

(defn home-page []
  [app-container])

(defn update-messages! [{:keys [message user]}]
  (swap! messages #(vec (take 100 (conj % message))))
  (swap! users assoc :active-user user))

(def pages
  {:home #'home-page})

(defn page []
  [:div
   [navbar]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes

(def router
  (reitit/router
   [["/" :home]]))

;; -------------------------
;; History
;; must be called after routes have been defined


(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     HistoryEventType/NAVIGATE
     (fn [event]
       (let [uri (or (not-empty (string/replace (.-token event) #"^.*#" "")) "/")]
         (rf/dispatch
          [:navigate (reitit/match-by-path router uri)]))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:navigate (reitit/match-by-name router :home)])

  (ajax/load-interceptors!)
  (hook-browser-navigation!)
  (ws/make-websocket! (str "ws://" (.-host js/location) "/ws") update-messages!)
  (mount-components))
