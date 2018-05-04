(ns blobs.core
  (:require [rum.core :as r]
            [octet.core :as o :refer-macros [with-byte-order]]
            [octet.buffer :as buffer]
            [octet.spec :as spec]
            [octet.spec.string :as sstr]
            [fipp.edn :as fipp]
            [blobs.buffer :as bbuf]
            [blobs.codec.solution :as csol]
            [cljs.tools.reader.edn :as edn]
            [clojure.string :as string]
            [blobs.codec.common :as ccommon]))

(enable-console-print!)

(def the-log-messages (atom []))

(r/defc log-window < r/reactive []
  [:div {:style {:display "flex" :flex-direction "column"}}
   (map-indexed
     (fn [i m]
       [:code {:key i} m])
     (r/react the-log-messages))])

(def the-text-contents (atom ""))

(r/defc output < r/reactive []
  [:div {:style {:box-sizing "border-box" :flex-grow 1 :display "flex" :flex-direction "column"}}
   [:textarea {:style {:width "100%" :height "100%" :flex-grow 1}
               :value (r/react the-text-contents)
               :on-change (fn [e] (reset! the-text-contents (.. e -target -value)))}]
   (log-window)])


(def the-file-arraybuffer (atom nil))

(r/defcs root [state]
  [:div {:style {:display "flex" :height "98vh"}}
   [:div {:style {:display "flex" :flex-direction "column" :max-width "12em"}}
    [:b "Opus Magnum solution encoder/decoder"]
    [:input {:type "file"
             :on-change (fn [e]
                          (when-let [file (some-> e (.-target) (.-files) (aget 0))]
                            (let [rdr (js/FileReader.)
                                  parsed (volatile! nil)]
                              (reset! the-log-messages [])
                              (set! (.-onload rdr)
                                    (fn [fe]
                                      (let [buf (.. fe -target -result)
                                            _ (reset! the-file-arraybuffer buf)
                                            parse-output (with-out-str
                                                           (vreset! parsed
                                                                    (-> buf bbuf/from-arraybuffer csol/read-solution)))]
                                        (reset! the-text-contents (with-out-str (fipp/pprint @parsed {:width 80})))
                                        (swap! the-log-messages into (string/split parse-output #"\n")))))
                              (.readAsArrayBuffer rdr file))))}]
    [:input {:type "button"
             :value "download solution file"
             :on-click (fn [e]
                         (println 'contents= (deref the-text-contents))
                         (try (fipp/pprint (edn/read-string @the-text-contents))
                              (catch :default e
                                (.log js/console e)
                                (reset! the-log-messages [(str e)])))
                         #_(set! (.. js/window -location -href) (.createObjectURL js/URL (js/Blob. [(deref the-text-contents)]))))}]
    [:a {:href "https://github.com/fazzone"} "source code"]
    [:div "to report a bug please use the button below to create a bug report and paste it into a new "
     [:a {:href "#"} "github issue"]]
    [:input {:type "button"
             :value "create bug report"
             :on-click (fn [e]
                         (let [file-data (atom nil)]
                           (when-let [b @the-file-arraybuffer]
                             (let [rdr (js/FileReader.)]
                               (set! (.-onload rdr) (fn [fe] (reset! file-data (.. fe -target -result))))
                               (.readAsDataURL rdr (js/Blob. [b]))))
                           (reset! the-text-contents
                                   (pr-str
                                     {:messages @the-log-messages
                                      :text-contents @the-text-contents
                                      :file-data @file-data}))))}]]
   (output)])

(defn mount-root
  []
  (print 'mount-root)
  (r/mount (root) (.getElementById js/document "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (mount-root)
  (println (ccommon/string->byte-vec "hello world")))

(mount-root)

