(ns example.server
  "Official Sente reference example: server"
  {:author "Peter Taoussanis (@ptaoussanis)"}

  (:require
    [ring.middleware.defaults]
    [compojure.core     :as comp :refer [defroutes GET POST]]
    [compojure.route    :as route]
    [hiccup.core        :as hiccup]
    [clojure.core.async :as async  :refer [<! <!! >! >!! put! chan go go-loop]]
    [taoensso.timbre    :as timbre :refer [tracef debugf infof warnf errorf]]
    [taoensso.sente     :as sente]
    [system.repl :refer [system]]))

;; (timbre/set-level! :trace) ; Uncomment for more logging
(reset! sente/debug-mode?_ true) ; Uncomment for extra debug info

;;;; Ring handlers

(defn landing-pg-handler [ring-req]
  (hiccup/html
    [:h1 "Sente reference example"]
    [:p "An Ajax/WebSocket" [:strong " (random choice!)"] " has been configured for this example"]
    [:hr]
    [:p [:strong "Step 1: "] " try hitting the buttons:"]
    [:p
     [:button#btn1 {:type "button"} "chsk-send! (w/o reply)"]
     [:button#btn2 {:type "button"} "chsk-send! (with reply)"]]
    [:p
     [:button#btn3 {:type "button"} "Test rapid server>user async pushes"]
     [:button#btn4 {:type "button"} "Toggle server>user async broadcast push loop"]]
    [:p
     [:button#btn5 {:type "button"} "Disconnect"]
     [:button#btn6 {:type "button"} "Reconnect"]]
    ;;
    [:p [:strong "Step 2: "] " observe std-out (for server output) and below (for client output):"]
    [:textarea#output {:style "width: 100%; height: 200px;"}]
    ;;
    [:hr]
    [:h2 "Step 3: try login with a user-id"]
    [:p  "The server can use this id to send events to *you* specifically."]
    [:p
     [:input#input-login {:type :text :placeholder "User-id"}]
     [:button#btn-login {:type "button"} "Secure login!"]]
    ;;
    [:hr]
    [:h2 "Step 4: want to re-randomize Ajax/WebSocket connection type?"]
    [:p "Hit your browser's reload/refresh button"]
    [:script {:src "js/main.js"}] ; Include our cljs target
    ))

(defn login-handler
  "Here's where you'll add your server-side login/auth procedure (Friend, etc.).
  In our simplified example we'll just always successfully authenticate the user
  with whatever user-id they provided in the auth request."
  [ring-req]
  (let [{:keys [session params]} ring-req
        {:keys [user-id]} params]
    (debugf "Login request: %s" params)
    {:status 200 :session (assoc session :uid user-id)}))

(defroutes ring-routes
           (GET  "/"      ring-req (landing-pg-handler            ring-req))
           (GET  "/chsk"  ring-req ((:ring-ajax-get-or-ws-handshake (:sente system)) ring-req))
           (POST "/chsk"  ring-req ((:ring-ajax-post (:sente system)) ring-req))
           (POST "/login" ring-req (login-handler                 ring-req))
           (route/resources "/js" {:root "js"}) ; Static files, notably public/main.js (our cljs target)
           (route/not-found "<h1>Page not found</h1>"))

(def main-ring-handler
  "**NB**: Sente requires the Ring `wrap-params` + `wrap-keyword-params`
  middleware to work. These are included with
  `ring.middleware.defaults/wrap-defaults` - but you'll need to ensure
  that they're included yourself if you're not using `wrap-defaults`."
  (ring.middleware.defaults/wrap-defaults
    ring-routes ring.middleware.defaults/site-defaults))

;;;; Some server>user async push examples

(defn test-fast-server>user-pushes
  "Quickly pushes 100 events to all connected users. Note that this'll be
  fast+reliable even over Ajax!"
  []
  (doseq [uid (:any @(:connected-uids (:sente system)))]
    (doseq [i (range 100)]
      ((:chsk-send! (:sente system)) uid [:fast-push/is-fast (str "hello " i "!!")]))))

(comment (test-fast-server>user-pushes))

(defonce broadcast-enabled?_ (atom true))

(defn start-example-broadcaster!
  "As an example of server>user async pushes, setup a loop to broadcast an
  event to all connected users every 10 seconds"
  []
  (let [broadcast!
        (fn [i]
          (let [uids (:any @(:connected-uids (:sente system)))]
            (debugf "Broadcasting server>user: %s uids" (count uids))
            (doseq [uid uids]
              ((:chsk-send! (:sente system)) uid
                                             [:some/broadcast
                                              {:what-is-this "An async broadcast pushed from server"
                                               :how-often "Every 10 seconds"
                                               :to-whom uid
                                               :i i}]))))]

    (go-loop [i 0]
      (<! (async/timeout 10000))
      (when @broadcast-enabled?_ (broadcast! i))
      (recur (inc i)))))

;;;; Sente event handlers

(defmulti -event-msg-handler
          "Multimethod to handle Sente `event-msg`s"
          :id ; Dispatch on event-id
          )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg) ; Handle event-msgs on a single thread
  ;; (future (-event-msg-handler ev-msg)) ; Handle event-msgs on a thread pool
  )

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defmethod -event-msg-handler :example/test-rapid-push
  [ev-msg] (test-fast-server>user-pushes))

(defmethod -event-msg-handler :example/toggle-broadcast
  [{:as ev-msg :keys [?reply-fn]}]
  (let [loop-enabled? (swap! broadcast-enabled?_ not)]
    (?reply-fn loop-enabled?)))

;; TODO Add your (defmethod -event-msg-handler <event-id> [ev-msg] <body>)s here...


;;;; Init stuff

(defn start! [] (start-example-broadcaster!))

(defn -main "For `lein run`, etc." [] (start!))

(comment
  (start!)
  (test-fast-server>user-pushes))
