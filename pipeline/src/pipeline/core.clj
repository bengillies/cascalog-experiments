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


;; locations of expected data
(def grant-in "../data/grants_epsrc_grants_1.csv")
(def department-in "../data/departments_epsrc_grants_1.csv")
(def organisation-in "../data/organisations_epsrc_grants_1.csv")
(def partner-in "../data/partner_epsrc_grants_1.csv")
(def partner-grant-mapping "../data/partner_grants_epsrc_grants_1.csv")


(defn get-year [date-str]
  "convert a date string into a year"
  (let [cal (Calendar/getInstance)
        date (.parse (SimpleDateFormat. "dd MMMMM yyyy") date-str)]
    (do
      (.setTime cal date)
      (.get cal Calendar/YEAR))))

;; grant data
;; XXX: this is ugly but cascalog makes us defn every function explicitly
;; (i.e. we can't generate this function using another function)
(defn to-grant [row]
  "convert a row from the grant csv into a hashmap"
  ((transform-row {:id 0
                  :ref 1
                  :title 2
                  :end-date 5
                  :start-date 6
                  :value 7
                  :desc 8}) row))

;; department data
(defn to-department [row]
  "convert  row from the department csv into a hashmap"
  ((transform-row {:organisation_id 0
                   :name 1
                   :id 2}) row))

;; organisation data
(defn to-organisation [row]
  " convert a row from the organisation csv into a hashmap"
  ((transform-row {:name 0 :id 1}) row))

;; define some intermediate queries
;;
;; we want to take all the grant data, associate it with an organisation, and
;; sum up all the values per organisation

(defn get-grant-values [n]
  "extract the values we need from the grant data"
  [(Long. (n :value))
   (n :id)
   (get-year (n :start-date))
   (n :ref)])

(def grant-values
  "query grant data for value, departmentid and year grant started"
  (<- [?value ?departmentid ?year ?ref]
      ((csv-tap grant-in) ?row)
      (to-grant ?row :> ?grant)
      (get-grant-values ?grant :> ?value ?departmentid ?year ?ref)))


(defn get-department-ids [n]
  "extract the values we need from the department data"
  [(n :id) (n :organisation_id)])

(def departments
  "query department data to map department ids to organisation ids"
  (<- [?departmentid ?organisationid]
      ((csv-tap department-in) ?row)
      (to-department ?row :> ?department)
      (get-department-ids ?department :>
                          ?departmentid ?organisationid)))


(defn get-organisation-values [n]
  "extract the values we need from the organisation data"
  [(n :id) (n :name)])

(def organisations
  "query organisation data for id and name"
  (<- [?id ?name]
      ((csv-tap organisation-in) ?row)
      (to-organisation ?row :> ?organisation)
      (get-organisation-values ?organisation :> ?id ?name)))


(def department-to-organisation
  "convert deprtment ids to organisation names"
  (<- [?departmentid ?orgname]
      (departments ?departmentid ?orgid)
      (organisations ?orgid ?orgname)))


(def grants-plus-orgs
  "swap department ids from grant-values with organisation names"
  (<- [?value ?orgname ?year]
      (grant-values ?value ?departmentid ?year ?ref)
      (department-to-organisation ?departmentid ?orgname)))


(def sum-grants
  "sum value fields per organisation/year"
  (<- [?orgname ?total ?year]
      (grants-plus-orgs ?value ?orgname ?year)
      (sum ?value :> ?total)))


(defn get-partner-map [row]
  "return partner id and grant reference"
  row)
(def partner-grant
  "partner to grant mappings"
  (<- [?id ?ref]
      ((csv-tap partner-grant-mapping) ?map-row)
      (get-partner-map ?map-row :> ?id ?ref)))


(defn get-partner-info [row]
  "return partner name and id from partner data"
  [(nth row 1) (nth row 2)])
(def partner-names
  "match up partner names with grant references"
  (<- [?partner ?ref]
      ((csv-tap partner-in) ?partner-row)
      (get-partner-info ?partner-row :> ?partner ?id)
      (partner-grant ?id ?ref)))


(def partner-organisations
  "filter only those partners who also have grants themselves and match them up
  to organisations"
  (<- [?partner ?orgname ?value ?ref]
      (partner-names ?partner ?ref)
      (grant-values ?value ?departmentid ?year ?ref)
      (department-to-organisation ?departmentid ?orgname)))

(defaggregateop combine-grants
                "sum up all grant values and return with a list of all grant
                references (and their values) that made up the total"
                ([] [0 []])
                ([[total refs] value grant-ref] [(+ total value)
                                                 (conj refs {:ref grant-ref
                                                             :value value})])
                ([[total refs]] [{:total total :refs refs}]))

(def sum-partners
  "sum total value of grants for each organisation per partner"
  (<- [?partner ?orgname ?total]
      (partner-organisations ?partner ?orgname ?value ?ref)
      (combine-grants ?value ?ref :> ?total)))

(defbufferop group-collaborations
             "reduce partner-organisations to organisations with a list of
             partners. Only count each grant once when calculating the grand
             total"
             [tuples]
             (let [orgs (group-by last tuples)
                   sum-refs (fn [lst]
                              (first (reduce (fn [acc tuple]
                                        (reduce (fn [[total refs] ref]
                                                  (if (some #{(ref :ref)} refs)
                                                    [total refs]
                                                    [(+ total (ref :value))
                                                     (conj refs (ref :ref))]))
                                                acc (:refs (nth tuple 1))))
                                      [0 []] lst)))]
               (map (fn [[org tuple-list]]
                      {:organisation org
                       :total (sum-refs tuple-list)
                       :collaborators (vec (map (fn [tuple]
                                                  {:collaborator (first tuple)
                                                   :refs (:refs (nth tuple 1))
                                                   :total (:total
                                                            (nth tuple 1))})
                                                tuple-list))})
                    orgs)))

(defbufferop group-totals
             "reduce tuples into a list grouping totals and years by
             organisation"
             [tuples]
             (let [orgs (group-by first tuples)]
               (map (fn [[org tuple-list]]
                      {:organisation org
                       :total (reduce #(+ %1 (nth %2 1)) 0 tuple-list)
                       :totals  (vec (map (fn [tuple]
                                            {:total (nth tuple 1)
                                             :year (last tuple)})
                                          tuple-list))})
                    orgs)))


(defn run-queries []
  "Calculate total grants per year per organisation"
  (?<- (output-tap "../foosite/data/totals") [?totals]
       (sum-grants ?orgname ?total ?year)
       (group-totals ?orgname ?total ?year :> ?totals))
  (with-debug (?<- (output-tap "../foosite/data/collaborations") [?collaborations]
       (sum-partners ?partner ?orgname ?total)
       (group-collaborations ?partner ?total ?orgname :>
                             ?collaborations))))

(defn -main [& m]
  (bootstrap)
  (println "started Hadoop")
  (run-queries)
  (println "finished processing grant data"))

