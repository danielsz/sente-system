(ns example.systems
  (:require
    [example.server :refer [event-msg-handler ring-routes new-example-broadcaster]]
    [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
    ;; or
    ;; [taoensso.sente.server-adapters.immutant :refer (sente-web-server-adapter)]
    ;; Optional, for Transit encoding:
    [taoensso.sente.packers.transit :as sente-transit]
    [environ.core :refer [env]]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [system.core :refer [defsystem]]
    (system.components
      [http-kit :refer [new-web-server]]
      [endpoint :refer [new-endpoint]]
      [middleware :refer [new-middleware]]
      [handler :refer [new-handler]]
      [sente :refer [new-channel-socket-server sente-routes]])
    [com.stuartsierra.component :as component]))

(defsystem dev-system
  [:sente-endpoint (component/using
                     (new-endpoint sente-routes)
                     [:sente])
   :app-endpoints (new-endpoint ring-routes)
   :example-broadcaster (component/using
                          (new-example-broadcaster)
                          [:sente :broadcast-enabled?_])
   :broadcast-enabled?_ (atom true)
   ; **NB**: Sente requires the Ring `wrap-params` + `wrap-keyword-params`
   ; middleware to work. These are included with
   ; `ring.middleware.defaults/wrap-defaults` - but you'll need to ensure
   ; that they're included yourself if you're not using `wrap-defaults`.
   :middleware (new-middleware {:middleware [[wrap-defaults site-defaults]]})
   :handler (component/using
              (new-handler)
              [:sente-endpoint :app-endpoints :middleware])
   :http (component/using
           (new-web-server (Integer. ^String (env :http-port)))
           [:handler])
   :sente (component/using
            (new-channel-socket-server event-msg-handler (get-sch-adapter) {:wrap-component? true})
            [:broadcast-enabled?_])])

(defsystem prod-system
  [:sente-endpoint (component/using
                     (new-endpoint sente-routes)
                     [:sente])
   :app-endpoints (new-endpoint ring-routes)
   :example-broadcaster (component/using
                          (new-example-broadcaster)
                          [:sente :broadcast-enabled?_])
   :broadcast-enabled?_ (atom true)
   ; **NB**: Sente requires the Ring `wrap-params` + `wrap-keyword-params`
   ; middleware to work. These are included with
   ; `ring.middleware.defaults/wrap-defaults` - but you'll need to ensure
   ; that they're included yourself if you're not using `wrap-defaults`.
   :middleware (new-middleware {:middleware [[wrap-defaults site-defaults]]})
   :handler (component/using
              (new-handler)
              [:sente-endpoint :app-endpoints :middleware])
   :http (component/using
           (new-web-server (Integer. ^String (env :http-port)))
           [:handler])
   :sente (component/using
            (new-channel-socket-server event-msg-handler (get-sch-adapter)
                               {:packer (sente-transit/get-transit-packer :edn)
                                :wrap-component? true})
            [:broadcast-enabled?_])])
