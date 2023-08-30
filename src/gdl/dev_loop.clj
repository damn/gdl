(ns gdl.dev-loop
  (:require [clojure.java.io :as io]
            [nrepl.server :refer [start-server]]
            [clojure.tools.namespace.repl :refer [disable-reload!
                                                  refresh
                                                  refresh-all]]))

(disable-reload!) ; keep same connection/nrepl-server up throughout refreshs

(declare app-ns
         app-fn)

(defn start-app []
  (eval `(do ; old namespace/var bindings are unloaded with refresh-all so always evaluate them fresh
          (require (quote ~app-ns))
          (~app-fn))))

(def ^Object obj (Object.))

(defn wait! []
  (locking obj
    (println "\n\n>>> WAITING FOR RESTART <<<")
    (.wait obj)))

(def app-start-failed (atom false))

(defn restart! []
  (reset! app-start-failed false)
  (locking obj
    (println "\n\n>>> RESTARTING <<<")
    (.notify obj)))

(require '[clj-commons.pretty.repl :as p])

(defn dev-loop []
  (println "start-app")
  (try (start-app)
       (catch Throwable t
         (p/pretty-pst t)
         (println "app-start-failed")
         (reset! app-start-failed true)))

  (loop []
    (when-not @app-start-failed
      (do
       (println "refresh")
       (def refresh-result (refresh :after 'gdl.dev-loop/dev-loop))
       (p/pretty-pst refresh-result)
       (println "error on refresh")))
    (wait!)
    (recur)))

; ( I dont know why nrepl start-server does not have this included ... )
(defn save-port-file
  "Writes a file relative to project classpath with port number so other tools
  can infer the nREPL server port.
  Takes nREPL server map and processed CLI options map.
  Returns nil."
  [server]
  ;; Many clients look for this file to infer the port to connect to
  (let [port (:port server)
        port-file (io/file ".nrepl-port")]
    (.deleteOnExit ^java.io.File port-file)
    (spit port-file port)))

(defn -main [& [app-namespace app-start-fn]]
  (.bindRoot #'app-ns (symbol app-namespace))
  (.bindRoot #'app-fn (symbol (str app-namespace "/" app-start-fn)))

  (defonce nrepl-server (start-server))
  (save-port-file nrepl-server)
  ;(println "Started nrepl server on port" (:port nrepl-server))

  (dev-loop))

; Example:
; lein run -m gdl.dev-loop gdl.simple-test app
