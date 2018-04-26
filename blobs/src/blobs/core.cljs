(ns blobs.core
    (:require [rum.core :as r]))

(enable-console-print!)

#_(println "This text is printed from src/blobs/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

#_(defonce app-state (atom {:text "Hello world!"}))

(def the-file-data (atom nil))

(r/defc output < r/reactive []
  [:div
   [:pre (r/react the-file-data)]])

(def the-text-contents (atom nil))

(r/defc input []
  [:input {:type "text"           
           :on-change (fn [e]
                        (reset! the-text-contents (.. e -target -value)))}])

(r/defcs root [state]
  [:div {:style {:display "flex"}}
   [:div {:style {:display "flex"
                  :flex-direction "column"}}
    #_[:input {:type "file"
               :ref "fileinput"}]
    #_[:input {:type "button"
               :value "go?"
               :on-click (fn [e]
                           (when-let [selected-file (-> (r/ref state "fileinput") (.-files) (aget 0))]
                             (let [rdr (js/FileReader.)]
                               (set! (.-onload rdr)
                                 (fn [fe]
                                   (println 'fe fe)
                                   (println (.. fe -target -result))))
                               (.readAsArrayBuffer rdr selected-file)))
                           false)}]
    [:input {:type "file"
             :on-change (fn [e]
                          (when-let [file (some-> e (.-target) (.-files) (aget 0))]
                            (let [rdr (js/FileReader.)]
                              (set! (.-onload rdr)
                                (fn [fe]
                                  (reset! the-file-data (.. fe -target -result))))
                              #_(.readAsArrayBuffer rdr file)
                              (.readAsDataURL rdr file))))}]]
   (output)
   (input)
   
   [:input {:type "button"
            :value "download?"
            :on-click (fn [e]
                        (println 'contents= (deref the-text-contents))
                        (set! (.. js/window -location -href ) (.createObjectURL js/URL (js/Blob. [(deref the-text-contents)])))
                        )}]])

(defn mount-root
  []
  (print 'mount-root)
  (r/mount (root) (.getElementById js/document "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (mount-root))

(mount-root)
