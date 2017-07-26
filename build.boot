(def +project+ 'org.danielsz.examples/sente-system)
(def +version+ "1.11.0")

(set-env!
 :resource-paths #{"resources"}
 :source-paths   #{"src/clj" "src/cljs"}
 :dependencies '[[adzerk/boot-cljs "2.0.0" :scope "test"]
                 [adzerk/boot-reload "0.5.1" :scope "test"]

                 ;;; Use lein-project if your editor/IDE needs a project.clj file
                 [onetom/boot-lein-generate "0.1.3" :scope "test"]

                 [org.clojure/clojure       "1.8.0"]
                 [org.clojure/clojurescript "1.9.671"]

                 [environ "1.1.0"]
                 [boot-environ "1.1.0"]
                 [org.danielsz/system "0.4.0"]
                 [org.clojure/tools.nrepl "0.2.13"]

                 [org.clojure/core.async "0.3.443"]
                 [com.taoensso/sente        "1.11.0"] ; <--- Sente
                 [com.taoensso/timbre       "4.10.0"]
                 [com.taoensso/encore "2.91.1"]

                 ;;; TODO Choose (uncomment) a supported web server -----------------------
                 [http-kit                             "2.2.0"] ; Default
                 ;; [org.immutant/web                  "2.1.4"]
                 ;; [nginx-clojure/nginx-clojure-embed "0.4.4"] ; Needs v0.4.2+
                 ;; [aleph                             "0.4.1"]
                 ;; -----------------------------------------------------------------------

                 [ring                      "1.6.2"]
                 [ring/ring-defaults        "0.3.0"] ; Includes `ring-anti-forgery`, etc.

                 [compojure                 "1.6.0"] ; Or routing lib of your choice
                 [hiccup                    "1.0.5"] ; Optional, just for HTML

   ;;; Transit deps optional; may be used to aid perf. of larger data payloads
   ;;; (see reference example for details):
                 [com.cognitect/transit-clj  "0.8.300"]
                 [com.cognitect/transit-cljs "0.8.239"]])

(task-options!
 pom {:project     +project+
      :version     +version+
      :description "Port of the Sente reference web-app example project to Boot + System"
      :url "https://github.com/danielsz/sente-system"
      :scm {:url "https://github.com/danielsz/sente-system"}
      :license {"EPL" "http://www.eclipse.org/legal/epl-v10.html"}})

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-reload    :refer [reload]]
 ;;; Use lein-project if your editor/IDE needs a project.clj file
 '[boot.lein :as lein]
 '[system.repl :refer [init start stop go reset]]
 '[example.systems :refer [dev-system]]
 '[environ.boot :refer [environ]]
 '[system.boot :refer [system run]])

;;; Use lein-project if your editor/IDE needs a project.clj file
(lein/generate)

(deftask dev
  "Run a restartable system in the Repl"
  []
  (comp
   (environ :env {:http-port "3019"})
   (watch)
   (system :sys #'dev-system :auto true :files ["server.clj"])
   (reload)
   (cljs :source-map true)
   (repl :server true)))
