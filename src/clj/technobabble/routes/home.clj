(ns technobabble.routes.home
  (:require [technobabble.layout :as layout]
            [clojure.core.async :as async]
            [compojure.core :refer [defroutes GET]]
            [chord.http-kit :refer [with-channel]]
            [medley.core :refer [random-uuid]]
            [ring.util.http-response :refer [ok]]))

; Use a transducer to append a unique id to each message
(defonce main-chan (async/chan 1 (map #(assoc % :id (random-uuid)))))

(defonce main-mult (async/mult main-chan))

(def users (atom {}))

(defn ws-handler
  [req]
  (with-channel req ws-ch
    (let [client-tap (async/chan)
          client-id (random-uuid)]
      (async/tap main-mult client-tap)
      (async/go-loop []
        (async/alt!
          client-tap ([message]
                      (if message
                        (do
                          (async/>! ws-ch message)
                          (recur))
                        (async/close! ws-ch)))
          ws-ch ([{:keys [message]}]
                 (if message
                   (let [{:keys [msg m-type]} message]
                     (if (= m-type :new-user)
                       (do
                         (swap! users assoc client-id msg)
                         (async/>! ws-ch  {:id (random-uuid)
                                           :msg @users
                                           :m-type :init-users})
                         (async/>! main-chan (assoc message :msg {client-id (:msg message)})))
                       (async/>! main-chan message))
                     (recur))
                   (do
                     (async/untap main-mult client-tap)
                     (async/>! main-chan {:m-type :user-left
                                          :msg client-id})
                     (swap! users dissoc client-id)))))))))

(defn home-page []
  (layout/render "bootswatch.html"))

(defn about-page []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/ws" [] ws-handler)
  (GET "/about" [] (about-page))
  (GET "/record" [] (home-page))
  (GET "/login" [] (home-page))
  (GET "/signup" [] (home-page))
  (GET "/remember" [] (home-page))
  (GET "/regard" [] (home-page))
  (GET "/thread/*" [] (home-page)))
