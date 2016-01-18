(ns five-k.executor
  (:require [clj-mesos.executor :as mesos]
            [clojure.java.shell :refer [sh]])
  (:import [java.util Date]))


(def secor_path "dist/secor-0.12-SNAPSHOT-bin.tar.gz")

(defn executor
  []
  (let [info (atom nil)]
    (mesos/executor
     (launchTask [driver task-info]
                 (future (sh ))
                 (mesos/send-status-update driver {:task-id (:task-id task-info)
                                                         :state :task-running}))
     (registered [driver executor-info framework-info slave-info]
                 (reset! info (:executor-id executor-info))
                 ))))
