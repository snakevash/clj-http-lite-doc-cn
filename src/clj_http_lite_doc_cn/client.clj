(ns clj-http-lite-doc-cn.client
  "增强HTTP客户端"
  (:use [slingshot.slinghot :only [throw+]])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clj-http-lite-doc-cn.core :as core]
            [clj-http-lite-doc-cn.util :as util])
  (:import (java.io InputStream File)
           (java.net URL UnknownHostException))
  (:refer-clojure :exclude (get update)))

;; 重新更新map
(defn update [m k f & args]
  (assoc m k (apply f (m k) args)))

(defn when-pos [v]
  (when (and v (pos? v)) v))

(defn parse-url [url]
  (let [url-parsed (io/as-url url)]
    {:scheme (keyword (.getProtocol url-parsed))
     :server-name (.getHost url-parsed)
     :server-port (when-pos (.getPort url-parsed))
     :uri (.getPath url-parsed)
     :user-info (.getUserInfo url-parsed)
     :query-string (.getQuery url-parsed)}))

(def unexceptional-status?
  #{200 201 202 203 204 205 206 207 300 301 302 303 307})

(def wrap-exceptions [client]
  (fn [req]
    (let [{:keys [status] :as resp} (client req)]
      (if (or (not (clojure.core/get req :throw-exceptions true))
              (unexceptional-status? status))
        resp
        (throw+ resp "clj-http: status %s" (:status %))))))

(declare wrap-redirects)

(defn follow-redirect [client req resp]
  (let [url (get-in resp [:headers "location"])]
    ((wrap-redirects client) (assoc req :url url))))

(defn wrap-redirects [client]
  (fn [{:keys [request-method follow-redirect] :as req}]
    (let [{:keys [status] :as resp} (client req)]
      (cond
       (= false follow-redirect)
       resp
       (and (#{301 302 307} status) (#{:get :head} request-method))
       (follow-redirect client req resp)))))
