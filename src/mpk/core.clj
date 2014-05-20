(ns mpk.core
  (:gen-class)
  (:use ring.middleware.params
        ring.util.response
        ring.adapter.jetty
        ring.middleware.session
        mpk.configuration)
  (:require [mpk.action-handlers :as handlers :refer :all]))


(def valid-actions {"put" :put "get" :get "key" :key})

(def actions {:get (handlers/->ReadHandler)
              :put (handlers/->SaveHandler)
              :key (handlers/->KeyHandler)})

(defn check-callback-paramater [params session success]
  (if (nil? (get "callback" params))
    (-> (response "This service responds only to JSONP request. You are missing callback parameter") (status 405))
    (success params session)))

(defn mpk-handler [request]
  (let [{action "action" :as params} (:params request) session (:session request)]
    (if (or (nil? action) (nil? (get valid-actions action)))
      (-> (response "Invalid method") (status 405))
      (let [the-action-handler ((get valid-actions action) actions)]
        (perform the-action-handler params session)))))

(def mpk-app (wrap-session (wrap-params mpk-handler)))

(defn -main [& args]
  (do-build-configuration args)
  (run-jetty mpk-app @configuration))
