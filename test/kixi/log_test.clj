(ns kixi.log-test
  (:require [clojure.test :refer :all]
            [kixi.log :refer :all]
            [kixi.log.timbre.appenders.logstash :refer [dedot]]
            [taoensso.timbre :as timbre]
            [cheshire.core :as json]))

(defn as-json
  [s]
  (try (json/parse-string-strict s keyword)
       (catch Exception e nil)))

(deftest dedot-test
  (testing ""
    (is (= {} (dedot {})))
    (is (= {:foo "bar"
            :fizz "buzz"}
           (dedot {:foo "bar"
                   :fizz "buzz"})))
    (is (= {:foo_bar "bizz buzz"}
           (dedot {:foo.bar "bizz buzz"})))
    (is (= {:foo {:bar_buzz "baz"}}
           (dedot {:foo {:bar.buzz "baz"}})))))

(deftest timbre-appender-logstash-test
  (timbre/set-config! {:level :info
                       :timestamp-opts default-timestamp-opts
                       :appenders {:direct-json (timbre-appender-logstash)}})
  (testing "Logstash keys are present"
    (let [result-str  (with-out-str (timbre/info "foobar"))
          result-json (as-json result-str)]
      (is result-json)
      (is (contains? result-json :msg))
      (is (contains? result-json :level))
      (is (contains? result-json :namespace))
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
      (is (= (:msg result-json) "{:foo \"bar\"} 123"))))

  (testing "Exception"
    (let [result-str  (with-out-str (timbre/info (Exception. "broke!")))
          result-json (as-json result-str)]
      (is result-json)
      (is (string? (:msg result-json)))
      (is (= (:msg result-json) "broke!"))
      (is (= (get-in result-json [:exception :type]) "class java.lang.Exception"))
      (is (= (get-in result-json [:exception :message]) "broke!"))))

  (testing "Exception + message"
    (let [result-str  (with-out-str (timbre/info (Exception. "broke!") "456"))
          result-json (as-json result-str)]
      (is result-json)
      (is (string? (:msg result-json)))
      (is (= (:msg result-json) "456"))
      (is (= (get-in result-json [:exception :type]) "class java.lang.Exception"))
      (is (= (get-in result-json [:exception :message]) "broke!"))))

  (testing "Exception Info + data"
    (let [result-str  (with-out-str (timbre/info (ex-info "broke!" {:data true})))
          result-json (as-json result-str)]
      (is result-json)
      (is (string? (:msg result-json)))
      (is (= (:msg result-json) "broke!"))
      (is (= (get-in result-json [:exception :type]) "class clojure.lang.ExceptionInfo"))
      (is (= (get-in result-json [:exception :message]) "broke!"))
      (is (= (get-in result-json [:exception :data]) {:data true}))))
  
  (testing "Exception Info + data with dedotted map"
    (let [result-str  (with-out-str (timbre/info (ex-info "broke!" {:foo 1
                                                                    :bar 2
                                                                    :foo.bar 3})))
          result-json (as-json result-str)]
      (is result-json)
      (is (string? (:msg result-json)))
      (is (= (:msg result-json) "broke!"))
      (is (= (get-in result-json [:exception :type]) "class clojure.lang.ExceptionInfo"))
      (is (= (get-in result-json [:exception :message]) "broke!"))
      (is (= {:foo 1
              :bar 2
              :foo_bar 3}
             (get-in result-json [:exception :data]))))))
