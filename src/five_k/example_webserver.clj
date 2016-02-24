(ns five-k.example-webserver
  (:require [compojure.core :refer [defroutes routes GET ANY POST]]
            [clj-mesos.executor :as mesos]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]))

;; Long term plan - rewrite this in Java
;; Sort of inspiration -- https://github.com/twosigma/Cook/blob/master/scheduler/src/cook/mesos/api.clj#L47
(defrecord Job
    [name
     command
     maxRetries
     maxRuntime])

(defroutes app-routes
  (GET "/" [] "Hello from 5K!")
  (POST "/api/cauchy/job/register" [] )
  (route/not-found "Not Found"))

(def app
  (-> (handler/api app-routes)))

(defn start-server
  [port]
  (run-server (app-routes) {:port port}))

(defn executor
  []
  (mesos/executor
   (launchTask [driver task-info]
               (start-server 9090)
               (mesos/send-status-update driver {:task-id (:task-id task-info)
                                                 :state :task-running}))
   (registered [driver executor-info framework-info slave-info]
               (println slave-info))))
