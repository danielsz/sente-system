(set-env!
 :source-paths   #{"src/clj" "src/cljs"}
 :dependencies '[[adzerk/boot-cljs      "0.0-3269-2" :scope "test"]
                 [adzerk/boot-reload    "0.2.6"      :scope "test"]
                 [environ "1.0.0"]
                 [danielsz/boot-environ "0.0.3"]
                 [org.danielsz/system "0.1.8"]
                 [org.clojure/clojure       "1.7.0-RC1"]
                 [org.clojure/tools.nrepl "0.2.10"]
           
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.taoensso/sente        "1.5.0"] ; <--- Sente

   ;;; ---> Choose (uncomment) a supported web server <---
                 [http-kit                  "2.1.19"]
                 ;; [org.immutant/web       "2.0.0-beta2"]

                 [ring                      "1.4.0-RC1"]
                 [ring/ring-defaults        "0.1.5"] ; Includes `ring-anti-forgery`, etc.

                 [compojure                 "1.3.4"] ; Or routing lib of your choice
                 [hiccup                    "1.0.5"] ; Optional, just for HTML

   ;;; Transit deps optional; may be used to aid perf. of larger data payloads
   ;;; (see reference example for details):
                 [com.cognitect/transit-clj  "0.8.275"]
                 [com.cognitect/transit-cljs "0.8.220"]])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-reload    :refer [reload]]
 '[reloaded.repl :refer [init start stop go reset]]
 '[example.systems :refer [dev-system]]
 '[danielsz.boot-environ :refer [environ]]
 '[system.boot :refer [system run]])

(deftask dev
  "Run a restartable system in the Repl"
  []
  (comp
   (environ :env {:http-port 3019})
   (watch)
   (system :sys #'dev-system :auto-start true :hot-reload true :files ["my_app.clj"])
   (reload)
   (cljs :source-map true)
   (repl :server true)))
