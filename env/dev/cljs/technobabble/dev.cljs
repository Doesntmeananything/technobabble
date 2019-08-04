(ns ^:figwheel-no-load technobabble.app
  (:require [technobabble.core :as core]
            [devtools.core :as devtools]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(figwheel/watch-and-reload
 :websocket-url "ws://localhost:3449/figwheel-ws"
 :on-jsload core/mount-components)

(devtools/install!)

(core/init!)
