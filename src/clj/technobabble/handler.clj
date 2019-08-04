(ns technobabble.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [compojure.route :as route]
            [technobabble.layout :refer [error-page]]
            [technobabble.routes.home :refer [home-routes]]
            [technobabble.routes.api :refer [service-routes]]
            [technobabble.env :refer [defaults]]
            [mount.core :as mount]
            [technobabble.middleware :as middleware]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop  ((or (:stop defaults) identity)))

(def app-routes
  (routes
   (-> #'home-routes
       (wrap-routes middleware/wrap-csrf)
       (wrap-routes middleware/wrap-formats))
   #'service-routes
   (route/not-found
    (:body
     (error-page {:status 404
                  :title "page not found"})))))

(defn app [] (middleware/wrap-base #'app-routes))
