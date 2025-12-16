(ns battleforge-ai.logic.simple-battle
  (:require [schema.core :as s]
            [battleforge-ai.models.game :as game]
            [battleforge-ai.models.deck :as deck]
            [battleforge-ai.logic.battle-modes :as battle-modes]
            [battleforge-ai.logic.game-state :as game-state]
            [battleforge-ai.logic.amber-pips :as amber-pips]
            [battleforge-ai.logic.strategy :as strategy]))

(s/defn play-card-simple :- game/Player
  [player :- game/Player
   card :- deck/Card]
  (let [player-after-amber (amber-pips/resolve-amber-pips player card)
        updated-hand (vec (remove #(= % card) (:hand player-after-amber)))
        player-after-hand (assoc player-after-amber :hand updated-hand)]
    
    (if (= (:card-type card) :creature)
      (update player-after-hand :battleline conj card)
      (update player-after-hand :discard conj card))))

(s/defn play-cards-simple :- game/Player
  "Play all cards from active house, prioritizing high amber value"
  [player :- game/Player
   active-house :- deck/House]
  (let [playable-cards (filter #(= (:house %) active-house) (:hand player))
        sorted-cards (sort-by amber-pips/get-card-amber-value > playable-cards)]
    (reduce play-card-simple player sorted-cards)))

(defn get-creature-power [creature]
  (- (or (:power creature) 0) (or (:damage creature) 0)))

(defn is-destroyed? [creature]
  (<= (get-creature-power creature) 0))

(defn deal-damage [creature damage-amount]
  (update creature :damage (fnil + 0) damage-amount))

(defn remove-destroyed-creatures [player]
  (let [battleline (:battleline player)
        alive (vec (remove is-destroyed? battleline))
        dead (filter is-destroyed? battleline)
        clean-dead (map #(dissoc % :damage) dead)]
    (-> player
        (assoc :battleline alive)
        (update :discard into clean-dead))))

(defn fight-creatures
  "Both creatures deal damage equal to their power. Returns [attacker defender]"
  [attacker defender]
  [(deal-damage attacker (or (:power defender) 0))
   (deal-damage defender (or (:power attacker) 0))])

(s/defn use-creatures-simple
  "Use creatures from active house - fight or reap based on strategy"
  [game-state :- game/GameState
   active-house :- deck/House]
  (let [current-player (game-state/get-current-player game-state)
        opponent (game-state/get-opponent game-state)
        my-creatures (filter #(= (:house %) active-house) (:battleline current-player))
        enemy-creatures (:battleline opponent)
        decision (strategy/should-fight-or-reap? current-player opponent active-house)
        should-fight? (and (= decision :fight) (seq enemy-creatures) (seq my-creatures))]
    
    (if should-fight?
      (let [fights (map vector my-creatures enemy-creatures)
            [updated-my-creatures updated-enemy-creatures]
            (reduce
              (fn [[my-acc enemy-acc] [attacker defender]]
                (let [[damaged-attacker damaged-defender] (fight-creatures attacker defender)]
                  [(conj my-acc damaged-attacker) (conj enemy-acc damaged-defender)]))
              [[] []]
              fights)
            my-non-fighters (drop (count fights) my-creatures)
            enemy-non-fighters (drop (count fights) enemy-creatures)
            updated-my-battleline (vec (concat 
                                         updated-my-creatures 
                                         my-non-fighters
                                         (remove #(= (:house %) active-house) (:battleline current-player))))
            updated-enemy-battleline (vec (concat updated-enemy-creatures enemy-non-fighters))
            current-after-fight (-> current-player
                                    (assoc :battleline updated-my-battleline)
                                    remove-destroyed-creatures)
            opponent-after-fight (-> opponent
                                     (assoc :battleline updated-enemy-battleline)
                                     remove-destroyed-creatures)]
        {:current-player current-after-fight
         :opponent opponent-after-fight
         :creatures-fought (count fights)
         :creatures-reaped 0})
      
      (let [amber-gained (count my-creatures)]
        {:current-player (update current-player :amber + amber-gained)
         :opponent opponent
         :creatures-fought 0
         :creatures-reaped amber-gained}))))

(s/defn execute-play-phase-simple :- game/GameState
  "Play cards from active house, then use creatures (fight or reap)"
  [game-state :- game/GameState]
  (let [current-player (game-state/get-current-player game-state)
        active-house (:active-house game-state)
        player-after-play (play-cards-simple current-player active-house)
        cards-played (- (count (:hand current-player)) (count (:hand player-after-play)))
        state-after-play (game-state/update-current-player game-state player-after-play)
        {:keys [current-player opponent creatures-fought creatures-reaped]} 
        (use-creatures-simple state-after-play active-house)
        updated-state (-> state-after-play
                         (game-state/update-current-player current-player)
                         (game-state/update-opponent opponent)
                         (game-state/advance-phase :ready))
        action-msg (cond
                     (> creatures-fought 0) (format "fights with %d creatures" creatures-fought)
                     (> creatures-reaped 0) (format "reaps with %d creatures" creatures-reaped)
                     :else "uses no creatures")]
    
    (game-state/add-to-game-log 
      updated-state
      (format "%s plays %d cards, %s (amber: %d, battleline: %d)" 
              (:id current-player) 
              cards-played 
              action-msg 
              (:amber current-player)
              (count (:battleline current-player))))))

(defrecord SimpleBattleHandler []
  battle-modes/BattleModeHandler
  
  (execute-play-phase [_this game-state]
    (execute-play-phase-simple game-state))
  
  (play-card [_this player card]
    (play-card-simple player card))
  
  (get-mode-name [_this]
    "Simple Battle")
  
  (get-mode-description [_this]
    "Fast simulation using amber heuristics and strategic decision-making. Cards are played for amber value without specific abilities."))

(defn simple-battle-handler []
  (->SimpleBattleHandler))