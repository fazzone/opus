(ns blobs.codec.solution
  (:require [blobs.codec.common :refer [expect read-str write-str read-pos write-pos write-int string->byte-vec]]
            [blobs.buffer :as buf]))

(defn read-solution-info
  [b]
  (let [n (buf/get-int b)]
    (if (zero? n)
      {:solved false
       :nobjects (buf/get-int b)}
      (do
        (expect 4 n "solution solved marker byte")
        (array-map
          :solved true
          :unknown-a (buf/get-int b)
          :cycles (buf/get-int b)
          :unknown-b (buf/get-int b)
          :cost (buf/get-int b)

          :always-2 (expect 2 (buf/get-int b) "solved solution header unknown I")
          :area (buf/get-int b)
          :always-3 (expect 3 (buf/get-int b) "solved solution header unknown II")
          :total-steps (buf/get-int b)
          :nobjects (buf/get-int b))))))

(defn write-solution-info
  [{:keys [solved nobjects unknown-a cycles unknown-b cost always-2 area always-3 total-steps nobjects]}]
  (if-not solved
    (concat (write-int 0)
            (write-int nobjects))
    (concat (write-int 4)
            (write-int unknown-a)
            (write-int cycles)
            (write-int unknown-b)
            (write-int cost)
            (write-int always-2)
            (write-int area)
            (write-int always-3)
            (write-int total-steps)
            (write-int nobjects))))

(defn read-solution-header
  [b]
  (expect 7 (buf/get-int b) "solution header magic number")
  (let [pzl-name (read-str b)
        sol-name (read-str b)]
    (assoc (read-solution-info b)
      :puzzle pzl-name
      :solution sol-name)))

(defn write-solution-header
  [{:keys [puzzle solution] :as hdr}]
  (concat
    (write-int 7)
    (write-str puzzle)
    (write-str solution)
    (write-solution-info hdr)))

(def ^:const step-bytes (string->byte-vec "RrEeGgPpAaCXO"))
(def ^:const step-names [:rotate-cw :rotate-ccw
                         :extend :retract
                         :grab :drop
                         :pivot-cw :pivot-ccw
                         :track-forward :track-back
                         :repeat :reset :nop])


(def ^:const step-decode-map (zipmap step-bytes step-names))
(def ^:const step-encode-map (zipmap step-names step-bytes))

(defn read-program-step
  [b]
  (let [chr (buf/get-byte b)
        step-num (buf/get-int b)]
    {:t step-num
     :step (get step-decode-map chr chr)}))

(defn write-program-step
  [{:keys [t step]}]
  (cons (get step-encode-map step)
        (write-int t)))

(defn read-object-header
  [b]
  (array-map
    :name (read-str b)
    :always-1 (expect 1 (buf/get-byte b) "object header magic byte")
    :position (read-pos b)
    :size (buf/get-int b)
    :rotation (buf/get-int b)
    :index (buf/get-int b)
    :steps (buf/get-int b)
    :track-len (buf/get-int b)))

(defn read-object
  [b]
  (let [name (read-str b)
        _ (expect 1 (buf/get-byte b) (str "object header, after name=" (pr-str name)))
        position (read-pos b)
        size (buf/get-int b)
        rotation (buf/get-int b)
        index (buf/get-int b)
        steps (buf/get-int b)
        ;; extra-len is used for tracks
        extra-len (buf/get-int b)
        object-header (cond-> {:name name :size size
                               :index index :position position
                               :rotation rotation}
                              (pos? steps) (assoc :steps steps)
                              (pos? extra-len) (assoc :extra-len extra-len))]
    (cond-> object-header
            (pos? steps)
            (assoc :steps (vec (repeatedly steps #(read-program-step b))))

            (and (= name "track") (pos? extra-len))
            (assoc :extra
                   (let [extra (vec
                                 (for [_ (range extra-len)]
                                   (read-pos b)))]
                     (expect 0 (buf/get-int b) (str "zero-padding after track, hdr=" (pr-str object-header)))
                     extra)))))

(defn write-object
  [{:keys [name position size rotation index steps extra-len extra]}]
  (concat
    (write-str name)
    [1]
    (write-pos position)
    (write-int size)
    (write-int rotation)
    (write-int index)
    (write-int (count steps))
    (write-int (or extra-len 0))
    (when steps
      (mapcat write-program-step steps))
    (when (and (= name "track") extra-len (pos? extra-len))
      (concat
        (mapcat write-pos extra)
        (write-int 0)))))

(defn assoc-last
  [m k v]
  (apply array-map (mapcat identity (concat m [[k v]]))))

(defn read-solution
  [b]
  (let [header (read-solution-header b)]
    (cond-> header
            (buf/remaining? b)
            (assoc-last :objects (vec (for [_ (range (:nobjects header))]
                                        (read-object b)))))))

(defn write-solution
  [{:keys [objects] :as sol}]
  (concat
    (write-solution-header sol)
    (mapcat write-object objects)))
