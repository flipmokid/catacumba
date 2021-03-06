;; Copyright (c) 2015-2016 Andrey Antukh <niwi@niwi.nz>
;; All rights reserved.
;;
;; Redistribution and use in source and binary forms, with or without
;; modification, are permitted provided that the following conditions
;; are met:
;;
;; 1. Redistributions of source code must retain the above copyright
;;    notice, this list of conditions and the following disclaimer.
;; 2. Redistributions in binary form must reproduce the above copyright
;;    notice, this list of conditions and the following disclaimer in the
;;    documentation and/or other materials provided with the distribution.
;;
;; THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
;; IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
;; OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
;; IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
;; INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
;; NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
;; DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
;; THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
;; (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
;; THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

(ns catacumba.serializers
  "A friction-less and extensioble serializers abstraction layer.

  The main public api consists in two functions:

  - `encode`: that takes the clojure data and the serialization format
    as keyword and returns serialized bytes.
  - `decode`: just does the reverse operation. It takes the a byte array
    and returns a deserialized data.

  If something wrong is happens in the process, the underlying library
  used for serialize or deserialize can raise exceptions.

  Here an simple example serializing and deserializing a clojure
  hash-map using json format:

  ```
  (require '[catacumba.serializers :as sz])

  (-> (sz/encode {:foo 2} :json)
      (sz/bytes->str))
  ;; => \"{\\\"foo\\\": 2}\"

  (sz/decode some-data :json)
  ;; => {:foo 2}
  ```
  "
  (:require [cheshire.core :as json]
            [cognitect.transit :as transit]
            [buddy.core.codecs :as codecs])
  (:import java.io.ByteArrayInputStream
           java.io.ByteArrayOutputStream))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public Api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def str->bytes codecs/str->bytes)
(def bytes->str codecs/bytes->str)

(defmulti -encode
  (fn [data opts] (::type opts)))

(defmulti -decode
  (fn [data opts] (::type opts)))

(defn encode
  "Encode data."
  ([data type]
   (-encode data {::type type}))
  ([data type opts]
   (-encode data (merge {::type type} opts))))

(defn decode
  "Encode data."
  ([data type]
   (-decode data {::type type}))
  ([data type opts]
   (-decode data (merge {::type type} opts))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod -encode :json
  [data _]
  (-> (json/encode data)
      (codecs/str->bytes)))

(defmethod -decode :json
  [data _]
  (-> (codecs/bytes->str data)
      (json/decode true)))

(defmethod -encode :transit+json
  [data {:keys [handlers]}]
  (with-open [out (ByteArrayOutputStream.)]
    (let [w (transit/writer out :json {:handlers handlers})]
      (transit/write w data)
      (.toByteArray out))))

(defmethod -encode :transit+msgpack
  [data {:keys [handlers]}]
  (with-open [out (ByteArrayOutputStream.)]
    (let [w (transit/writer out :msgpack {:handlers handlers})]
      (transit/write w data)
      (.toByteArray out))))

(defmethod -decode :transit+json
  [data {:keys [handlers]}]
  (with-open [input (ByteArrayInputStream. data)]
    (let [reader (transit/reader input :json {:handlers handlers})]
      (transit/read reader))))

(defmethod -decode :transit+msgpack
  [data {:keys [handlers]}]
  (with-open [input (ByteArrayInputStream. data)]
    (let [reader (transit/reader input :msgpack {:handlers handlers})]
      (transit/read reader))))
