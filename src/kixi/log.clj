(ns kixi.log
  (:require [kixi.log.timbre.appenders.logstash :as timbre-appenders-logstash]))

(def timbre-appender-logstash
  timbre-appenders-logstash/json-appender)

(def default-timestamp-opts
  "iso8601 timestamps"
  {:pattern  "yyyy-MM-dd HH:mm:ss,SSS"
   :locale   :jvm-default
   :timezone :utc})
