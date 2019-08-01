(ns technobabble.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [technobabble.ajax :as ajax]
   [technobabble.events]
   [reitit.core :as reitit]
   [clojure.string :as string]
   [chord.client :refer [ws-ch]]
   [cljs.core.async :as async :include-macros true])
  (:import goog.History))

(goog-define ws-url "ws://localhost:3000/ws")

(defonce app-state (r/atom {:text "Default text"
                            :active-panel :login
                            :user "Default user"}))

(defonce users (r/atom {}))
(defonce msg-list (r/atom []))

(defonce send-chan (async/chan))

(defn send-msg
  [msg]
  (async/put! send-chan msg))

(defn send-msgs
  [svr-chan]
  (async/go-loop []
    (when-let [msg (async/<! send-chan)]
      (async/>! svr-chan msg)
      (recur))))

(defn receive-msgs
  [svr-chan]
  (async/go-loop []
    (if-let [new-msg (:message (async/<! svr-chan))]
      (do
        (case (:m-type new-msg)
          :init-users (reset! users (:msg new-msg))
          :chat (swap! msg-list conj (dissoc new-msg :m-type))
          :new-user (swap! users merge (:msg new-msg))
          :user-left (swap! users dissoc (:msg new-msg)))
        (recur))
      (println "Websocket closed"))))

(defn setup-websockets! []
  (async/go
    (let [{:keys [ws-channel error]} (async/<! (ws-ch ws-url))]
      (if error
        (println "Something went wrong with the websocket")
        (do
          (send-msg {:m-type :new-user
                     :msg (:user @app-state)})
          (send-msgs ws-channel)
          (receive-msgs ws-channel))))))

(defn chat-input []
  (let [v (r/atom nil)]
    (fn []
      [:div {:class "chat-input"}
       [:form
        {:on-submit (fn [x]
                      (.preventDefault x)
                      (when-let [msg @v] (send-msg {:msg msg
                                                    :user (:user @app-state)
                                                    :m-type :chat}))
                      (reset! v nil))}
        [:div {:class "chat-input-form"}
         [:input {:type "text"
                  :class "chat-input-field"
                  :value @v
                  :placeholder "Type a message"
                  :on-change #(reset! v (-> % .-target .-value))}]
         [:span {:class "chat-input-span"}]
         [:br]]]])))

(defn chat-history []
  (r/create-class
   {:render (fn []
              [:div {:class "chat-history"}
               (for [m @msg-list]
                 ^{:key (:id m)} [:p (str (:user m) ": " (:msg m))])])
    :component-did-update (fn [this]
                            (let [node (r/dom-node this)]
                              (set! (.-scrollTop node) (.-scrollHeight node))))}))

(defn login-view []
  (let [v (r/atom nil)]
    (fn []
      [:div {:class "login-container"}
       [:form
        {:on-submit (fn [x]
                      (.preventDefault x)
                      (swap! app-state assoc :user @v)
                      (swap! app-state assoc :active-panel :chat)
                      (setup-websockets!))}
        [:div {:class "login-input-container"}
         [:input {:type "text"
                  :class "login-input"
                  :value @v
                  :placeholder "Username"
                  :on-change #(reset! v (-> % .-target .-value))}]
         [:span {:class "bottom"}]
         [:span {:class "right"}]
         [:span {:class "top"}]
         [:span {:class "left"}]]
        [:br]
        [:button {:type "submit"
                  :class "login-button"} "Start chatting!"]]])))

(defn sidebar []
  [:div {:class "sidebar"}
   (into [:ul]
         (for [[k v] @users]
           ^{:key k} [:li v]))])

(defn chat-view []
  [:div {:class "chat-view"}
   [chat-history]
   [chat-input]
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
  (mount-components))
