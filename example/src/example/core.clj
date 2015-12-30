; Clojure Adapter for the Java Communications API
;
; by Otto Linnemann
; (C) 2015, Eclipse Public License

(ns example.core
  (:require [cider.nrepl :refer (cider-nrepl-handler)]
              [clojure.tools.nrepl.server :as nrepl-server]
              [clojure.core.async
               :as a
               :refer [>! <! >!! <!! go chan buffer close! thread
                       alts! alts!! timeout sliding-buffer]]
              [clj-comm.api :as comm :refer [create-serial-port-channels release-serial-port-channels]])
  (:import [gnu.io SerialPortEvent]))


(defn process-serial-channel
  [device-name]
  (let [channel-data (create-serial-port-channels :device-name device-name)
        serial-in-ch (:serial-in-ch channel-data)
        serial-out-ch (:serial-out-ch channel-data)]
    (go
      (loop [cnt 0]
        (let [in (<! serial-in-ch)]
          (if (isa? (class in) SerialPortEvent)
            (do
              (condp = (.getEventType in)
                SerialPortEvent/BI (println "got break interrupt")
                SerialPortEvent/CD (println "got carrier detect")
                SerialPortEvent/CTS (println "got clear to send")
                SerialPortEvent/DSR (println "got data set ready")
                SerialPortEvent/FE (println "got framing error")
                SerialPortEvent/OE (println "got overrun error")
                SerialPortEvent/OUTPUT_BUFFER_EMPTY (println "got output buffer empty")
                SerialPortEvent/PE (println "got parity error")
                SerialPortEvent/RI (println "got ring indicator"))
              (recur (inc cnt)))
            (if (= (.trim in) "quit")
              (do
                (>! serial-out-ch "*** terminate processing loop ***\r\n")
                (Thread/sleep 100)
                (release-serial-port-channels channel-data))
              (do
                (>! serial-out-ch (str "got ->" in "\r\n"))
                (recur (inc cnt))))))))
    channel-data))


(comment

  (def ch-data (process-serial-channel "/dev/ttys004"))
  (>!! (:serial-in-ch ch-data) "quit")
                                        ;(release-serial-port-channels ch-data)

  )
