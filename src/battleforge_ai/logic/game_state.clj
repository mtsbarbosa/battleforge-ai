(ns battleforge-ai.logic.game-state
  (:require [schema.core :as s]
            [battleforge-ai.models.game :as game]
            [battleforge-ai.models.deck :as deck]
            [java-time :as time]))

;; ============================================================================
;; Game State Creation and Management (Pure Functions)
;; ============================================================================

(s/defn create-initial-player :- game/Player
  "Create initial player state from a deck"
  [player-id :- s/Str
   deck :- deck/Deck]
  (let [shuffled-deck (shuffle (:cards deck))]
    {:id            player-id
     :deck          (vec (drop 7 shuffled-deck))  ; Remove starting hand
     :hand          (vec (take 7 shuffled-deck))  ; Starting hand of 7
     :discard       []
     :purged        []
     :archive       []
     :battleline    []
     :artifacts     []
     :houses        (:houses deck)                ; Houses from the deck
     :amber         0
     :keys          0
     :chains        (or (:chains deck) 0)
     :ready-amber   0}))

(s/defn create-initial-game-state :- game/GameState
  "Create initial game state from two decks"
  [deck1 :- deck/Deck
   deck2 :- deck/Deck
   game-id :- s/Str]
  (let [player1 (create-initial-player "player1" deck1)
        player2 (create-initial-player "player2" deck2)
        first-player (if (< (rand) 0.5) :player1 :player2)]
    {:id              game-id
     :player1         player1
     :player2         player2
     :current-player  first-player
     :turn-count      1
     :phase           :setup
     :active-house    nil
     :first-turn?     true
     :game-log        []
     :started-at      (java.util.Date/from (time/instant))}))

(s/defn get-current-player :- game/Player
  "Get the current active player"
  [game-state :- game/GameState]
  (if (= (:current-player game-state) :player1)
    (:player1 game-state)
    (:player2 game-state)))

(s/defn get-opponent :- game/Player
  "Get the opponent of the current player"
  [game-state :- game/GameState]
  (if (= (:current-player game-state) :player1)
    (:player2 game-state)
    (:player1 game-state)))

(s/defn update-current-player :- game/GameState
  "Update the current player's state"
  [game-state :- game/GameState
   updated-player :- game/Player]
  (if (= (:current-player game-state) :player1)
    (assoc game-state :player1 updated-player)
    (assoc game-state :player2 updated-player)))

(s/defn update-opponent :- game/GameState
  "Update the opponent's state"
  [game-state :- game/GameState
   updated-opponent :- game/Player]
  (if (= (:current-player game-state) :player1)
    (assoc game-state :player2 updated-opponent)
    (assoc game-state :player1 updated-opponent)))

(s/defn update-player :- game/GameState
  "Update a specific player's state by player keyword"
  [game-state :- game/GameState
   player-key :- (s/enum :player1 :player2)
   updated-player :- game/Player]
  (assoc game-state player-key updated-player))

(s/defn switch-active-player :- game/GameState
  "Switch to the other player"
  [game-state :- game/GameState]
  (let [new-player (if (= (:current-player game-state) :player1) :player2 :player1)
        new-turn-count (if (= new-player :player1) 
                         (inc (:turn-count game-state))
                         (:turn-count game-state))]
    (-> game-state
        (assoc :current-player new-player)
        (assoc :turn-count new-turn-count)
        (assoc :first-turn? false))))

(s/defn can-forge-key? :- s/Bool
  "Check if a player can forge a key (has 6+ amber and less than 3 keys)"
  [player :- game/Player]
  (and (>= (:amber player) 6)
       (< (:keys player) 3)))

(s/defn has-won? :- s/Bool
  "Check if a player has won the game (has 3 keys)"
  [player :- game/Player]
  (>= (:keys player) 3))

(s/defn game-over? :- s/Bool
  "Check if the game has ended (someone has 3 keys)"
  [game-state :- game/GameState]
  (or (>= (:keys (:player1 game-state)) 3)
      (>= (:keys (:player2 game-state)) 3)))

(s/defn get-winner :- (s/maybe (s/enum :player1 :player2))
  "Get the winner if the game is over"
  [game-state :- game/GameState]
  (cond
    (>= (:keys (:player1 game-state)) 3) :player1
    (>= (:keys (:player2 game-state)) 3) :player2
    :else nil))

(s/defn check-win-condition :- (s/maybe (s/enum :player1 :player2))
  "Check and return the winner if someone has won"
  [game-state :- game/GameState]
  (get-winner game-state))

(s/defn add-to-game-log :- game/GameState
  "Add a message to the game log"
  [game-state :- game/GameState
   message :- s/Str]
  (update game-state :game-log conj message))

(s/defn advance-phase :- game/GameState
  "Advance to the next phase in the turn"
  [game-state :- game/GameState
   next-phase :- game/GamePhase]
  (assoc game-state :phase next-phase))

(s/defn advance-turn :- game/GameState
  "Advance to the next turn (increment turn counter)"
  [game-state :- game/GameState]
  (update game-state :turn-count inc))

(s/defn set-phase :- game/GameState
  "Set the game phase"
  [game-state :- game/GameState
   phase :- game/GamePhase]
  (assoc game-state :phase phase))

(s/defn set-active-house :- game/GameState
  "Set the active house for the current turn"
  [game-state :- game/GameState
   house :- s/Keyword]
  (assoc game-state :active-house house))

;; ============================================================================
;; Card Drawing Utilities (Pure Functions)
;; ============================================================================

(s/defn draw-card :- game/Player
  "Draw a single card from deck to hand. Returns player unchanged if deck is empty."
  [player :- game/Player]
  (let [deck (:deck player)
        hand (:hand player)]
    (if (seq deck)
      (-> player
          (assoc :hand (conj hand (first deck)))
          (assoc :deck (vec (rest deck))))
      player)))

(s/defn draw-cards :- game/Player
  "Draw multiple cards from deck to hand. Stops if deck is exhausted."
  [player :- game/Player
   num-cards :- s/Int]
  (if (<= num-cards 0)
    player
    (let [deck (:deck player)
          hand (:hand player)
          available-cards (min num-cards (count deck))
          new-cards (take available-cards deck)
          remaining-deck (drop available-cards deck)]
      (-> player
          (assoc :hand (vec (concat hand new-cards)))
          (assoc :deck (vec remaining-deck))))))

(s/defn shuffle-deck-with-hand :- game/Player
  "Shuffle hand back into deck and draw specified number of cards (used for mulligan)"
  [player :- game/Player
   cards-to-draw :- s/Int]
  (let [hand (:hand player)
        deck (:deck player)
        new-deck (shuffle (concat deck hand))
        new-hand (vec (take cards-to-draw new-deck))
        remaining-deck (vec (drop cards-to-draw new-deck))]
    (-> player
        (assoc :hand new-hand)
        (assoc :deck remaining-deck))))