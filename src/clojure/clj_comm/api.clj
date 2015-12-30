; Clojure Adapter for the Java Communications API
;
; by Otto Linnemann
; (C) 2015, Eclipse Public License

(ns clj-comm.api
    (:gen-class)
    (:require [cider.nrepl :refer (cider-nrepl-handler)]
              [clojure.tools.nrepl.server :as nrepl-server]
              [clojure.core.async
               :as a
               :refer [>! <! >!! <!! go chan buffer close! thread
                       alts! alts!! timeout sliding-buffer]])
    (:import [java.util Vector]
             [gnu.io CommPortIdentifier CommPort SerialPort SerialPortEvent SerialPortEventListener]
             [clj_comm BufferedSerialPortReader BufferedSerialPortEventListener BufferedSerialPortEvent]))


(defn create-serial-port-channels
  "Open a serial commincation device and create and bind a pair of two asynchronous
   communication channels of Rich's core.async library to it. The data which is
   received from the serial device is internally cached for performance reasons.
   Only in case a sequence of specified trigger characters is received the cache
   is written to the channel. The following keyword arments are accepted:

   :device-name     file name of the serial communication device e.g. /dev/ttyS0
   :baud-rate       bits per second of the serial line, default is 115200
   :data-bits       number of data bits, default 8
   :stop-bits       number of stop bits, default 1
   :parity          number of parity bits, default 0
   :open-timeout    millisecons to wait for the serial line to open, default 2000
   :cached-buffers  number of maximum cached buffer chunks, default 10
   :trigger-strings vector of strings where each triggers data to be written to
                    the communication channel, default ['\r' '\n'].

   The function returns the hash map with the following keys:

   :serial-in-ch    reads the data received from the serial line from this channel
   :serial-out-ch   writes data out to the serial line over this channel
   :port            internally used, represents the serial port object
   :is              internally used, represents buffered input stream
   :os              internally used, represents buffered output stream.
   :buf-reader      internally used, represents serial line proxy object."
  [& {:keys [device-name baud-rate data-bits stop-bits parity open-timeout
             cached-buffers trigger-strings]
      :or {baud-rate 115200 data-bits 8 stop-bits 1 parity 0 open-timeout 2000
           cached-buffers 10 trigger-strings ["\r" "\n"]}}]
  (let [id-pty (CommPortIdentifier/getPortIdentifier device-name)
        port (.open id-pty "CljPortWrapper" open-timeout)
        os (.getOutputStream port)
        is (.getInputStream port)
        triggers (Vector.)
        _ (dorun (map #(.add triggers (.getBytes %)) trigger-strings))
        buf-reader (new BufferedSerialPortReader is 4096 triggers)
        serial-in-ch (chan (sliding-buffer cached-buffers))
        serial-out-ch (chan (sliding-buffer cached-buffers))
        serial-reader (reify BufferedSerialPortEventListener
                        (bufferedSerialEvent [this e]
                          (if (= (.getEventType e) SerialPortEvent/DATA_AVAILABLE)
                            (>!! serial-in-ch (String. (.getBuffer e)))
                            (>!! serial-in-ch e))))]
    (.setSerialPortParams port baud-rate data-bits stop-bits parity)
    (.addEventListener port buf-reader)
    (.addEventListener buf-reader serial-reader)
    (.notifyOnDataAvailable port true)
    (go
      (loop [cont true]
        (let [in (<! serial-out-ch)]
          (.write os (.getBytes in))
          (if (not (and (instance? SerialPortEvent (type in)) (= (.getEventType in) (SerialPortEvent/BI))))
            (recur true)
            (println "______ out channel loop interrupted ______")))))
    {:serial-in-ch serial-in-ch :serial-out-ch serial-out-ch
     :port port :os os :is is :buf-reader buf-reader :serial-reader serial-reader}))


(defn release-serial-port-channels
  "close serial line and releases associated data. The function takes the
   output of create-serial-port-channels as input data. Refer to the
   create-serial-port channels for further information."
  [{port :port buf-reader :buf-reader serial-reader :serial-reader
    serial-in-ch :serial-in-ch serial-out-ch :serial-out-ch}]
  (>!! serial-out-ch (SerialPortEvent. port SerialPortEvent/BI true true)) ;; send interupt
  (.notifyOnDataAvailable port false)
  (.removeEventListener buf-reader)
  (.removeEventListener port)
  (.close port))
