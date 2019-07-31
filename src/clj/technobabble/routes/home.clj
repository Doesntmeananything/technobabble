(ns technobabble.routes.home
  (:require
   [clojure.tools.logging :as log]
   [technobabble.layout :as layout]
   [technobabble.middleware :as middleware]
   [org.httpkit.server :refer [send! with-channel on-close on-receive]]))

(defonce channels (atom #{}))

(defn notify-clients [msg]
  (doseq [channel @channels]
    (send! channel msg)))

(defn connect! [channel]
  (log/info "channel open")
  (swap! channels conj channel))

(defn disconnect! [channel status]
  (log/info "channel closed:" status)
  (swap! channels #(remove #{channel} %)))

(defn ws-handler [request]
  (with-channel request channel
    (connect! channel)
    (on-close channel (partial disconnect! channel))
    (on-receive channel #(notify-clients %))))

(defn home-page [request]
  (layout/render request "home.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf]}
   ["/" {:get home-page}]
   ["/ws" {:get ws-handler}]])
