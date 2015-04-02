(ns clj-http-lite-doc-cn.core
  "核心 HTTP request/response 实现"
  (:require [clojure.java.io :as io])
  (:import (java.io ByteArrayOutputStream InputStream IOException)
           (java.net URI URL HttpURLConnection)))

;; 解析头
(defn parse-headers
  "从URLConnection和返回一个名称的map.
  如果名称的值不只一个(像`set-cookie`),那么将会是一个vector包含."
  [conn]
  ;; 内部循环的两个参数 i是索引 headers map
  (loop [i 1
         headers {}]
    ;; k 是键
    ;; v 是值
    (let [k (.getHeaderFieldKey ^HttpURLConnection conn i)
          v (.getHeaderField ^HttpURLConnection conn i)]
      (if k
        ;; update-in 集合 [过滤条件] 函数 参数
        (recur (inc i) (update-in headers [k] conj v))
        ;; zipmap 做一些key 和 val 的改写
        (zipmap (for [k (keys headers)]
                  ;; 全部小写
                  (.toLowerCase ^String k))
                ;; 遍历每个值 如果是多个值 那就是vec
                (for [v (vals headers)]
                  (if (= 1 (count v))
                    (first v)
                    (vec v))))))))

;; 解析返回值
(defn- coerce-body-entity
  "返回来自http实体,不管是字节数组还是流,
  并且在最后关闭连接"
  [{:keys [as]} conn]
  (let [ins (try
              (.getInputStream ^HttpURLConnection conn)
              (catch Exception e
                (.getErrorStream ^HttpURLConnection conn)))]
    ;; 判断 如果出错那么直接返回错误 .getErrorStream
    (if (or (= :stream as) (nil? ins))
      ins
      ;; ins 是输入流 baos 是byte数组
      (with-open [ins ^InputStream ins
                  baos (ByteArrayOutputStream.)]
        ;; 把结果拷贝到 baos中
        (io/copy ins baos)
        ;; 输出流
        (.flush baos)
        ;; 返回字节数组
        (.toByteArray baos)))))

(defn request
  "执行一个HTTP请求并且返回一个Ring map

  注意 Ring 使用InputStream, 但是clj-http使用字节数组"
  [{:keys [request-method scheme server-name server-port uri query-string
           headers content-type character-encoding body socket-timeout
           conn-timeout multipart debug insecure? save-request? follow-redirects] :as req}]
  ;; http-url 重新拼接url
  ;; conn 打开链接对象
  (let [http-url (str (name scheme) "://" server-name
                      (when server-port (str ":" server-port))
                      uri
                      (when query-string (str "?" query-string)))
        conn (.openConnection ^URL (URL. http-url))]
    ;; 如果 内容格式和字符编码 有值
    (when (and content-type character-encoding)
      (.setRequestProperty conn "Content-Type" (str content-type
                                                    "; charset="
                                                    character-encoding)))
    ;; 有内容格式但是没有字符编码
    (when (and content-type (not character-encoding))
      (.setRequestProperty conn "Content-Type" content-type))
    ;; 遍历序列
    (doseq [[h v] headers]
      (.setRequestProperty conn h v))
    ;; 是否设置URL重定向
    (when (false? follow-redirects)
      (.setInstanceFollowRedirects ^HttpURLConnection conn false))
    ;; 设置请求方法
    (.setRequestMethod ^HttpURLConnection conn (.toUpperCase (name request-method)))
    (when body
      (.setDoOutput conn true))
    (when socket-timeout
      (.setReadTimeout conn socket-timeout))
    (when conn-timeout
      (.setConnectTimeout conn conn-timeout))
    ;; 请求
    (.connect conn)
    ;; body
    (when body
      (with-open [out (.getOutputStream conn)]
        (io/copy body out)))
    (merge {:headers (parse-headers conn)
            ;; 结果状态
            :status (.getResponseCode ^HttpURLConnection conn)
            :body (when-not (= request-method :head)
                    (coerce-body-entity req conn))}
           (when save-request?
             ;; 把:save-request? 去掉
             ;; 把:http-url 增加
             {:request (assoc (dissoc req :save-request?)
                         :http-url http-url)}))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
