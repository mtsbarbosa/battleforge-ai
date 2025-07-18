(require '[battleforge-ai.logic.mulligan :as mulligan])

;; Define test cards with different amber values
(def zero-amber-card   {:amber 0 :house :brobnar :card-type :creature})
(def one-amber-card    {:amber 1 :house :dis :card-type :action})
(def two-amber-card    {:amber 2 :house :logos :card-type :creature})
(def three-amber-card  {:amber 3 :house :brobnar :card-type :action})

;; Test hands with different amber characteristics
(def no-amber-hand [zero-amber-card zero-amber-card zero-amber-card 
                    zero-amber-card zero-amber-card zero-amber-card])

(def low-amber-hand [one-amber-card one-amber-card zero-amber-card
                     zero-amber-card zero-amber-card zero-amber-card])

(def good-amber-hand [three-amber-card two-amber-card one-amber-card
                      one-amber-card zero-amber-card zero-amber-card])

;; Calculate metrics for each hand
(defn analyze-hand [hand]
  (let [amber-sources (count (filter #(> (:amber %) 0) hand))
        total-amber (reduce + (map :amber hand))]
    {:hand-name (if (= total-amber 0) "no-amber"
                   (if (< total-amber 3) "low-amber" "good-amber"))
     :amber-sources amber-sources
     :total-amber total-amber
     :should-mulligan? (< total-amber 3)}))

(println "🔍 AMBER METRICS IN MULLIGAN DECISIONS")
(println "=" 50)

(doseq [hand [no-amber-hand low-amber-hand good-amber-hand]]
  (let [result (analyze-hand hand)]
    (println (format "📋 %-12s | Amber Sources: %d | Total Amber: %d | Mulligan: %s"
                    (:hand-name result)
                    (:amber-sources result) 
                    (:total-amber result)
                    (:should-mulligan? result)))))
