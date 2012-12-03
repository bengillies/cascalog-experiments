(ns pipeline.core
  (:require [cascalog.workflow :as w])
  (:use [cascalog.playground :only [bootstrap]]
        [cascalog.ops :only [sum]]
        [clojure.string :only [join trim]]
        [clojure-csv.core :only [parse-csv]]
        [clojure.java.io :only [reader]]
        cascalog.api)
  (:import [java.util Calendar]
           [java.text SimpleDateFormat]))

;; because...
(defmacro log [& msg]
  `(println (join " " '~msg)))

;; define an input and output tap

(defn csv-tap [path]
  "load in a csv file and drop the first line (i.e. the header)"
  (map #(conj [] %) (rest (parse-csv (reader path)))))

(defn output-tap [path]
  "open up a tap to the named directory ready for outputting"
  (lfs-textline path))


(defn transform-row [mapping]
  "return a function that converts the given row into a hashmap based on a
   mapping of key -> row number"
  (fn [row]
    (let [get (comp trim (partial nth row))]
      (reduce #(assoc %1 %2 (get (%1 %2))) mapping (keys mapping)))))


(def grant-in "../data/grants_epsrc_grants_1.csv")
(def department-in "../data/departments_epsrc_grants_1.csv")
(def organisation-in "../data/organisations_epsrc_grants_1.csv")


(defn get-year [date-str]
  (let [cal (Calendar/getInstance)
        date (.parse (SimpleDateFormat. "dd MMMMM yyyy") date-str)]
    (do
      (.setTime cal date)
      (.get cal Calendar/YEAR))))

;; grant data
;; XXX: this is ugly but cascalog makes us defn every function explicitly
;; (i.e. we can't generate this function using another function)
(defn to-grant [row]
  ((transform-row {:id 0
                  :ref 1
                  :title 2
                  :end-date 5
                  :start-date 6
                  :value 7
                  :desc 8}) row))

(defn grant-query [path-in path-out]
  (?<- (output-tap path-out) [?grant]
       ((csv-tap path-in) ?row)
       (to-grant ?row :> ?grant)))

;; department data
(defn to-department [row]
  ((transform-row {:organisation_id 0
                   :name 1
                   :id 2}) row))

(defn department-query [path-in path-out]
  (?<- (output-tap path-out) [?dep]
       ((csv-tap path-in) ?row)
       (to-department ?row :> ?dep)))


;; organisation data
(defn to-organisation [row]
  ((transform-row {:name 0 :id 1}) row))

(defn organisation-query [path-in path-out]
  (?<- (output-tap path-out) [?org]
       ((csv-tap path-in) ?row)
       (to-organisation ?row :> ?org)))


;; define some intermediate queries
;;
;; we want to take all the grant data, associate it with an organisation, and
;; sum up all the values per organisation

(defn get-grant-values [n]
  [(Long. (n :value))
   (n :id)
   (get-year (n :start-date))])
(def grant-values (<- [?value ?departmentid ?year]
                      ((csv-tap grant-in) ?row)
                      (to-grant ?row :> ?grant)
                      (get-grant-values ?grant :> ?value ?departmentid ?year)))

(defn get-department-ids [n] [(n :id) (n :organisation_id)])
(def departments (<- [?departmentid ?organisationid]
                     ((csv-tap department-in) ?row)
                     (to-department ?row :> ?department)
                     (get-department-ids ?department :>
                                         ?departmentid ?organisationid)))

(defn get-organisation-values [n] [(n :id) (n :name)])
(def organisations (<- [?id ?name]
                       ((csv-tap organisation-in) ?row)
                       (to-organisation ?row :> ?organisation)
                       (get-organisation-values ?organisation :> ?id ?name)))

(def department-to-organisation (<- [?departmentid ?orgname]
                                    (departments ?departmentid ?orgid)
                                    (organisations ?orgid ?orgname)))

(def grants-plus-orgs (<- [?value ?orgname ?year]
                          (grant-values ?value ?departmentid ?year)
                          (department-to-organisation ?departmentid ?orgname)))

(def sum-grants (<- [?orgname ?total ?year]
                    (grants-plus-orgs ?value ?orgname ?year)
                    (sum ?value :> ?total)))

(defn pair [tuple]
  {:total (tuple :total)
   :year (tuple :year)})

(defbufferop group-totals [tuples]
             (let [orgs (group-by first tuples)]
               (map (fn [[org tuple-list]]
                      {:organisation org
                       :totals  (vec (map (fn [tuple]
                                            {:total (nth tuple 1)
                                             :year (last tuple)})
                                          tuple-list))})
                    orgs)))

(defn sum-grants-to-obj [org total year]
  {:organisation org
   :total total
   :year year})

(defn run-queries []
  "run all the specified queries"
  (?<- (output-tap "../foosite/data/totals") [?totals]
       (sum-grants ?orgname ?total ?year)
       (group-totals ?orgname ?total ?year :> ?totals)))

(defn -main [& m]
  (bootstrap)
  (log started Hadoop)
  (run-queries)
  (log finished processing grant data))

