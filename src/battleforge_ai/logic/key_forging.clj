(ns battleforge-ai.logic.key-forging
  (:require [schema.core :as s]
            [battleforge-ai.models.game :as game]))

;; ============================================================================
;; Key Forging Logic (Pure Functions)
;; ============================================================================

(def ^:private key-cost 6)

(s/defn get-current-key-cost :- s/Int
  "Get the current cost to forge a key (base 6, may be modified later)"
  [player :- game/Player]
  ;; For now, always 6 amber
  ;; In full implementation, this could be modified by cards/effects
  key-cost)

(s/defn can-forge-key? :- s/Bool
  "Check if player can forge a key"
  [player :- game/Player]
  (let [current-cost (get-current-key-cost player)]
    (and (>= (:amber player) current-cost)
         (< (:keys player) 3))))

(s/defn forge-key :- game/Player
  "Forge a key for the player, spending the required amber"
  [player :- game/Player]
  (if (can-forge-key? player)
    (let [cost (get-current-key-cost player)]
      (-> player
          (update :amber - cost)
          (update :keys inc)))
    player))

(s/defn process-key-phase :- game/Player
  "Process the key forging phase for a player.
   Key forging is mandatory if the player can afford it."
  [player :- game/Player]
  (if (can-forge-key? player)
    (forge-key player)
    player))

(s/defn process-key-forging :- game/Player
  "Process key forging for a player (alias for process-key-phase)"
  [player :- game/Player]
  (process-key-phase player))

(s/defn get-forging-message :- s/Str
  "Get a message describing the key forging action"
  [player :- game/Player
   forged? :- s/Bool]
  (if forged?
    (format "%s forges a key! (Keys: %d/3, Amber: %d)" 
            (:id player) (:keys player) (:amber player))
    (format "%s does not forge a key. (Amber: %d/%d, Keys: %d/3)" 
            (:id player) (:amber player) (get-current-key-cost player) (:keys player)))) 