;(ns blobs.test
;  (:require [octet.core :as o :refer-macros [with-byte-order]]
;            [octet.buffer :as buffer]
;            [octet.spec :as spec]
;            [octet.spec.string :as sstr])
;  (:import [java.nio ByteBuffer ByteOrder]))
;
;
;
;
;(defprotocol Buffer
;  (pos [this])
;  (remaining [this])
;  (seek [this pos])
;  (read-byte [this])
;  (write-byte [this c]))
;
;(defprotocol Spec
;  (read-val [this buf])
;  (write-val [this buf v]))
;
;(defn vec->buffer
;  [vb]
;  (doto (ByteBuffer/wrap (byte-array vb))
;    (.order ByteOrder/LITTLE_ENDIAN)))
;
;(.getInt (vec->buffer [7 7 0 0]))

(def int32
  (reify Spec
    (read-val [_ buf]
      (reduce + (map * (repeatedly 4 #(read-byte buf)) [0x1 0x100 0x10000 0x1000000])))
    (write-val [_ buf v]
      )))

;(def ubyte
;  (reify Spec
;    (read-val [_ buf]
;      (read-byte buf))
;    (write-val [_ buf c]
;      (write-byte buf c))))
;
;(defn bind
;  [header header->spec value->header]
;  (reify Spec
;    (read-val [_ buf]
;      (let [head-val (read-val header buf)]
;        (read-val (header->spec head-val) buf)))
;    (write-val [_ buf v]
;      (let [head-val (value->header v)]
;        (write-val header buf head-val)
;        (write-val (header->spec head-val) buf v)))))
;
;(defn repeated
;  [n spec]
;  (reify Spec
;    (read-val [_ buf]
;      (repeatedly n #(read-val spec buf)))
;    (write-val [_ buf vs]
;      (doseq [v vs]
;        (write-val spec buf v)))))
;
;(defn fmap
;  [spec rf wf]
;  (reify Spec
;    (read-val [_ buf]
;      (rf (read-val spec buf)))
;    (write-val [_ buf v]
;      (write-val spec buf (wf v)))))
;
;(defn byte-buf->vec
;  [buf]
;  (vec
;    (for [i (range (.limit buf))]
;      (.get buf i))))
;
;(byte-buf->vec (vec->buffer [1 2 3 1 234]))
;
;(defn roundtrip
;  [spec val]
;  (let [b (ByteBuffer/allocate 10)]
;    (write-val spec b val)
;    (println )
;    (seek b 0)
;    (println 'r= (remaining b)
;             'p= (pos b))
;    (read-val spec b)))
;
;(roundtrip int32 123)
;
;(read-val (repeated 4 int32) (vec->buffer [1 2 3 4 7 0 0 0 1 1 0 0 0 1 0 0]))
;
;
;#_(def string*
;  (bind
;    ubyte
;    (fn [len]
;      (repeated len ubyte)
;      )
;    )
;  )
;
;
;(extend-protocol Buffer
;  ByteBuffer
;  (pos [this] (.position this))
;  (remaining [this] (.remaining this))
;  (seek [this pos] (.position this pos))
;  (read-byte [this] (.get this))
;  (write-byte [this c] (.put (byte c))))
;
;(read-val int32 (vec->buffer [7 7 0 0]))