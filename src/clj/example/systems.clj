(ns example.systems
  (:require 
   [example.my-app :refer [event-msg-handler* my-ring-handler]]
   [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]
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
  [:web (new-web-server (Integer. (env :http-port)) my-ring-handler)
   :sente (new-channel-sockets event-msg-handler* sente-web-server-adapter)])

(defsystem prod-system
  [:web (new-web-server (Integer. (env :http-port)) my-ring-handler)
   :sente (new-channel-sockets event-msg-handler* sente-web-server-adapter
                               {:packer (sente-transit/get-flexi-packer :edn)})])

