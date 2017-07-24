(ns example.systems
  (:require 
   [example.server :refer [event-msg-handler main-ring-handler]]
   [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
   ;; or
   ;; [taoensso.sente.server-adapters.immutant :refer (sente-web-server-adapter)]
   ;; Optional, for Transit encoding:
   [taoensso.sente.packers.transit :as sente-transit]
   [environ.core :refer [env]]
   [system.core :refer [defsystem]]
   (system.components 
    [http-kit :refer [new-web-server]]
    [sente :refer [new-channel-sockets]])))
   
(defsystem dev-system
  [:web (new-web-server (Integer. ^String (env :http-port)) main-ring-handler)
   :sente (new-channel-sockets event-msg-handler (get-sch-adapter))])

(defsystem prod-system
  [:web (new-web-server (Integer. ^String (env :http-port)) main-ring-handler)
   :sente (new-channel-sockets event-msg-handler (get-sch-adapter)
                               {:packer (sente-transit/get-transit-packer :edn)})])
