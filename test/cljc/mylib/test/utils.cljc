(ns mylib.test.utils
  #?(:clj
     (:require [mylib.utils :refer :all]
               [technobabble.misc.html :refer :all]
               [clojure.test :refer :all])
     :cljs
     (:require [mylib.utils :refer [in-seq?]]
               [cljs.test :refer-macros [deftest is]])))

(deftest test-in-seq
  (is (in-seq? [0 19 2 3 1 22] 1))
  (is (in-seq? [:href :style :p :a :span] :style))
  (is (false? (in-seq? [:href :style :p :a :span] :on-click)))
  (is (in-seq? [{:a 1 :b 2} {:a 2 :b 3} {:a 4 :b 5}] {:b 3 :a 2})))

(deftest test-parse-string
  (are [input output] (= output (parse-string-number input))
    nil 0
    "" 0
    "0" 0
    "0.12" 0.12
    "1984" 1984))

(deftest test-remove-html
  (is (= "Hello world" (remove-html "<h1>Hello world")))
  (is (= "Hello world" (remove-html "<h1>Hello world</h1>")))
  (is (= "Hello world" (remove-html "Hello <h1>world</h1>")))
  (is (= "Hello\n\nworld" (remove-html "Hello\n\nworld")))
  (is (= "Hello\n world" (remove-html "Hello
  <h1>world</h1>")))
  (is (= "Hello\n world\n\n**some bold**" (remove-html "Hello\n<h1>world</h1>\n\n**some bold**")))
  (is (= "Hello\n world\n**some bold**" (remove-html "Hello\n<h1>world</h1>\\n**some bold**")))
  (is (= "a < b" (remove-html "a < b")))
  (is (= "a < b or else" (remove-html "a < b<script>injected!</script> or else")))
  (is (= "a < b nice try" (remove-html "a < b <a onclick='alert()'>nice try</a>"))))