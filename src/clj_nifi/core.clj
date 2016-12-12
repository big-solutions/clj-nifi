(ns clj-nifi.core
  (:import (org.apache.nifi.components PropertyDescriptor$Builder)
           (org.apache.nifi.processor Relationship$Builder)))

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
