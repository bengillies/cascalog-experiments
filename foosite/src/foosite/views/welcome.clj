(ns foosite.views.welcome
  (:require [foosite.views.common :as common]
            [noir.response :as response])
  (:use noir.core
        [cascalog.playground :only [bootstrap]]
        [clojure.string :only [split]]))

(defn load-data [path]
  (let [data (slurp path)
        orgs (split data #"\n")]
    (map load-string orgs)))

(defpage "/" []
         (common/layout
           [:h1 "Universities and the amount they received in Grants"]))


(defpage "/organisations" []
         (response/json (load-data "data/totals/part-00000")))