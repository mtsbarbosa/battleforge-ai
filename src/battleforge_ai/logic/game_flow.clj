(ns battleforge-ai.logic.game-flow
  (:require [schema.core :as s]
            [battleforge-ai.models.game :as game]
            [battleforge-ai.logic.game-state :as game-state]
            [battleforge-ai.logic.mulligan :as mulligan]
            [battleforge-ai.logic.key-forging :as key-forging]
            [battleforge-ai.logic.battle-modes :as battle-modes]
            [battleforge-ai.logic.strategy :as strategy]
            [java-time.api :as time]))

(s/defn execute-setup-phase
  :-
  game/GameState
  [game-state :- game/GameState]
  (let [current-player (game-state/get-current-player game-state)
        opponent (game-state/get-opponent game-state)
        first-player-bonus (if (:first-turn? game-state)
                             (game-state/draw-card current-player)
                             current-player)
        current-after-mulligan (mulligan/process-mulligan-decision
                                 first-player-bonus)
        opponent-after-mulligan (mulligan/process-mulligan-decision opponent)
        updated-state (-> game-state
                          (game-state/update-current-player
                            current-after-mulligan)
                          (game-state/update-opponent opponent-after-mulligan)
                          (game-state/advance-phase :forge))]
    (-> updated-state
        (game-state/add-to-game-log (format "Setup: %s"
                                            (mulligan/analyze-hand-quality
                                              current-after-mulligan)))
        (game-state/add-to-game-log (format "Setup: %s"
                                            (mulligan/analyze-hand-quality
                                              opponent-after-mulligan))))))

(s/defn execute-forge-phase
  :-
  game/GameState
  [game-state :- game/GameState]
  (let [current-player (game-state/get-current-player game-state)
        can-forge-before? (key-forging/can-forge-key? current-player)
        updated-player (key-forging/process-key-phase current-player)
        forged? (and can-forge-before?
                     (> (:keys updated-player) (:keys current-player)))
        updated-state (-> game-state
                          (game-state/update-current-player updated-player))
        winner (game-state/get-winner updated-state)]
    (if winner
      (-> updated-state
          (assoc :phase :end
                 :ended-at (java.util.Date/from (time/instant)))
          (game-state/add-to-game-log
            (key-forging/get-forging-message updated-player forged?))
          (game-state/add-to-game-log (format "🎉 %s wins the game with 3 keys!"
                                              (:id updated-player))))
      (-> updated-state
          (game-state/advance-phase :choose)
          (game-state/add-to-game-log
            (key-forging/get-forging-message updated-player forged?))))))

(s/defn execute-choose-phase
  :-
  game/GameState
  [game-state :- game/GameState]
  (let [current-player (game-state/get-current-player game-state)
        opponent (game-state/get-opponent game-state)
        chosen-house (strategy/choose-house-strategic current-player opponent)
        updated-state (-> game-state
                          (assoc :active-house chosen-house)
                          (game-state/advance-phase :play))]
    (game-state/add-to-game-log
      updated-state
      (format "%s chooses %s as their active house (Battleline delta: %.1f)"
              (:id current-player)
              chosen-house
              (strategy/calculate-battleline-delta current-player)))))

(s/defn execute-play-phase
  :-
  game/GameState
  [game-state :- game/GameState]
  (let [battle-mode (or (:battle-mode game-state)
                        (battle-modes/get-default-battle-mode))
        battle-handler (battle-modes/get-battle-mode-handler battle-mode)]
    (battle-modes/execute-play-phase battle-handler game-state)))

(s/defn execute-ready-phase
  :-
  game/GameState
  [game-state :- game/GameState]
  (-> game-state
      (game-state/advance-phase :draw)
      (game-state/add-to-game-log (format "%s readies their cards"
                                          (:id (game-state/get-current-player
                                                 game-state))))))

(s/defn draw-cards
  :-
  game/Player
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

(s/defn execute-draw-phase
  :-
  game/GameState
  [game-state :- game/GameState]
  (let [current-player (game-state/get-current-player game-state)
        updated-player (draw-cards current-player)
        updated-state (-> game-state
                          (game-state/update-current-player updated-player)
                          (game-state/advance-phase :end))]
    (game-state/add-to-game-log updated-state
                                (format "%s draws cards (Hand: %d)"
                                        (:id updated-player)
                                        (count (:hand updated-player))))))

(s/defn execute-turn
  :-
  game/GameState
  [game-state :- game/GameState]
  (-> game-state
      execute-forge-phase
      execute-choose-phase
      execute-play-phase
      execute-ready-phase
      execute-draw-phase
      game-state/switch-active-player
      (game-state/advance-phase :forge)))

(s/defn simulate-game
  :-
  game/GameState
  [initial-state :- game/GameState]
  (loop [state (execute-setup-phase initial-state)
         max-turns 1000]
    (cond (game-state/game-over? state) (assoc state
                                          :phase :end
                                          :ended-at (java.util.Date/from
                                                      (time/instant)))
          (<= max-turns 0)
            (-> state
                (assoc :phase :end
                       :ended-at (java.util.Date/from (time/instant)))
                (game-state/add-to-game-log "Game ended due to turn limit"))
          :else (recur (execute-turn state) (dec max-turns)))))