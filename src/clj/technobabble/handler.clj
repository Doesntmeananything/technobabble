(ns technobabble.handler
  (:require
   [technobabble.middleware :as middleware]
   [technobabble.layout :refer [error-page]]
   [technobabble.routes.home :refer [home-routes]]
   [technobabble.routes.services :refer [service-routes]]
   [technobabble.env :refer [defaults]]
   [mount.core :as mount]
   [compojure.core :refer [routes wrap-routes]]
   [compojure.route :as route]))

(mount/defstate init-app
  :start ((or (:init defaults) (fn [])))
  :stop  ((or (:stop defaults) (fn []))))

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

