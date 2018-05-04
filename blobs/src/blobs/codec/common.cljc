(ns blobs.codec.common
  (:require [blobs.buffer :as buf]))

(defn expect
  ([exp val info]
   (when (not= val exp)
     (println "warning - expected " exp " buf got " val "(" info ")\n"))))

;; STRINGS (byte length + bytes)

(defn read-str
  [b]
  (let [len (buf/get-byte b)]
    (apply str (repeatedly len #(char (buf/get-byte b))))))

(defn string->byte-vec
  [s]
  #?(:clj  (vec (.getBytes s))
     :cljs (vec
             (for [i (range (.-length s))]
               (.charCodeAt s i)))))

(defn write-str
  [s]
  (let [bv (string->byte-vec s)]
    (into [(byte (count bv))] bv)))

;; POSITIONS (two 32-bit little-endian ints)

(defn read-pos
  [b]
  [(buf/get-int b) (buf/get-int b)])

(defn write-int
  [v]
  [(bit-and 0xff v)
   (bit-and 0xff (bit-shift-right v 8))
   (bit-and 0xff (bit-shift-right v 16))
   (bit-and 0xff (bit-shift-right v 24))])

(defn write-pos
  [[i j]]
  (concat (write-int i) (write-int j)))



