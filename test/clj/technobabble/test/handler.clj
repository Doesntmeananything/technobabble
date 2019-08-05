(ns technobabble.test.handler
  (:require [technobabble.handler :refer :all]
            [clojure.test :refer :all]
            [ring.mock.request :refer :all]))

(deftest test-app
  (testing "Our general routes are setup"
    (are [status path] (= status (:status ((app) (request :get path))))
      200 "/"
      200 "/login"
      200 "/signup"))

  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response))))))
