(ns battleforge-ai.logic.mulligan
  (:require [clojure.string :as str]
            [schema.core :as s]
            [battleforge-ai.models.deck :as deck]
            [battleforge-ai.models.game :as game]
            [battleforge-ai.logic.game-state :as game-state]))

;; ============================================================================
;; Mulligan Logic (Pure Functions)
;; ============================================================================

(s/defn has-creature-control? :- s/Bool
  "Check if a card has high creature control (likely board wipes or creature removal)"
  [card :- deck/Card]
  (> (:creature-control card 0) 2))

(s/defn has-amber-control? :- s/Bool
  "Check if a card has high amber control (likely amber steal/manipulation)"
  [card :- deck/Card]
  (> (:amber-control card 0) 1))

(s/defn is-early-game-useless? :- s/Bool
  "Check if a card is likely useless in early game"
  [card :- deck/Card]
  (or (has-creature-control? card)
      (has-amber-control? card)))

(s/defn analyze-house-distribution :- {deck/House s/Int}
  "Analyze the distribution of cards by house in hand"
  [hand :- [deck/Card]]
  (frequencies (map :house hand)))

(s/defn has-bad-house-distribution? :- s/Bool
  "Check if hand has poor house distribution (2/2/2 pattern)"
  [hand :- [deck/Card]]
  (let [house-dist (analyze-house-distribution hand)
        hand-size (count hand)]
    (and (= hand-size 6)
         (= (count house-dist) 3)
         (every? #(= % 2) (vals house-dist)))))

(s/defn should-mulligan? :- s/Bool
  "AI decision: should this player take a mulligan based on their starting hand?
   
   Key heuristics:
   - 2/2/2 house distribution is a strong indicator of a bad hand
   - Too many board wipes or amber control cards (useless early game)
   - No amber sources (cards with amber pips)
   - No creatures or actions to play early game"
  [player :- game/Player]
  (let [hand (:hand player)
        hand-size (count hand)
        {:keys [amber-sources playable-cards total-amber useless-early-cards house-dist]}
        (reduce (fn [acc card]
                  (let [amber (:amber card)
                        house (:house card)]
                    (-> acc
                        (update :amber-sources (if (> amber 0) inc identity))
                        (update :playable-cards (if (<= amber 2) inc identity))
                        (update :total-amber + amber)
                        (update :useless-early-cards (if (is-early-game-useless? card) inc identity))
                        (update :house-dist (fn [dist] (update dist house (fnil inc 0)))))))
                {:amber-sources 0
                 :playable-cards 0
                 :total-amber 0
                 :useless-early-cards 0
                 :house-dist {}}
                hand)
        is-2-2-2 (and (= hand-size 6)
                      (= (count house-dist) 3)
                      (every? #(= % 2) (vals house-dist)))]
    
    (or 
      is-2-2-2
      (>= useless-early-cards 3)
      (= amber-sources 0)
      (< playable-cards 2)
      (< total-amber 3))))

(s/defn execute-mulligan :- game/Player
  "Execute mulligan: put hand back in deck, shuffle, draw (hand size - 1) cards"
  [player :- game/Player]
  (let [new-hand-size (dec (count (:hand player)))]
    (game-state/shuffle-deck-with-hand player new-hand-size)))

(s/defn process-mulligan-decision :- game/Player
  "Process AI mulligan decision for a player"
  [player :- game/Player]
  (if (should-mulligan? player)
    (execute-mulligan player)
    player))

(s/defn analyze-hand-quality :- s/Str
  "Analyze hand quality for logging purposes"
  [player :- game/Player]
  (let [hand (:hand player)
        hand-size (count hand)
        {:keys [amber-sources playable-cards total-amber creature-count action-count 
                useless-early-cards house-dist]} 
        (reduce (fn [acc card]
                  (let [amber (:amber card)
                        house (:house card)]
                    (-> acc
                        (update :amber-sources (if (> amber 0) inc identity))
                        (update :playable-cards (if (<= amber 2) inc identity))
                        (update :total-amber + amber)
                        (update :useless-early-cards (if (is-early-game-useless? card) inc identity))
                        (update :house-dist (fn [dist] (update dist house (fnil inc 0)))))))
                {:amber-sources 0
                 :playable-cards 0
                 :total-amber 0
                 :useless-early-cards 0
                 :house-dist {}}
                hand)
        is-2-2-2 (and (= hand-size 6)
                      (= (count house-dist) 3)
                      (every? #(= % 2) (vals house-dist)))]
    
    (format "Hand: %d cards, %s distribution, %d amber sources, %d playable early, %d total amber, %d creatures, %d actions, %d useless early"
            hand-size
            (if is-2-2-2 "2/2/2 (BAD)" (str "houses: " (vals house-dist)))
            amber-sources playable-cards total-amber creature-count action-count useless-early-cards)))