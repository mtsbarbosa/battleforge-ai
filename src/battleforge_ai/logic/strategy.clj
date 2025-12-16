(ns battleforge-ai.logic.strategy
  "Strategic decision-making for AI: house selection, fight/reap decisions, threat assessment"
  (:require [schema.core :as s]
            [battleforge-ai.models.game :as game]
            [battleforge-ai.models.deck :as deck]
            [battleforge-ai.config.ai-params :as ai-params]))

(s/defn get-creature-amber-potential :- s/Num
  [card :- deck/Card]
  (if (= (:card-type card) :creature)
    (or (:expected-amber card) (:default-creature-amber (ai-params/get-ai-params)))
    0.0))

(s/defn calculate-house-battleline-potential :- s/Num
  [battleline :- [deck/Card]
   house :- deck/House]
  (->> battleline
       (filter #(= (:house %) house))
       (map get-creature-amber-potential)
       (reduce + 0.0)))

(s/defn calculate-battleline-delta :- s/Num
  "Best house potential minus second best house potential"
  [player :- game/Player]
  (let [battleline (:battleline player)
        houses (:houses player)
        house-potentials (map #(calculate-house-battleline-potential battleline %) houses)
        sorted-potentials (sort > house-potentials)]
    (if (>= (count sorted-potentials) 2)
      (- (first sorted-potentials) (second sorted-potentials))
      (or (first sorted-potentials) 0.0))))

(s/defn get-best-battleline-house :- (s/maybe deck/House)
  [player :- game/Player]
  (let [battleline (:battleline player)
        houses (:houses player)]
    (when (seq battleline)
      (->> houses
           (map (fn [house]
                  [house (calculate-house-battleline-potential battleline house)]))
           (sort-by second >)
           (first)
           (first)))))

(s/defn calculate-hand-amber-potential :- s/Num
  [hand :- [deck/Card]
   house :- deck/House]
  (->> hand
       (filter #(= (:house %) house))
       (map #(or (:expected-amber %) (:amber %) (:default-creature-amber (ai-params/get-ai-params))))
       (reduce + 0.0)))

(s/defn get-hand-house-potentials :- {deck/House s/Num}
  [player :- game/Player]
  (let [hand (:hand player)
        houses (:houses player)]
    (into {} (map (fn [house]
                    [house (calculate-hand-amber-potential hand house)])
                  houses))))

(s/defn can-forge-next-turn? :- s/Bool
  [opponent :- game/Player]
  (let [current-amber (:amber opponent)
        key-cost (:key-cost (ai-params/get-ai-params))]
    (>= current-amber key-cost)))

(s/defn is-final-key? :- s/Bool
  [opponent :- game/Player]
  (= (:keys opponent) 2))

(s/defn get-card-amber-control :- s/Num
  "Heuristic estimate of amber control potential (steal/destroy)"
  [card :- deck/Card]
  (let [params (ai-params/get-ai-params)]
    (cond
      (and (= (:card-type card) :action) (> (get card :amber 0) 0)) 
      (:amber-control-action-with-pips params)
      (= (:card-type card) :artifact) 
      (:amber-control-artifact params)
      (and (= (:card-type card) :creature) (> (get card :amber 0) 0)) 
      (:amber-control-creature-with-pips params)
      :else 
      (:amber-control-default params))))

(s/defn calculate-amber-control-potential :- s/Num
  [player :- game/Player
   house :- deck/House]
  (let [hand-control (->> (:hand player)
                         (filter #(= (:house %) house))
                         (map get-card-amber-control)
                         (reduce + 0.0))
        battleline-control (->> (:battleline player)
                               (filter #(= (:house %) house))
                               (map get-card-amber-control)
                               (reduce + 0.0))]
    (+ hand-control battleline-control)))

(s/defn get-house-with-most-amber-control :- deck/House
  [player :- game/Player]
  (let [houses (:houses player)
        house-controls (map (fn [house]
                             [house (calculate-amber-control-potential player house)])
                           houses)]
    (->> house-controls
         (sort-by second >)
         (first)
         (first))))

(s/defn predict-our-amber-potential :- s/Num
  [player :- game/Player
   house :- deck/House]
  (+ (calculate-hand-amber-potential (:hand player) house)
     (calculate-house-battleline-potential (:battleline player) house)))

(s/defn predict-opponent-amber-potential :- s/Num
  [opponent :- game/Player]
  (+ (calculate-battleline-delta opponent)
     (:opponent-hand-amber-estimate (ai-params/get-ai-params))))

(s/defn should-use-amber-control-strategy? :- s/Bool
  [current-player :- game/Player
   opponent :- game/Player]
  (cond
    (and (can-forge-next-turn? opponent) (is-final-key? opponent))
    true
    
    (can-forge-next-turn? opponent)
    (let [houses (:houses current-player)
          our-best-potential (apply max (map #(predict-our-amber-potential current-player %) houses))
          opponent-potential (predict-opponent-amber-potential opponent)
          multiplier (:opponent-potential-multiplier (ai-params/get-ai-params))]
      (< our-best-potential (* multiplier opponent-potential)))
    
    :else false))

(s/defn calculate-battleline-power :- s/Num
  [battleline :- [deck/Card]]
  (->> battleline
       (map #(or (:power %) 0))
       (reduce + 0)))

(s/defn estimate-fight-casualties :- {:our-losses s/Num :their-losses s/Num}
  "Estimate amber potential lost by each side in 1-to-1 fights"
  [our-creatures :- [deck/Card]
   enemy-creatures :- [deck/Card]]
  (let [our-sorted (sort-by #(or (:power %) 0) > our-creatures)
        enemy-sorted (sort-by #(or (:power %) 0) > enemy-creatures)
        fights (map vector our-sorted enemy-sorted)]
    (reduce 
      (fn [{:keys [our-losses their-losses]} [attacker defender]]
        (let [our-power (or (:power attacker) 0)
              their-power (or (:power defender) 0)]
          {:our-losses (if (>= their-power our-power)
                         (+ our-losses (get-creature-amber-potential attacker))
                         our-losses)
           :their-losses (if (>= our-power their-power)
                           (+ their-losses (get-creature-amber-potential defender))
                           their-losses)}))
      {:our-losses 0.0 :their-losses 0.0}
      fights)))

(s/defn calculate-delta-swing :- s/Num
  "Their losses minus our losses. Positive = fight favors us"
  [our-creatures :- [deck/Card]
   enemy-creatures :- [deck/Card]]
  (let [{:keys [our-losses their-losses]} (estimate-fight-casualties our-creatures enemy-creatures)]
    (- their-losses our-losses)))

(s/defn should-fight-or-reap? :- (s/enum :fight :reap)
  "Fight if delta swing is favorable, reap otherwise"
  [current-player :- game/Player
   opponent :- game/Player
   active-house :- deck/House]
  (let [params (ai-params/get-ai-params)
        our-creatures (filter #(= (:house %) active-house) (:battleline current-player))
        enemy-creatures (:battleline opponent)
        our-power (reduce + 0 (map #(or (:power %) 0) our-creatures))
        enemy-power (calculate-battleline-power enemy-creatures)
        amber-to-forge (- (:key-cost params) (:amber current-player))
        opponent-amber (:amber opponent)
        
        no-enemies? (empty? enemy-creatures)
        no-fighters? (<= our-power 0)
        we-near-forge? (<= amber-to-forge (:reap-amber-to-forge-threshold params))
        opponent-near-forge? (>= opponent-amber (:fight-forge-threat-amber params))
        opponent-about-to-win? (and (= (:keys opponent) 2) opponent-near-forge?)
        power-ratio (if (zero? enemy-power) Float/POSITIVE_INFINITY (/ our-power enemy-power))
        severely-outmatched? (< power-ratio (:fight-min-power-ratio params))
        base-delta-swing (calculate-delta-swing our-creatures enemy-creatures)
        delta-swing (if opponent-near-forge?
                      (+ base-delta-swing (:fight-forge-threat-bonus params))
                      base-delta-swing)]
    
    (cond
      no-enemies? :reap
      no-fighters? :reap
      opponent-about-to-win? :fight
      we-near-forge? :reap
      severely-outmatched? :reap
      (>= delta-swing (:fight-min-delta-swing params)) :fight
      :else :reap)))

(s/defn should-stick-with-battleline-house? :- s/Bool
  [current-player :- game/Player
   opponent :- game/Player]
  (let [current-delta (calculate-battleline-delta current-player)
        opponent-delta (calculate-battleline-delta opponent)
        threat-level (- opponent-delta current-delta)
        best-battleline-house (get-best-battleline-house current-player)]
    (boolean 
      (and 
        best-battleline-house
        (> current-delta opponent-delta)
        (> current-delta 0)
        (let [high-threshold (:high-threat-threshold (ai-params/get-ai-params))
              advantage-req (:battleline-advantage-vs-high-threat (ai-params/get-ai-params))]
          (or (<= threat-level high-threshold)
              (> current-delta (+ opponent-delta advantage-req))))))))

(s/defn should-switch-for-hand-advantage? :- [(s/one s/Bool "should-switch") (s/one (s/maybe deck/House) "house")]
  [current-player :- game/Player
   opponent :- game/Player]
  (let [current-delta (calculate-battleline-delta current-player)
        opponent-delta (calculate-battleline-delta opponent)
        threat-level (- opponent-delta current-delta)
        best-battleline-house (get-best-battleline-house current-player)
        hand-potentials (get-hand-house-potentials current-player)
        best-hand-entry (reduce (fn [best [house potential]]
                                  (if (> potential (second best)) [house potential] best))
                                [nil 0.0]
                                hand-potentials)
        [best-hand-house best-hand-potential] best-hand-entry
        battleline-potential (if best-battleline-house
                               (calculate-house-battleline-potential 
                                 (:battleline current-player) best-battleline-house)
                               0.0)
        advantage-threshold (ai-params/get-advantage-threshold threat-level)
        delta-threshold (ai-params/get-delta-threshold threat-level)
        min-cards-advantage (:min-cards-for-hand-advantage (ai-params/get-ai-params))
        min-cards-switch (:min-cards-for-house-switch (ai-params/get-ai-params))]
    
    (cond
      (and (> best-hand-potential (* battleline-potential advantage-threshold))
           (>= (count (filter #(= (:house %) best-hand-house) (:hand current-player))) min-cards-advantage))
      [true best-hand-house]
      
      (and (<= current-delta delta-threshold)
           best-battleline-house
           (= 0 (count (filter #(= (:house %) best-battleline-house) (:hand current-player))))
           (>= (count (filter #(= (:house %) best-hand-house) (:hand current-player))) min-cards-switch))
      [true best-hand-house]
      
      :else [false nil])))

(s/defn choose-house-strategic :- deck/House
  [current-player :- game/Player
   opponent :- game/Player]
  (cond
    (should-use-amber-control-strategy? current-player opponent)
    (get-house-with-most-amber-control current-player)
    
    (should-stick-with-battleline-house? current-player opponent)
    (get-best-battleline-house current-player)
    
    :else
    (let [[should-switch? house] (should-switch-for-hand-advantage? current-player opponent)]
      (if should-switch?
        house
        (let [current-delta (calculate-battleline-delta current-player)
              opponent-delta (calculate-battleline-delta opponent)
              threat-level (- opponent-delta current-delta)
              houses (:houses current-player)
              house-counts (frequencies (map :house (:hand current-player)))
              high-threshold (:high-threat-threshold (ai-params/get-ai-params))]
          (if (> threat-level high-threshold)
            (let [hand-potentials (get-hand-house-potentials current-player)
                  best-entry (reduce (fn [best [h p]] (if (> p (second best)) [h p] best))
                                     [nil 0.0] hand-potentials)]
              (first best-entry))
            (reduce (fn [best h] (if (> (get house-counts h 0) (get house-counts best 0)) h best))
                    (first houses) houses)))))))