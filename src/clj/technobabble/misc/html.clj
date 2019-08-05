(ns technobabble.misc.html
  (:require [clojure.string :as string])
  (:import [org.jsoup Jsoup]))

(defn remove-html
  "Cleans HTML tags from a string while preserving new lines"
  [^String s]
  (let [no-nl (string/replace s #"\n" "\\\\n")
        clean (.text (Jsoup/parse no-nl))]
    (string/replace clean #"\\n" "\n")))

(defn clean-message-text
  "Removes the HTML from a message's thought.
  Probably best suited for a technobabble.utils, but we don't have one of those yet"
  [message]
  (update-in message [:thought] remove-html))