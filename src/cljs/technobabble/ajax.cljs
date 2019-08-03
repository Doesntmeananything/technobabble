(ns technobabble.ajax
  (:require
   [ajax.core :as ajax]
   [luminus-transit.time :as time]
   [cognitect.transit :as transit]
   [re-frame.core :as rf]
   [reagent.session :as session]))

(defn local-uri? [{:keys [uri]}]
  (not (re-find #"^\w+?://" uri)))

(defn default-headers [request]
  (if (local-uri? request)
    (-> request
        (update :headers #(merge {"x-csrf-token" js/csrfToken} %)))
    request))

;; injects transit serialization config into request options
(defn as-transit [opts]
  (merge {:raw             false
          :format          :transit
          :response-format :transit
          :reader          (transit/reader :json time/time-deserialization-handlers)
          :writer          (transit/writer :json time/time-serialization-handlers)}
         opts))

(defn load-interceptors! []
  (swap! ajax/default-interceptors
         conj
         (ajax/to-interceptor {:name "default headers"
                               :request default-headers})))

(defn set-default-opts [opts]
  (-> opts
      (update
       :headers
       #(merge
         {"Accept" "application/transit+json"
          "x-csrf-token" js/csrfToken}
         %))
      (update :error-handler #(or % default-error-handler))))

(defn GET [url opts]
  (session/put! :user-event true)
  (ajax/GET (str js/context url) (set-default-opts opts)))

(defn POST [url opts]
  (session/put! :user-event true)
  (ajax/POST (str js/context url) (set-default-opts opts)))