(ns technobabble.env
  (:require [clojure.tools.logging :as log]
            [luminus-migrations.core :as migrations]
            [technobabble.config :refer [env]]))

(def defaults
  {:init
   (fn []
     (do
       (log/info "-=[ Applying migrations ]=-")
       (migrations/migrate ["migrate"] (select-keys env [:database-url]))
       (log/info "...migrations done")
       (log/info "\n-=[technobabble started successfully]=-")))
   :stop
   (fn []
     (log/info "\n-=[technobabble has shut down successfully]=-"))
   :middleware identity})
