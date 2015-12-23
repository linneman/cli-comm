; Clojure Repl Server
;
; main class for generation of stand-alone app
;
; by Otto Linnemann
; (C) 2015, GNU General Public Licence

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
  [{port :port buf-reader :buf-reader serial-reader :serial-reader
    serial-in-ch :serial-in-ch serial-out-ch :serial-out-ch}]
  (>!! serial-out-ch (SerialPortEvent. port SerialPortEvent/BI true true)) ;; send interupt
  (.notifyOnDataAvailable port false)
  (.removeEventListener buf-reader)
  (.removeEventListener port)
  (.close port))



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



(comment


  (def id-pty (CommPortIdentifier/getPortIdentifier "/dev/ttys004"))
  (def port (.open id-pty "CljPortWrapper" 2000))
  (.setSerialPortParams port 115200 SerialPort/DATABITS_8 SerialPort/STOPBITS_1 SerialPort/PARITY_NONE)

  (def os (.getOutputStream port))
  (def is (.getInputStream port))

  (def bufferedReader (new BufferedSerialPortReader is,4096,
              (doto (Vector.) (.add (.getBytes "\n")) (.add (.getBytes "\r")))))

  (. bufferedReader (add 1 2))
  .add bufferedReader 1 2


(def serialReader
    (reify SerialPortEventListener
      (serialEvent [this e]
        (comment condp = (.getEventType e)
          SerialPortEvent/DATA_AVAILABLE
          (do
            (do ; (conj gs1-ready-str-list (.toString gs1-in-stream))
              (println "-->" (.toString e))
                                        ;(>!! ch (.toString gs1-in-stream))
              ))
          SerialPortEvent/BI (println "break interrupt")
          SerialPortEvent/CD (println "carrier detect")
          SerialPortEvent/CTS (println "clear to send")
          SerialPortEvent/DSR (println "data set ready")
          SerialPortEvent/FE (println "framing error")
          SerialPortEvent/OE (println "overrun error")
          SerialPortEvent/OUTPUT_BUFFER_EMPTY (println "output buffer empty")
          SerialPortEvent/PE (println "parity error")
          SerialPortEvent/RI (println "ring indicator"))
        (String. (.toString e)))))


  (def serialReader
    (reify BufferedSerialPortEventListener
      (bufferedSerialEvent [this e]
        (comment condp = (.getEventType e)
          SerialPortEvent/DATA_AVAILABLE
          (do
            (do ; (conj gs1-ready-str-list (.toString gs1-in-stream))
              (println "-->" (String. (.getBuffer e)))
                                        ;(>!! ch (.toString gs1-in-stream))
              ))
          SerialPortEvent/BI (println "break interrupt")
          SerialPortEvent/CD (println "carrier detect")
          SerialPortEvent/CTS (println "clear to send")
          SerialPortEvent/DSR (println "data set ready")
          SerialPortEvent/FE (println "framing error")
          SerialPortEvent/OE (println "overrun error")
          SerialPortEvent/OUTPUT_BUFFER_EMPTY (println "output buffer empty")
          SerialPortEvent/PE (println "parity error")
          SerialPortEvent/RI (println "ring indicator"))
        (String. (.getBuffer e)))))

  (.addEventListener bufferedReader serialReader)
  (.removeEventListener bufferedReader serialReader)

  (.addEventListener port bufferedReader)
  (.addEventListener port serialReader)
  (.notifyOnDataAvailable port true)
  (.notifyOnDataAvailable port false)

  (.removeEventListener port)


  (.write os (.getBytes "Hello, World!\n\r"))

  )




(defn -main
  "execute script and start repl (cider)"
  [& args]
  (println "Hello"))


(comment



  (def channel-data (create-serial-port-channels :device-name "/dev/ttys004"))
  (release-serial-port-channels channel-data)

  (def serial-in-ch (:serial-in-ch channel-data))
  (def serial-out-ch (:serial-out-ch channel-data))
  (def port (:port channel-data))
  (def buf-reader (:buf-reader channel-data))
  (def serial-reader (:serial-reader channel-data))


  (go
    (loop [cnt 0]
      (let [in (<! serial-in-ch)]
        (if (= (.trim in) "quit")
          (>! serial-out-ch "*** terminate processing loop ***\r\n")
          (do
            (>! serial-out-ch (str "got ->" in "\r\n"))
            (recur (inc cnt)))))))




  (def id-pty (CommPortIdentifier/getPortIdentifier "/dev/ttys004"))
  (def port (.open id-pty "CljPortWrapper" 2000))
  (.setSerialPortParams port 115200 SerialPort/DATABITS_8 SerialPort/STOPBITS_1 SerialPort/PARITY_NONE)

  (def os (.getOutputStream port))
  (def is (.getInputStream port))
  (def  ch (chan (sliding-buffer 10)))
  (def  buf-reader (new BufferedSerialPortReader is 4096
                        (doto (Vector.) (.add (.getBytes "\n")) (.add (.getBytes "\r")))))
  (def serial-reader (reify BufferedSerialPortEventListener
                          (bufferedSerialEvent [this e]
                            (if (= (.getEventType e) SerialPortEvent/DATA_AVAILABLE)
                              (>!! ch (String. (.getBuffer e)))
                              (>!! ch e)))))

  (.addEventListener port buf-reader)
  (.addEventListener buf-reader serial-reader)
  )


(comment

  (ns clj-comm.main
    (:gen-class)
    (:require [cider.nrepl :refer (cider-nrepl-handler)]
              [clojure.tools.nrepl.server :as nrepl-server]
              [clojure.core.async
               :as a
               :refer [>! <! >!! <!! go chan buffer close! thread
                       alts! alts!! timeout sliding-buffer]])
    (:import (gnu.io CommPortIdentifier CommPort SerialPort
                     SerialPortEvent SerialPortEventListener)
             (java.io OutputStream ByteArrayOutputStream)))


  (defn exec-script-when-existing
  "executes the given clojure file when existing"
  [filename]
  (when (.exists (clojure.java.io/as-file filename))
    (load-file filename)))


  (defn start-repl
  "starts cider repl server at given port"
  [port]
  (println "staring clojure repl ...")
  (nrepl-server/start-server :port port :handler cider-nrepl-handler)
  (println "clojure repl server cider is running")
  (println "connect to IP ADDR ATM:"
           ;; (.getCanonicalHostName (java.net.InetAddress/getLocalHost))
           ", port:" port))


  (defn -main
  "execute script and start repl (cider)"
  [& args]
  (do
    (when-let  [filename (first args)]
      (println "executing clojure file " filename " ...")
      (exec-script-when-existing filename))
    (start-repl 7888)))


  (comment

    (def the-ports (CommPortIdentifier/getPortIdentifiers))
    (.hasMoreElements the-ports)


    (def id-tty-gs1 (CommPortIdentifier/getPortIdentifier "/dev/ttyGS1"))

    (def port-gs1 (.open id-tty-gs1 "CljPortWrapper" 2000))
    (.setSerialPortParams port-gs1 115200 SerialPort/DATABITS_8 SerialPort/STOPBITS_1 SerialPort/PARITY_NONE)

    (def gs1-out (.getOutputStream port-gs1))
    (def gs1-in (.getInputStream port-gs1))

    (.write gs1-out (.getBytes "Hello, World!\n\r"))



    (def gs1-in-stream (ByteArrayOutputStream.))

    (def ch (chan (sliding-buffer 10)))


    (def serialReader
      (reify SerialPortEventListener
        (serialEvent [this e]
          (condp = (.getEventType e)
            SerialPortEvent/DATA_AVAILABLE
            (do
                                        ; (println "data available:" (.available gs1-in) )
              (let [chunk (byte-array (.available gs1-in))
                    bytes-read (.read gs1-in chunk)]
                                        ;(println (str " --> got " bytes-read " bytes:" (String. chunk)))
                (.write gs1-in-stream chunk 0 bytes-read)
                (if (has-barray-cr-or-lf chunk)
                  (do ; (conj gs1-ready-str-list (.toString gs1-in-stream))
                    (println "-->" (.toString gs1-in-stream))
                    ; (>!! ch (.toString gs1-in-stream))
                    (.reset gs1-in-stream)))))
            SerialPortEvent/BI (println "break interrupt")
            SerialPortEvent/CD (println "carrier detect")
            SerialPortEvent/CTS (println "clear to send")
            SerialPortEvent/DSR (println "data set ready")
            SerialPortEvent/FE (println "framing error")
            SerialPortEvent/OE (println "overrun error")
            SerialPortEvent/OUTPUT_BUFFER_EMPTY (println "output buffer empty")
            SerialPortEvent/PE (println "parity error")
            SerialPortEvent/RI (println "ring indicator")))))

    (.addEventListener port-gs1 serialReader)
    (.notifyOnDataAvailable port-gs1 true)
    (.notifyOnDataAvailable port-gs1 false)

    (.removeEventListener port-gs1)

    (.toString gs1-in-stream)
    (.reset gs1-in-stream)

    (def a (.getBytes "ABC\n"))
    (def b (.getBytes "ABC"))
    (aget a 2)
    (alength a)



    (has-barray-cr-or-lf a)
    (has-barray-cr-or-lf b)

    (<!! ch)


    )


  (defn has-barray-cr-or-lf
    "true when byte array contains CR or LF character"
    [a]
    (loop [i (alength a)]
      (if (> i 0)
        (let [b (aget a (dec i))]
          (if (or (= b 10) (= b 13))
            true
            (recur (dec i)))))))



  (defn register-serial-port-evt-listener
    [comm-port]
    (let [inp-stream (.getInputStream port-gs1)
          channel (chan (sliding-buffer 10))]
      (reify SerialPortEventListener
        (serialEvent [this e]
          (condp = (.getEventType e)
            SerialPortEvent/DATA_AVAILABLE
            (do
              (let [chunk (byte-array (.available inp-stream))
                    bytes-read (.read inp-stream chunk)]
                (.write gs1-in-stream chunk 0 bytes-read)
                (>!! channel chunk)
                (if (has-barray-cr-or-lf chunk)
                  (do ; (conj gs1-ready-str-list (.toString gs1-in-stream))
                    (println "-->" (.toString gs1-in-stream))
                    (>!! ch (.toString gs1-in-stream))
                    (.reset gs1-in-stream)))))
            SerialPortEvent/BI (println "break interrupt")
            SerialPortEvent/CD (println "carrier detect")
            SerialPortEvent/CTS (println "clear to send")
            SerialPortEvent/DSR (println "data set ready")
            SerialPortEvent/FE (println "framing error")
            SerialPortEvent/OE (println "overrun error")
            SerialPortEvent/OUTPUT_BUFFER_EMPTY (println "output buffer empty")
            SerialPortEvent/PE (println "parity error")
            SerialPortEvent/RI (println "ring indicator"))))))



  )
