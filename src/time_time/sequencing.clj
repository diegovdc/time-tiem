(ns time-time.sequencing
  (:require [overtone.core :as o]))

(defn sequencer- [nome sequence* on-event state]
  (let [{:keys [start-at index repeat]} @state
        val* (first sequence*)]
    ;; FIXME `at` does not seem to do anything useful, see source
    (o/at (nome (+ start-at (:elapsed val*)))
        (on-event val* index)
        (if-not (:remainder? val*)
          (o/apply-by (nome (+ start-at (:elapsed (second sequence*))))
                    sequencer-
                    nome
                    (rest sequence*)
                    on-event
                    [(do (swap! state #(update-in % [:index] inc))
                         state)])
          (if (not= 0 repeat)
            (o/apply-by (nome (+ start-at (:elapsed val*) (:dur val*)))
                      sequencer-
                      nome
                      (@state :sequence)
                      on-event
                      [(do (swap! state
                                  #(-> %
                                       (assoc :start-at
                                              (+ start-at (:elapsed val*) (:dur val*)))
                                       (update-in [:index]
                                                  (constantly 0))
                                       (update-in [:repeat]
                                                  (fn [r] (if (= r :inf) r (dec r))))))
                           state)])))))
  state)

(defn sequencer
  ([nome sequence on-event]
   (sequencer- nome
               sequence
               on-event
               (atom {:on-event on-event
                      :sequence sequence
                      :start-at (nome)
                      :index 0})))
  ([nome sequence on-event {:keys [repeat offset]
                            :or {offset 0}
                            :as opts}]
   (sequencer- nome
               sequence
               on-event
               (atom (merge opts
                            {:on-event on-event
                             :sequence sequence
                             :start-at (+ (nome) offset)
                             :index 0})))))
(comment
  (let [nome* (metronome 60)
        now- (now)]
    ((user/capture :sequencer) (sequencer
                                nome
                                (conj (mapv #(hash-map :elapsed %) (range 5))
                                      {:remainder? true :dur 1 :elapsed 5})
                                (fn [vals index]
                                        ;(swap! quil-test/st #(assoc % :bg (rand 255)))
                                  (hh)
                                  nil)
                                {:repeat :inf}))))

(comment
  (require '[time-time.converge :refer [converge]])
  (def kick (o/freesound 2086))
  (def hh (o/freesound 178663))
  (let [nome (o/metronome 120)]
    (->> (converge {:durs (repeat 6 1)
                    :tempos [4 7]
                    :cps [5]
                    :bpm 120
                    :period 20})
         (map (fn [voice] (sequencer
                           nome
                           voice
                           (fn [{:keys [tempo-index]} index]
                             (case tempo-index
                               0 (kick)
                               1 (hh))
                             nil)
                           {:repeat nil}))))))
