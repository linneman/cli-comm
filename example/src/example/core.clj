(ns example.core
  (:require [cider.nrepl :refer (cider-nrepl-handler)]
              [clojure.tools.nrepl.server :as nrepl-server]
              [clojure.core.async
               :as a
               :refer [>! <! >!! <!! go chan buffer close! thread
                       alts! alts!! timeout sliding-buffer]]
              [clj-comm.api :as comm :refer [create-serial-port-channels release-serial-port-channels]]))

(comment

  (defn process-serial-channel
    [device-name]
    (let [channel-data (create-serial-port-channels :device-name device-name)
          serial-in-ch (:serial-in-ch channel-data)
          serial-out-ch (:serial-out-ch channel-data)]
      (go
        (loop [cnt 0]
          (let [in (<! serial-in-ch)]
            (if (= (.trim in) "quit")
              (do
                (>! serial-out-ch "*** terminate processing loop ***\r\n")
                (Thread/sleep 100)
                (release-serial-port-channels channel-data))
              (do
                (>! serial-out-ch (str "got ->" in "\r\n"))
                (recur (inc cnt)))))))
      channel-data))


  (def ch-data (process-serial-channel "/dev/ttys004"))
  (>!! (:serial-in-ch ch-data) "quit")
  (release-serial-port-channels ch-data)

  )
