(ns foosite.views.common
  (:use [noir.core :only [defpartial]]
        [hiccup.page :only [include-css html5 include-js]]))

(defpartial layout [& content]
            (html5
              [:head
               [:title "foosite"]
               (include-css "/css/reset.css")
               (include-css "/css/rickshaw.min.css")
               (include-css "/css/main.css")]
              [:body
               [:div#content
                [:p content]
                [:div#graph]
                [:div#legend]]
               [:script {:src "/js/jquery-1.8.3.min.js"}]
               [:script {:src "/js/d3.v2.min.js"}]
               [:script {:src "/js/jquery-ui.min.js"}]
               [:script {:src "/js/rickshaw.min.js"}]
               (include-js "/js/main.js")]))
