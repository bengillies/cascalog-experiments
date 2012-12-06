(ns foosite.views.welcome
  (:require [foosite.views.common :as common]
            [noir.response :as response]
            [noir.request :as request])
  (:use noir.core
        [cascalog.playground :only [bootstrap]]
        [clojure.string :only [split]]))

(defn load-data [path]
  (let [data (slurp path)
        orgs (split data #"\n")]
    (map read-string orgs)))

(defpage "/" []
         (common/layout
           [:h1
            [:span "Top "]
            [:span#num-orgs ""]
            [:span " Universities and the amount they received in Grants"]]
           [:div#graph]
           [:div#legend]))


(defpage "/organisations" []
         (response/json (sort-by :total >
                                 (load-data "data/totals/part-00000"))))

(defpage "/collaborations" []
         (let [accept (get ((request/ring-request) :headers) "accept")
               contains (fn [obj match] (and (not (nil? obj))
                                             (not= (.indexOf obj match) -1)))]
           (if (contains accept "application/json")
             (response/json (sort-by
                              :total >
                              (load-data "data/collaborations/part-00000")))
             (common/layout
               [:h1 "Collaborations"]))))
