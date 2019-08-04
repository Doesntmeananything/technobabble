(ns user
  (:require [mount.core :as mount]
            [technobabble.figwheel :refer [start-fw stop-fw cljs]]
            technobabble.core))

(defn start []
  (mount/start-without #'technobabble.core/repl-server))

(defn stop []
  (mount/stop-except #'technobabble.core/repl-server))

(defn restart []
  (stop)
  (start))


