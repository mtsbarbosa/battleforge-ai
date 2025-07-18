(ns battleforge-ai.logic.game-flow
  (:require [schema.core :as s]
            [battleforge-ai.models.game :as game]
            [battleforge-ai.models.deck :as deck]
            [battleforge-ai.logic.game-state :as game-state]
            [battleforge-ai.logic.mulligan :as mulligan]
            [battleforge-ai.logic.key-forging :as key-forging]
            [battleforge-ai.logic.amber-pips :as amber-pips]
            [java-time :as time]))

;; ============================================================================
;; Game Flow Orchestration (Pure Functions)
;; ============================================================================

(s/defn execute-setup-phase :- game/GameState
  "Execute the setup phase: first player draws extra card, handle mulligans"
  [game-state :- game/GameState]
  (let [current-player (game-state/get-current-player game-state)
        opponent (game-state/get-opponent game-state)
        
        first-player-bonus (if (:first-turn? game-state)
                            (game-state/draw-card current-player)
                            current-player)
        
        current-after-mulligan (mulligan/process-mulligan-decision first-player-bonus)
        opponent-after-mulligan (mulligan/process-mulligan-decision opponent)
        
        updated-state (-> game-state
                         (game-state/update-current-player current-after-mulligan)
                         (game-state/update-opponent opponent-after-mulligan)
                         (game-state/advance-phase :forge))]
    
    ;; Add mulligan messages to log
    (-> updated-state
        (game-state/add-to-game-log 
          (format "Setup: %s" (mulligan/analyze-hand-quality current-after-mulligan)))
        (game-state/add-to-game-log 
          (format "Setup: %s" (mulligan/analyze-hand-quality opponent-after-mulligan))))))

(s/defn execute-forge-phase :- game/GameState
  "Execute the key forging phase"
  [game-state :- game/GameState]
  (let [current-player (game-state/get-current-player game-state)
        can-forge-before? (key-forging/can-forge-key? current-player)
        updated-player (key-forging/process-key-phase current-player)
        forged? (and can-forge-before? (> (:keys updated-player) (:keys current-player)))
        
        updated-state (-> game-state
                         (game-state/update-current-player updated-player))
        
        winner (game-state/get-winner updated-state)]
    
    (if winner
      (-> updated-state
          (assoc :phase :end :ended-at (java.util.Date/from (time/instant)))
          (game-state/add-to-game-log
            (key-forging/get-forging-message updated-player forged?))
          (game-state/add-to-game-log
            (format "🎉 %s wins the game with 3 keys!" (:id updated-player))))
      (-> updated-state
          (game-state/advance-phase :choose)
          (game-state/add-to-game-log
            (key-forging/get-forging-message updated-player forged?))))))

(s/defn choose-house-simple :- deck/House
  "Simple AI house selection: choose house with most cards in hand"
  [player :- game/Player
   deck :- deck/Deck]
  (let [houses (:houses deck)
        hand (:hand player)
        house-counts (frequencies (map :house hand))
        best-house (apply max-key #(get house-counts % 0) houses)]
    best-house))

(s/defn execute-choose-phase :- game/GameState
  "Execute the house selection phase"
  [game-state :- game/GameState]
  (let [current-player (game-state/get-current-player game-state)
        houses (:houses current-player)
        ;; Simple AI: choose house with most cards in hand
        hand (:hand current-player)
        house-counts (frequencies (map :house hand))
        chosen-house (apply max-key #(get house-counts % 0) houses)
        
        updated-state (-> game-state
                         (assoc :active-house chosen-house)
                         (game-state/advance-phase :play))]
    
    (game-state/add-to-game-log 
      updated-state
      (format "%s chooses %s as their active house" 
              (:id current-player) chosen-house))))

(s/defn play-cards-simple :- game/Player
  "Simple AI card playing: play cards with amber pips prioritizing high amber value"
  [player :- game/Player
   active-house :- deck/House]
  (let [hand (:hand player)
        ;; Filter cards that belong to active house
        playable-cards (filter #(= (:house %) active-house) hand)
        ;; Sort by amber value (descending)
        sorted-cards (sort-by amber-pips/get-card-amber-value > playable-cards)
        ;; For now, play first card (highest amber)
        card-to-play (first sorted-cards)]
    
    (if card-to-play
      (let [;; Resolve amber pips from the card
            player-after-amber (amber-pips/resolve-amber-pips player card-to-play)
            ;; Remove card from hand (simplified - no discard pile yet)
            updated-hand (vec (remove #(= % card-to-play) (:hand player-after-amber)))]
        (assoc player-after-amber :hand updated-hand))
      player)))

(s/defn execute-play-phase :- game/GameState
  "Execute the main play phase where cards are played"
  [game-state :- game/GameState]
  (let [current-player (game-state/get-current-player game-state)
        active-house (:active-house game-state)
        updated-player (play-cards-simple current-player active-house)
        
        updated-state (-> game-state
                         (game-state/update-current-player updated-player)
                         (game-state/advance-phase :ready))]
    
    ;; For now, simple message
    (game-state/add-to-game-log 
      updated-state
      (format "%s plays cards" (:id updated-player)))))

(s/defn execute-ready-phase :- game/GameState
  "Execute the ready phase (for now, just advance)"
  [game-state :- game/GameState]
  (-> game-state
      (game-state/advance-phase :draw)
      (game-state/add-to-game-log 
        (format "%s readies their cards" 
                (:id (game-state/get-current-player game-state))))))

(s/defn draw-cards :- game/Player
  "Draw cards to fill hand (usually to 6 cards)"
  [player :- game/Player]
  (let [hand (:hand player)
        deck (:deck player)
        target-hand-size 6
        cards-to-draw (max 0 (- target-hand-size (count hand)))
        cards-to-draw (min cards-to-draw (count deck))
        
        drawn-cards (take cards-to-draw deck)
        new-hand (vec (concat hand drawn-cards))
        new-deck (vec (drop cards-to-draw deck))]
    
    (-> player
        (assoc :hand new-hand)
        (assoc :deck new-deck))))

(s/defn execute-draw-phase :- game/GameState
  "Execute the draw phase"
  [game-state :- game/GameState]
  (let [current-player (game-state/get-current-player game-state)
        updated-player (draw-cards current-player)
        
        updated-state (-> game-state
                         (game-state/update-current-player updated-player)
                         (game-state/advance-phase :end))]
    
    (game-state/add-to-game-log 
      updated-state
      (format "%s draws cards (Hand: %d)" 
              (:id updated-player) (count (:hand updated-player))))))

(s/defn execute-turn :- game/GameState
  "Execute a complete turn for the current player"
  [game-state :- game/GameState]
  (-> game-state
      execute-forge-phase
      execute-choose-phase  
      execute-play-phase
      execute-ready-phase
      execute-draw-phase
      game-state/switch-active-player
      (game-state/advance-phase :forge)))

(s/defn simulate-game :- game/GameState
  "Simulate a complete game until someone wins"
  [initial-state :- game/GameState]
  (loop [state (execute-setup-phase initial-state)
         max-turns 1000]
    
    (cond
      (game-state/game-over? state)
      (assoc state :phase :end :ended-at (java.util.Date/from (time/instant)))
      
      (<= max-turns 0)
      (-> state
          (assoc :phase :end :ended-at (java.util.Date/from (time/instant)))
          (game-state/add-to-game-log "Game ended due to turn limit"))
      
      :else
      (recur (execute-turn state) (dec max-turns)))))