(defproject lotrdb "0.1.0-SNAPSHOT"
  :description "lotrdb"
  :url "https://github.com/Danurai/danurai.github.io"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.7.1"
  
  :main         lotrdb.system

  :jar-name     "lotrdb.jar"
  :uberjar-name "lotrdb-standalone.jar"
  
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/core.async  "0.3.443"]
                 ; Web server
                 [http-kit "2.3.0"]
                 [com.stuartsierra/component "0.3.2"]
                 ; routing
                 [compojure "1.6.0"]
                 [ring/ring-defaults "0.3.1"]
                 ; Websocket sente
                 ; [com.taoensso/sente "1.12.0"]
                 ; page rendering
                 [hiccup "1.0.5"]
								 [cheshire "5.8.0"]
                 ; [reagent "0.7.0"]
                 ; user management
                 [com.cemerick/friend "0.2.3"]
                 ; Databasing
                 [org.clojure/java.jdbc "0.7.5"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]]

  :plugins [[lein-figwheel "0.5.14"]
           [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
           [lein-autoexpect "1.9.0"]]

  :source-paths ["src"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :figwheel true
                :compiler {:main lotrdb.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/app.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           ;; To console.log CLJS data-structures make sure you enable devtools in Chrome
                           ;; https://github.com/binaryage/cljs-devtools
                           :preloads [devtools.preload]}}
               ;; This next build is a compressed minified build for
               ;; production. You can build this with:
               ;; lein cljsbuild once min
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/app.js"
                           :main lotrdb.core
                           :optimizations :advanced
                           :pretty-print false}}]}

  :figwheel { :css-dirs ["resources/public/css"]}

  ;; Setting up nREPL for Figwheel and ClojureScript dev
  ;; Please see:
  ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl
  :profiles {:uberjar {:aot :all
                     :source-paths ["src"]
                     :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
                     }
            :dev {:dependencies [[reloaded.repl "0.2.4"]
                               [expectations "2.2.0-rc3"]
                               [binaryage/devtools "0.9.4"]
                               [figwheel-sidecar "0.5.14"]
                               [com.cemerick/piggieback "0.2.2"]]
                   ;; need to add dev source path here to get user.clj loaded
                   :source-paths ["src" "dev"]
                   ;; for CIDER
                   ;; :plugins [[cider/cider-nrepl "0.12.0"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   ;; need to add the compliled assets to the :clean-targets
                   :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                                     :target-path]}})
