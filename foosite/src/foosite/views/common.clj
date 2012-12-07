(ns foosite.views.common
  (:use [noir.core :only [defpartial]]
        [hiccup.page :only [include-css html5 include-js]]))

(defpartial layout [& content]
            (html5
              [:head
               [:title "foosite"]
               (include-css "/css/reset.css")
               (include-css "/css/rickshaw.min.css")
               (include-css "/css/chosen.css")
               (include-css "/css/main.css")]
              [:body
               [:nav
                [:a {:href "/"} "Grant Funding"]
                [:a {:href "/collaborations"} "Collaborations"]]
               [:div#content
                [:p content]]
               (include-js "/js/jquery-1.8.3.min.js")
               (include-js "/js/d3.v2.min.js")
               (include-js "/js/jquery-ui.min.js")
               (include-js "/js/rickshaw.min.js")
               (include-js "/js/chosen.jquery.min.js")
               (include-js "/js/domb.js")
               (include-js "/js/fp.js")
               (include-js "/js/main.js")]))
