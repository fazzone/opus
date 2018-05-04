(ns blobs.codec.solution
  (:require [blobs.codec.common :refer [expect read-str write-str read-pos write-pos write-int]]
            [blobs.buffer :as buf]))

(defn write-solution
  [{:keys [puzzle solution solved nobjects objects
           unknown-a cycles unknown-b cost area total-steps nobjects] :as sol}]
  (concat
    (write-int 7)
    (write-str puzzle)
    (write-str solution)
    (if-not solved
      [0]
      (into [4 2]
            (mapcat #(write-int (or % 0))
                    [unknown-a cycles unknown-b cost area 3 total-steps])))
    (write-int nobjects)
    (mapcat
      (fn [{}])
      )
    )
  )


;{:puzzle "P008",
; :solution "NEW SOLUTION 1",
; :solved false,
; :nobjects 23,
; :objects [{:name "unbonder",



(defn read-solution-info
  [b]
  (let [n (buf/get-int b)]
    (if (zero? n)
      {:solved false}
      (do
        (expect 4 n "solution solved marker byte")
        (array-map
          :solved true
          :unknown-a (buf/get-int b)
          :cycles (buf/get-int b)
          :unknown-b (buf/get-int b)
          :cost (buf/get-int b))))))

(defn read-solution-header
  [b]
  (expect 7 (buf/get-int b) "solution header first byte")
  (let [pzl-name (read-str b)
        sol-name (read-str b)
        info (read-solution-info b)
        nobjects? (buf/get-int b)
        more (if-not (:solved info)
               {:nobjects nobjects?}
               (let [area (buf/get-int b)
                     _ (expect 3 (buf/get-int b) "solved solution header unknown")
                     total-steps (buf/get-int b)]
                 (expect 2 nobjects? "this should be 2 for solved files")
                 {:area area
                  :total-steps total-steps
                  :nobjects (buf/get-int b)}))]
    (merge {:puzzle pzl-name :solution sol-name}
           info
           more)))

(def ^:const step-decode-map
  (zipmap "RrEeGgPpAaCXO"
          [:rotate-cw :rotate-ccw
           :extend :retract
           :grab :drop
           :pivot-cw :pivot-ccw
           :track-forward :track-back
           :repeat :reset :nop]))

(defn read-program-step
  [b]
  (let [chr (char (buf/get-byte b))
        step (buf/get-int b)]
    {:t step
     :step (get step-decode-map chr (str chr))}))

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

(defn orderify
  [m last-key]
  (apply array-map last-key (get m last-key) (mapcat identity (dissoc m last-key))))

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


