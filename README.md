# Clojure Adapter for the Java Communications API

Binding  of a serial  communication  device   to  a  pair  of   two  asynchronous
communication channels of Rich's [core.async](https://github.com/clojure/core.async)  library. The library is based on
[RXTX](http://rxtx.qbang.org/wiki/index.php/Main_Page) which is an open source
implementation of the [Java Communications API](http://docs.oracle.com/cd/E17802_01/products/products/javacomm/reference/api/javax/comm/package-summary.html).


## Prerequesites
Make sure you have the RXTX installed on your Java system and accessible via its
classpath. Refer to the [updated version of librxtx for GNU autobuild](https://github.com/linneman/librxtx)
which I did for hopefully seamless installation on major OSes and cross build
to embedded targets.


## Usage

Integrate the following line into your project setup:

[clj-comm "0.0.1-SNAPSHOT"]

This code snippet illustrates how to write a simple echo application
for a serial line interface:

    (def channel-data (create-serial-port-channels :device-name "/dev/ttys004"))
    (def serial-in-ch (:serial-in-ch channel-data))
    (def serial-out-ch (:serial-out-ch channel-data))

    (go
      (loop [cnt 0]
        (let [in (<! serial-in-ch)]
          (if (isa? (class in) SerialPortEvent)
            (recur (inc cnt))
            (if (= (.trim in) "quit")
              (do
                (>! serial-out-ch "*** terminate processing loop ***\r\n")
                (Thread/sleep 100)
                (release-serial-port-channels channel-data))
              (do
                (>! serial-out-ch (str "got ->" in "\r\n"))
                (recur (inc cnt))))))))


## License
This implementation code stands under the terms of the
[Eclipse Public License -v 1.0](http://opensource.org/licenses/eclipse-1.0.txt), the same as Clojure.

Decemeber 2015, Otto Linnemann

## Resources and links
Thanks to all the giants whose shoulders we stand on. And the giants theses giants stand on...
And special thanks to Rich Hickey (and the team) for Clojure. Really, thanks!

* Clojure: http://clojure.org
* Leiningen: https://github.com/technomancy/leiningen
