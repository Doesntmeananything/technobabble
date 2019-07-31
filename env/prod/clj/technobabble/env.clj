(ns technobabble.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[technobabble started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[technobabble has shut down successfully]=-"))
   :middleware identity})
