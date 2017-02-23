(ns kixi.log-test
  (:require [clojure.test :refer :all]
            [kixi.log :refer :all]
            [taoensso.timbre :as timbre]
            [cheshire.core :as json]))

(defn as-json
  [s]
  (try (json/parse-string-strict s keyword)
       (catch Exception e nil)))

(deftest timbre-appender-logstash-test
  (timbre/set-config! {:level :info
                       :timestamp-opts default-timestamp-opts
                       :appenders {:direct-json (timbre-appender-logstash "my-app")}})
  (testing "Logstash keys are present"
    (let [result-str  (with-out-str (timbre/info "foobar"))
          result-json (as-json result-str)]
      (is result-json)
      (is (contains? result-json :msg))
      (is (contains? result-json :level))
      (is (contains? result-json :namespace))
      (is (contains? result-json :application))
      (is (contains? result-json :file))
      (is (contains? result-json :line))
      (is (contains? result-json :exception))
      (is (contains? result-json :hostname))
      (is (contains? result-json (keyword "@timestamp")))))

  (testing "Normal string msg, single-arity"
    (let [result-str  (with-out-str (timbre/info "foobar"))
          result-json (as-json result-str)]
      (is result-json)
      (is (string? (:msg result-json)))
      (is (= (:msg result-json) "foobar"))))

  (testing "Normal string msg, multi-arity"
    (let [result-str  (with-out-str (timbre/info "foo" "bar"))
          result-json (as-json result-str)]
      (is result-json)
      (is (string? (:msg result-json)))
      (is (= (:msg result-json) "foo bar"))))

  (testing "Map, single-arity"
    (let [result-str  (with-out-str (timbre/info {:foo "bar"}))
          result-json (as-json result-str)]
      (is result-json)
      (is (map? (:msg result-json)))
      (is (= (get-in result-json [:msg :foo]) "bar"))))

  (testing "Map, multi-arity"
    (let [result-str  (with-out-str (timbre/info {:foo "bar"} "123"))
          result-json (as-json result-str)]
      (is result-json)
      (is (string? (:msg result-json)))
      (is (= (:msg result-json) "{:foo \"bar\"} 123")))))
