(ns blobs.buffer
  #?(:clj (:import [java.nio ByteBuffer ByteOrder])))

(defprotocol Buffer
  (get-byte [this])
  (get-int [this])
  (remaining? [this]))

#?(:clj
   (extend-protocol Buffer
     ByteBuffer
     (get-byte [^ByteBuffer this] (.get this))
     (get-int [^ByteBuffer this] (.getInt this))
     (remaining? [^ByteBuffer this] (.hasRemaining this))))

#?(:cljs (deftype DataviewBuffer [dv pos]))
#?(:cljs
   (extend-protocol Buffer
     DataviewBuffer
     (get-byte [b]
       (let [v (.getInt8 (.-dv b) (.-pos b))]
         (set! (.-pos b) (inc (.-pos b)))
         v))
     (get-int [b]
       ;; true for little-endian
       (let [v (.getInt32 (.-dv b) (.-pos b) true)]
         (set! (.-pos b) (+ 4 (.-pos b)))
         v))
     (remaining? [b]
       (< (.-pos b) (.-byteLength (.-dv b))))))


#?(:cljs
   (defn from-arraybuffer
     [ab]
     (->DataviewBuffer (js/DataView. ab) 0)))