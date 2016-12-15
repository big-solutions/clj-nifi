(ns clj-nifi.core
  (:import (org.apache.nifi.components PropertyDescriptor$Builder PropertyValue)
           (org.apache.nifi.processor Relationship$Builder ProcessSession ProcessContext DataUnit)
           (org.apache.nifi.flowfile FlowFile)
           (org.apache.nifi.processor.io OutputStreamCallback)
           (java.io InputStream OutputStream)
           (java.util.concurrent TimeUnit)))

(defn relationship [& {:keys [name description auto-terminate]
                       :or {name           "Relationship"
                            description    "Description"
                            auto-terminate false}}]
  (-> (new Relationship$Builder)
      (.name name)
      (.description description)
      (.autoTerminateDefault auto-terminate)
      (.build)))

(defn add-validators [builder validators]
  (reduce #(.addValidator ^PropertyDescriptor$Builder % %2)
          builder validators))

(defn property [&{:keys [name display-name description required? dynamic? sensitive?
                         expressions? default validators allowable-values]
                  :or {name             "Property"
                       display-name     "Property"
                       description      "Description"
                       required?        false
                       dynamic?         false
                       sensitive?       false
                       expressions?     false
                       default          ""
                       validators       []
                       allowable-values nil}}]
  (-> (new PropertyDescriptor$Builder)
      (.name name)
      (.description description)
      (.required required?)
      (.dynamic dynamic?)
      (.sensitive sensitive?)
      (.expressionLanguageSupported expressions?)
      (.defaultValue default)
      (add-validators validators)
      (.allowableValues (if allowable-values (set allowable-values)))
      (.build)))

(defn queue-size [session]
  (.getQueueSize session))

(defn init [context session]
  {:context context :session session})

(defn adjust-counter [{:keys [session] :as scope} counter delta immediate?]
  (.adjustCounter session counter delta immediate?)
  scope)

(defn create [{:keys [session] :as scope}]
  (assoc scope :file (.create session)))

(defn get-one [{:keys [session] :as scope}]
  (assoc scope :file (.get session)))

(defn get-batch [{:keys [session] :as scope} ^int max-count]
  (map #(assoc scope :file %)
       (.get session max-count)))

(defn get-by [{:keys [session] :as scope} file-filter]
  (map #(assoc scope :file %)
       (.get session file-filter)))

(defn penalize [{:keys [session file] :as scope}]
  (assoc scope :file (.penalize session file)))

(defn remove-file [{:keys [session file] :as scope}]
  (.remove session file)
  scope)

(defn clone
  ([{:keys [session] :as scope} parent]
   (assoc scope :file (.clone session parent)))
  ([{:keys [session] :as scope} parent offset size]
   (assoc scope :file (.clone session parent offset size))))

(defn put-attribute [{:keys [session file] :as scope} k v]
  (assoc scope :file (.putAttribute session file k v)))

(defn remove-attribute [{:keys [session file] :as scope} k]
  (assoc scope :file (.removeAttribute session file k)))

(defn remove-attributes [{:keys [session file] :as scope} ks]
  (assoc scope :file (.removeAllAttributes session file (set ks))))

(defn remove-attributes-by [{:keys [session file] :as scope} pattern]
  (assoc scope :file (.removeAllAttributes session file (re-pattern pattern))))


(defn output-callback [contents]
  (reify OutputStreamCallback
    (process [_ out]
      (spit out contents))))

(defn write [{:keys [session file] :as scope} contents]
  (assoc scope :file (.write session file
                       (output-callback contents))))

(defn append [{:keys [session file] :as scope} contents]
  (assoc scope :file (.append session file
                       (output-callback contents))))

(defn with-read [{:keys [session file] :as scope} f & args]
  (let [in (.read session file)
        transformer (or (f in args) identity)]
    (transformer scope)))

(defn import-from
  ([{:keys [session file] :as scope} ^InputStream in]
   (assoc scope :file (.importFrom session file in)))
  ([{:keys [session file] :as scope} path keep-original?]
   (assoc scope :file (.importFrom session file path keep-original?))))

(defn export-to
  ([{:keys [session file] :as scope} ^OutputStream out]
   (.exportTo session file out)
   scope)
  ([{:keys [session file] :as scope} path append?]
   (.exportTo session file path append?)
   scope))

(defn merge-files
  ([{:keys [session file] :as scope} sources]
   (assoc scope :file (.merge session file sources)))
  ([{:keys [session file] :as scope} sources header footer demarcator]
   (assoc scope :file (.merge session file sources header footer demarcator))))

(defn transfer
  ([{:keys [session file] :as scope}]
   (.transfer session file)
   scope)
  ([{:keys [session file] :as scope} rel]
  (.transfer session file rel)
  scope))

(defn as-string [value]
  (.getValue ^PropertyValue value))

(defn as-double [value]
  (.asDouble ^PropertyValue value))

(defn as-float [value]
  (.asFloat ^PropertyValue value))

(defn as-integer [value]
  (.asInteger ^PropertyValue value))

(defn as-long [value]
  (.asLong ^PropertyValue value))

(defn as-data-size [value unit]
  (.asDataSize ^PropertyValue value unit))

(defn as-time-period [value unit]
  (.asTimePeriod ^PropertyValue value unit))

(defn as-controller [value]
  (.asControllerService ^PropertyValue value))

(def property-value-fns
  {:string            as-string
   :double            as-double
   :float             as-float
   :integer           as-integer
   :long              as-long
   :data.B            #(as-data-size % DataUnit/B)
   :data.KB           #(as-data-size % DataUnit/KB)
   :data.MB           #(as-data-size % DataUnit/MB)
   :data.GB           #(as-data-size % DataUnit/GB)
   :time.nanoseconds  #(as-time-period % TimeUnit/NANOSECONDS)
   :time.microseconds #(as-time-period % TimeUnit/MICROSECONDS)
   :time.milliseconds #(as-time-period % TimeUnit/MILLISECONDS)
   :time.seconds      #(as-time-period % TimeUnit/SECONDS)
   :time.minutes      #(as-time-period % TimeUnit/MINUTES)
   :time.hours        #(as-time-period % TimeUnit/HOURS)
   :time.day          #(as-time-period % TimeUnit/DAYS)
   :controller        as-controller})

(defn get-property
  ([^ProcessContext context ^String prop]
   (.getProperty context prop))
  ([^ProcessContext context ^String prop value-fn-key]
   ((value-fn-key property-value-fns) 
     (.getProperty context prop))))

(defn get-properties [^ProcessContext context]
  (.getProperties context))


(defn demo [context session]
  (->> (-> (init context session)
           (get-batch 10))
       (map #(-> % (write "hejhaj")
                   (append "jaganjac")))))

(defn demo2 [context session]
  (->> (get-batch (init context session) 10)
       (map #(write % "hejhaj"))
       (map #(append % "jaganjac"))))
