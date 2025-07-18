(ns battleforge-ai.logic.amber-pips
  (:require [schema.core :as s]
            [battleforge-ai.models.game :as game]
            [battleforge-ai.models.deck :as deck]))

;; ============================================================================
;; Amber Pips Resolution Logic (Pure Functions)
;; ============================================================================

(s/defn get-card-amber-value :- s/Int
  "Get the amber value (pips) from a card"
  [card :- deck/Card]
  (or (:amber card) 0))

(s/defn get-amber-value :- s/Int
  "Get the amber value (pips) from a card (alias for get-card-amber-value)"
  [card :- deck/Card]
  (get-card-amber-value card))

(s/defn resolve-amber-pips :- game/Player
  "Resolve amber pips when a card is played - gain amber equal to card's amber value"
  [player :- game/Player
   card :- deck/Card]
  (let [amber-gain (get-card-amber-value card)]
    (if (> amber-gain 0)
      (update player :amber + amber-gain)
      player)))

(s/defn get-amber-gain-message :- s/Str
  "Get message describing amber gained from card"
  [player :- game/Player
   card :- deck/Card
   amber-gained :- s/Int]
  (if (> amber-gained 0)
    (format "%s gains %d amber from %s (Total: %d)" 
            (:id player) amber-gained (:name card) (:amber player))
    (format "%s plays %s (no amber gained)" 
            (:id player) (:name card))))

(s/defn calculate-total-amber-in-hand :- s/Int
  "Calculate total potential amber from all cards in hand"
  [player :- game/Player]
  (reduce + (map get-card-amber-value (:hand player))))

(s/defn get-best-amber-cards :- [deck/Card]
  "Get cards from hand sorted by amber value (highest first)"
  [player :- game/Player]
  (->> (:hand player)
       (sort-by get-card-amber-value >)))

(s/defn has-amber-sources? :- s/Bool
  "Check if player has any cards with amber pips in hand"
  [player :- game/Player]
  (some #(> (get-card-amber-value %) 0) (:hand player)))

;; ============================================================================
;; Amber-based AI Decision Making
;; ============================================================================

(s/defn should-prioritize-amber-cards? :- s/Bool
  "AI decision: should we prioritize playing cards with amber pips?
   
   Heuristics:
   - If we're close to forging a key (need < 4 amber)
   - If opponent is close to forging (apply pressure)
   - Early game amber acceleration"
  [player :- game/Player
   opponent :- game/Player]
  (or 
    ;; Close to forging our own key
    (<= (- 6 (:amber player)) 3)
    ;; Opponent is close to forging
    (>= (:amber opponent) 4)
    ;; Early game (first 3 turns)
    (< (:keys player) 1)))

(s/defn get-amber-priority-score :- s/Num
  "Get priority score for a card based on amber value and other factors"
  [card :- deck/Card]
  (let [amber-value (get-card-amber-value card)
        type-bonus (case (:card-type card)
                     :action 0.5   ; Actions are usually immediate value
                     :creature 0.3 ; Creatures provide ongoing value
                     :artifact 0.2 ; Artifacts provide utility
                     :upgrade 0.1  ; Upgrades are situational
                     0.0)]
    (+ amber-value type-bonus)))

(s/defn calculate-card-priority :- s/Int
  "Calculate card priority based on amber value (simple version for tests)"
  [card :- deck/Card]
  (get-card-amber-value card))

(s/defn choose-card-to-play :- (s/maybe deck/Card)
  "Choose the best card to play from hand based on amber value"
  [player :- game/Player]
  (when-let [hand (:hand player)]
    (when (seq hand)
      (apply max-key get-card-amber-value hand))))

(s/defn analyze-amber-potential :- s/Str
  "Analyze a player's amber situation for logging purposes"
  [player :- game/Player]
  (let [current-amber (:amber player)
        hand (:hand player)
        hand-potential (calculate-total-amber-in-hand player)
        card-count (count hand)]
    (format "Current amber: %d, Hand amber potential: %d, %d cards in hand"
            current-amber hand-potential card-count)))