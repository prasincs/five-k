(ns five-k.system
  (:require [com.stuartsierra.component :as component]
            [five-k.component.curator :refer [new-curator]]
            [five-k.component.executor-driver :refer [new-executor-driver]]
            [five-k.component.leader-driver :refer [new-leader-driver]]
            [five-k.component.scheduler-driver :refer [new-scheduler-driver]]
            [five-k.component.scheduler :refer [new-scheduler]]
            [five-k.example-webserver :as web]
            [five-k.secor-manager :as secor]
            [five-k.executor :refer [executor]]
            [five-k.scheduler :refer [scheduler] :as sched]
            [five-k.zookeeper-state :refer [new-zookeeper-state]])
  (:gen-class))

(defn executor-system
  []
  (component/system-map
   :driver (new-executor-driver (executor))))

(defn webserver-system
  []
  (component/system-map
   :driver (new-executor-driver (web/executor))))


(comment (defn secor-system
   []
   (component/system-map
    :driver (new-executor-driver (secor/executor)))))

(defn scheduler-system
  [master initial-state exhibitor task-launcher zk-path]
  (component/system-map
   :curator (new-curator exhibitor)
   :zookeeper-state (component/using
                      (new-zookeeper-state initial-state zk-path)
                      [:curator])
   :scheduler (component/using
               (new-scheduler task-launcher)
               [:zookeeper-state])
   :driver (component/using
            (new-scheduler-driver master)
            [:scheduler])))

(defn ha-scheduler-system
  [master initial-state exhibitor task-launcher zk-path]
  (component/system-map
   :curator (new-curator exhibitor)
   :zookeeper-state (component/using
                     (new-zookeeper-state initial-state zk-path)
                     [:curator])
   :scheduler (component/using
               (new-scheduler task-launcher)
               [:zookeeper-state])
   :leader-driver (component/using
                   (new-leader-driver zk-path master "five-k" "five-k")
                   [:curator :scheduler])))

(defn -main
  [command-type & [scheduler-type master n-tasks & _]]
  (println "Command-type" command-type "Scheduler-type" scheduler-type)
  (let [state {:tasks n-tasks}
        system (condp = [command-type scheduler-type]
                 ["scheduler" "jar"] (scheduler-system master state sched/jar-task-info)
                 ["scheduler" "docker"] (scheduler-system master state sched/docker-task-info)
                 ["scheduler" "ha"] (ha-scheduler-system master state sched/jar-task-info)
                 ;; avoid HA for the time being
                 ["scheduler" "secor"] (scheduler-system master state sched/secor-task-info)
                 ["executor" nil] (executor-system)
                 ["executor" "secor"] (secor-system)
                 ["example-webserver" nil] (webserver-system))]
    (component/start system)
    (while true
      (Thread/sleep 1000000))))
