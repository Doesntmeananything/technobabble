(ns mylib.utils
  #?(:cljs
     (:require [cljs.reader :refer [read-string]])))

(defn in-seq? [s x]
  (some? (some #{x} s)))

(defn parse-string-number
  "Receives a string and parses it. If the string is nil or empty, it returns 0."
  [s]
  (cond
    (not-empty s) (read-string s)
    :else 0))