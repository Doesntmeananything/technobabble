(ns technobabble.core
  (:require
   [bidi.bidi :as bidi]
   [day8.re-frame.http-fx]
   [reagent.core :as reagent :refer [atom]]
   [re-frame.core :refer [dispatch reg-sub reg-event-db subscribe dispatch-sync clear-subscription-cache!]]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [technobabble.ajax :as ajax]
   [technobabble.events]
   [technobabble.handlers.auth :refer [clear-token-on-unauth]]
   [technobabble.handlers.ui-state]
   [technobabble.handlers.routing :as r]
   [technobabble.helpers :as helpers :refer [<sub]]
   [reitit.core :as reitit]
   [clojure.string :as string]
   [chord.client :refer [ws-ch]]
   [cljs.core.async :as async :include-macros true])
  (:import goog.History)
  (:require-macros [reagent.ratom :refer [reaction]]))


;;;;------------------------------
;;;; Queries
;;;;------------------------------


(defn general-query
  [db [sid element-id]]
  (get-in db [sid element-id]))

(reg-sub :note general-query)
(reg-sub :cache general-query)
(reg-sub :ui-state general-query)
(reg-sub :search-state general-query)
(reg-sub :credentials general-query)



;;;;------------------------------
;;;; Handlers
;;;;------------------------------


(defn dispatch-on-press-enter [e d]
  (when (= 13 (.-which e))
    (dispatch d)))

(reg-event-db
 :initialize
 (fn [app-state _]
   (merge app-state {:ui-state {:is-busy?        false
                                :wip-login?      false
                                :show-thread?    false
                                :section         :record
                                :results-page    0
                                :memories        {:pages 0}
                                :is-searching?   false}
                     :cache    {}                          ; Will be used for caching threads and reminders
                     :note     {:edit-memory nil}})))

(goog-define ws-url "ws://localhost:3000/ws")

(defonce app-state (atom {:text "Default text"
                          :active-panel :login
                          :user "Default user"}))

(defonce users (atom {}))
(defonce msg-list (atom []))

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
  (let [v (atom nil)]
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
  (reagent/create-class
   {:render (fn []
              [:div {:class "chat-history"}
               (for [m @msg-list]
                 ^{:key (:id m)} [:p (str (:user m) ": " (:msg m))])])
    :component-did-update (fn [this]
                            (let [node (reagent/dom-node this)]
                              (set! (.-scrollTop node) (.-scrollHeight node))))}))

(defn login-view []
  (let [v (atom nil)]
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

(defn login-form []
  (let [username  (<sub [:credentials :username])
        password  (<sub [:credentials :password])
        confirm   (<sub [:credentials :password-confirm])
        message   (<sub [:credentials :message])
        section   (<sub [:ui-state :section])
        signup?   (reaction (= :signup section))
        u-class   (reaction (if (and @signup? (empty? username)) " has-error"))
        pw-class  (reaction (if (and @signup? (> 7 (count password))) " has-error"))
        pw2-class (reaction (if (not= password confirm) " has-error"))]
    [:div {:class "field"}
     [:div {:class "modal-dialog"}
      [:div {:class "modal-content"}
       [:div {:class "modal-header"}
        [:h4 {:class "modal-title"} "Login"]]
       [:div {:class "modal-body"}
        (when message
          [:div {:class (str "col-lg-12 alert " (:type message))}
           [:p (:text message)]])
        [:div {:class (str "form-group" @u-class)}
         [:label {:for "inputLogin" :class "col-sm-2 control-label"} "Username"]
         [:div {:class "col-sm-10"}
          [:input {:type         "text"
                   :class        "formControl col-sm-8"
                   :id           "inputLogin"
                   :placeholder  "user name"
                   :on-change    #(dispatch-sync [:state-credentials :username (-> % .-target .-value)])
                   :on-key-press #(dispatch-on-press-enter % [:auth-request @signup?])
                   :value        username}]]]
        [:div {:class (str "form-group" @pw-class)}
         [:label {:for "inputPassword" :class "col-sm-2 control-label"} "Password"]
         [:div {:class "col-sm-10"}
          [:input {:type         "password"
                   :class        "formControl col-sm-8"
                   :id           "inputPassword"
                   :on-change    #(dispatch-sync [:state-credentials :password (-> % .-target .-value)])
                   :on-key-press #(dispatch-on-press-enter % [:auth-request @signup?])
                   :value        password}]]]
        (if @signup?
          [:div {:class (str "form-group" @pw2-class)}
           [:label {:for "inputPassword2" :class "col-sm-2 col-lg-2 control-label"} "Confirm:"]
           [:div {:class "col-sm-10 col-lg-10"}
            [:input {:type         "password"
                     :class        "formControl col-sm-8 col-lg-8"
                     :id           "inputPassword2"
                     :on-change    #(dispatch-sync [:state-credentials :password2 (-> % .-target .-value)])
                     :on-key-press #(dispatch-on-press-enter % [:auth-request @signup?])
                     :value        confirm}]]])]
       [:div {:class "modal-footer"}
        [:button {:type "button" :class "btn btn-primary" :disabled (<sub [:ui-state :wip-login?]) :on-click #(dispatch [:auth-request @signup?])} "Submit"]]]]]))

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
    :login [login-form]
    :chat [chat-view]))

(defn navbar-item
  "Renders a navbar item. Having each navbar item have its own subscription will probably
  have a bit of overhead, but I don't imagine it'll be anything major since we won't have
  more than a couple of them.

  It will use the section id to get the route to link to."
  [label section]
  (let [current     (subscribe [:ui-state :section])
        is-current? (reaction (= section @current))
        class       (when @is-current? "active")]
    [:a.navbar-item {:class    class
                     :eventKey section
                     :href     (bidi/path-for r/routes section)}
     label
     (when @is-current?
       [:span {:class "sr-only"} "(current)"])]))

(defn navbar []
  (reagent/with-let [expanded? (atom false)]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "Technobabble"]]
     [:div.navbar-end
      (if (nil? (<sub [:credentials :token]))
        [:div [navbar-item "Login" :login]
         [navbar-item "Sign up" :signup]]
        [:div
         [navbar-item "Record" :record]])]]))

(defn home-page []
  [app-container])

(def pages
  {:home #'home-page})

(defn page []
  [:div
   [navbar]
   [(pages @(subscribe [:page]))]])

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
         (dispatch
          [:navigate (reitit/match-by-path router uri)]))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-components []
  (clear-subscription-cache!)
  (reagent/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (dispatch-sync [:navigate (reitit/match-by-name router :home)])

  (ajax/load-interceptors!)
  (hook-browser-navigation!)
  (mount-components))
