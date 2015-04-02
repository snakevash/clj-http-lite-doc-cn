(defproject clj-http-lite-doc-cn "0.2.2-SNAPSHOT"
  :description "clj-http-lite with chinese comment for learn"
  :url "http://github.com/snakevash/clj-http-lite-doc-cn"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [slingshot "0.12.1"]]
  :main ^:skip-aot clj-http-lite-doc-cn.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[ring/ring-jetty-adapter "1.3.2"]
                                  [ring/ring-devel "1.3.2"]]}}
  :test-selectors {:default #(not (:integration %))
                   :integration :integration
                   :all (constantly true)}
  :aliases {"all" ["with-profile" "dev"]})
