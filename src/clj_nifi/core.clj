(ns clj-nifi.core
  (:import (org.apache.nifi.components PropertyDescriptor$Builder)
           (org.apache.nifi.processor Relationship$Builder ProcessSession)
           (org.apache.nifi.flowfile FlowFile)
           (org.apache.nifi.processor.io OutputStreamCallback)
           (java.io InputStream OutputStream)))

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

(defn demo [session]
  (->> (-> (init nil session)
           (get-batch 10))
       (map #(-> % (write "hejhaj")
                   (append "jaganjac")))))

(defn demo2 [session]
  (->> (get-batch (init nil session) 10)
       (map #(write % "hejhaj"))
       (map #(append % "jaganjac"))))
