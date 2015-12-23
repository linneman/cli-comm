# Integration of a Clojure REPL into a Java ANT Project

This code snippet illustrates how to integrate Clojure's [read eval print loop
(repl)](http://en.m.wikipedia.org/wiki/REPL) within an [Ant](http://ant.apache.org) based Java
application. The approach allows to advance to a state of the art functional programming style and
to introspect and manipulate a Java application while it is running.

The Clojure libraries including application specific implemention is managed by Clojure's build
system [Leiningen](http://leiningen.org) which generates a single jar archive file. This so called
'uberjar' stand-alone java archive is afterwords published to the classpath.

## Prerequesites

Beneith the JDK and ant you need the clojure build tool leinignen for compilation. Download
the lein script file from Github

    $ cd ~/bin
    $ wget http://github.com/technomancy/leiningen/raw/stable/bin/lein
    $ chmod +x lein

and type

    $ lein self-install

## Build

To build the complete demo just invoke the script 'build.sh'

    $ ./build.sh

or in order to build the Clojure and the Java part separately enter

    $ cd clojure
    $ lein uberjar
    $ mv target/cljrepl-1.0.0-SNAPSHOT-standalone.jar ../java/lib/

    $ cd ../java
    $ ant jar

## Setting up the Classpath and Startup of the Application

This example requires to setup the classpath and invoke the main class explicitly:

    $ cd java
    $ java -classpath lib/cljrepl-1.0.0-SNAPSHOT-standalone.jar:build/jar/HelloWorld.jar oata.HelloWorld

##  REPL Based Development
The so called Read-Eval-Print-Loop, in short REPL, is a very powerful way of changing Clojure code
while its is executed. This is in fact one of most powerful concepts in software development and
allows to try and apply changes from the program editor directly while a program is still
running. You can connect your favorite editor to the application side running Clojure respectively
Java code on top of the JVM. A very brief summary of the required steps how to do this is given in
the following. This explanation targets the Emacs text editor but any other editor or IDE shall with
support for Clojure's [nrepl interface](https://github.com/clojure/tools.nrepl) will work as well.
For Emacs you will need the extensions [cider](https://github.com/clojure-emacs/cider) and
[clojure-mode](https://github.com/technomancy/clojure-mode).  Alternatively you can clone the Emacs
setup which was used for the most of the development in this project from
[Github](https://github.com/linneman/emacs-setup).

* start the Java application as explained above
* open up a Clojure file
* e.g. clojure/src/cljrepl/main.clj
* connect slime with meta-x followed by "cider-connect"
* enter 'localhost' for the server and port number 7888 in the command line dialogue afterwords
* emacs should show two buffers now - 'main.clj' and the 'cider-repl'. If not setup the frames manually. Refer to the emacs documentation how to do this.
* change the namespace to main.clj by hitting ctrl-x meta-n. Set 'main.clj' as active buffer first
* you can now evaluate the Clojure the expressions e.g. given in main.clj by putting the cursor after the expression and pressing crtl-x-e
* More information is available on the [cider project](https://github.com/clojure-emacs/cider)

## Use the provided nrepl Client to Script your Java Application

This repository provides a simple nrepl client that allows to connect to the nrepl server via the
shell command line standard I/O or TCP/IP socket connection. The latter one is the recommended
approach when injecting clojure expressions from an arbitraty script language e.g. for test
automation since the JVM does not have to be restarted for each injection.

The nrepl client is implemented in Clojure well and uses the very same build system Leingingen.
Here is how to use it:

Inside a shell make 'repl-client' your current directory and build and run a standalone application
with the commands:

    $ lein uberjar
    $ java -jar target/repl-client-1.0.0-SNAPSHOT-standalone.jar -l <port>

or do both in one step with the leiningen command

    $ lein trampoline run -l <port>

The above commands start the repl-client in server mode. Afterwords you can connect to this server via [telnet](http://en.m.wikipedia.org/wiki/Telnet),
[socat](http://www.dest-unreach.org/socat/), [netcat](http://en.m.wikipedia.org/wiki/Socat#Variants)
or any other network utility.

### Examples
Use telnet to connect to repl-client listening to port 5000 on localhost (above example):

    $ telnet localhost 5000

Do the same with socat:

    $ socat stdio tcp:localhost:5000

Evaluate the expression '(+ 1 2)' on the remote system from a shell:

    $ echo "(+ 1 2)" | socat -t 4 - tcp:localhost:5000

For more examples with respect how to retrieve and change Java objects refer to the
script example within the file test.clj which is found in the directory 'java/clojure'.

## License
This implementation code stands under the terms of the
[Eclipse Public License -v 1.0](http://opensource.org/licenses/eclipse-1.0.txt), the same as Clojure.

February 2015, Otto Linnemann

## Resources and links
Thanks to all the giants whose shoulders we stand on. And the giants theses giants stand on...
And special thanks to Rich Hickey (and the team) for Clojure. Really, thanks!

* Clojure: http://clojure.org
* Leiningen: https://github.com/technomancy/leiningen
