(ns kixi.log.timbre.appenders.logstash
  (:require [cheshire.core :as json])
  (:import [java.io Writer]))

(defn stacktrace-element->vec
  [^StackTraceElement ste]
  [(.getClassName ste) (.getFileName ste) (.getLineNumber ste) (.getMethodName ste)])

(defn exception->map
  [^Throwable e]
  (merge
   {:type (str (type e))
    :trace (apply str (mapv stacktrace-element->vec (.getStackTrace e)))}
   (when-let [m (.getMessage e)]
     {:message m})
   (when-let [c (.getCause e)]
     {:cause (exception->map c)})))

(defn not-empty-str
  [s]
  (when-not (clojure.string/blank? s)
    s))

(defn extract-msg
  [data]
  (let [f (first (:vargs data))]
    (if (and (map? f)
             (= 1 (count (:vargs data))))
      f
      (not-empty-str (force (:msg_ data))))))

(defn log->json
  [data]
  (let [opts (get-in data [:config :options])
        exp (some-> (force (:?err data)) exception->map)
        msg (or (extract-msg data) (:message exp))]
    {:level (:level data)
     :namespace (:?ns-str data)
     :file (:?file data)
     :line (:?line data)
     :exception exp
     :hostname (force (:hostname_ data))
     :msg msg
     "@timestamp" (force (:timestamp_ data))}))

(defn get-lock
  [^Writer writer]
  (let [lock-field (.getDeclaredField Writer "lock")]
    (.setAccessible lock-field true)
    (.get lock-field writer)))

(defn json->out
  []
  (let [lock (get-lock *out*)]
    (fn [data]
      (locking lock
        (json/generate-stream
         (log->json data)
         *out*)
        (prn)))))

(defn json-appender
  []
  {:enabled?   true
   :async?     false
   :output-fn identity
   :fn (json->out)})
