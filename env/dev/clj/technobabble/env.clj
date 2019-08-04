(ns technobabble.env
  (:require [selmer.parser :as parser]
            [taoensso.timbre :as timbre]
            [technobabble.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init       (fn []
                 (parser/cache-off!)
                 (timbre/info "\n-=[technobabble started successfully using the development profile]=-"))
   :stop       (fn []
                 (timbre/info "\n-=[technobabble has shut down successfully]=-"))
   :middleware wrap-dev})
