(ns technobabble.ws-chat
  (:require
   [chord.client :refer [ws-ch]]
   [cljsjs.react-bootstrap]
   [cljs.core.async :as async :include-macros true]
   [reagent.core :as reagent :refer [atom]]
   [re-frame.core :refer [dispatch reg-sub reg-event-db subscribe dispatch-sync]]))

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
                  :on-change #(reset! v (-> % .-target .-value))}]]
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