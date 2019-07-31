(ns technobabble.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [technobabble.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[technobabble started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[technobabble has shut down successfully]=-"))
   :middleware wrap-dev})
