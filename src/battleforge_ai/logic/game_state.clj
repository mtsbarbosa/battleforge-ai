(ns battleforge-ai.logic.game-state
  (:require [schema.core :as s]
            [battleforge-ai.models.game :as game]
            [battleforge-ai.models.deck :as deck]
            [java-time.api :as time]))

(s/defn create-initial-player
  :-
  game/Player
  [player-id :- s/Str deck :- deck/Deck]
  (let [shuffled-deck (shuffle (:cards deck))]
    {:id player-id,
     :deck (vec (drop 7 shuffled-deck)),
     :hand (vec (take 7 shuffled-deck)),
     :discard [],
     :purged [],
     :archive [],
     :battleline [],
     :artifacts [],
     :houses (:houses deck),
     :amber 0,
     :keys 0,
     :chains (or (:chains deck) 0),
     :ready-amber 0}))

(s/defn create-initial-game-state
  :-
  game/GameState
  [deck1 :- deck/Deck deck2 :- deck/Deck game-id :- s/Str]
  (let [player1 (create-initial-player "player1" deck1)
        player2 (create-initial-player "player2" deck2)
        first-player (if (< (rand) 0.5) :player1 :player2)]
    {:id game-id,
     :player1 player1,
     :player2 player2,
     :current-player first-player,
     :turn-count 1,
     :phase :setup,
     :active-house nil,
     :first-turn? true,
     :game-log [],
     :started-at (java.util.Date/from (time/instant))}))

(s/defn get-current-player
  :-
  game/Player
  [game-state :- game/GameState]
  (if (= (:current-player game-state) :player1)
    (:player1 game-state)
    (:player2 game-state)))

(s/defn get-opponent
  :-
  game/Player
  [game-state :- game/GameState]
  (if (= (:current-player game-state) :player1)
    (:player2 game-state)
    (:player1 game-state)))

(s/defn update-current-player
  :-
  game/GameState
  [game-state :- game/GameState updated-player :- game/Player]
  (if (= (:current-player game-state) :player1)
    (assoc game-state :player1 updated-player)
    (assoc game-state :player2 updated-player)))

(s/defn update-opponent
  :-
  game/GameState
  [game-state :- game/GameState updated-opponent :- game/Player]
  (if (= (:current-player game-state) :player1)
    (assoc game-state :player2 updated-opponent)
    (assoc game-state :player1 updated-opponent)))

(s/defn update-player
  :-
  game/GameState
  [game-state :- game/GameState player-key :- (s/enum :player1 :player2)
   updated-player :- game/Player]
  (assoc game-state player-key updated-player))

(s/defn switch-active-player
  :-
  game/GameState
  [game-state :- game/GameState]
  (let [new-player
          (if (= (:current-player game-state) :player1) :player2 :player1)
        new-turn-count (if (= new-player :player1)
                         (inc (:turn-count game-state))
                         (:turn-count game-state))]
    (-> game-state
        (assoc :current-player new-player)
        (assoc :turn-count new-turn-count)
        (assoc :first-turn? false))))

(s/defn can-forge-key?
  :-
  s/Bool
  [player :- game/Player]
  (and (>= (:amber player) 6) (< (:keys player) 3)))

(s/defn has-won? :- s/Bool [player :- game/Player] (>= (:keys player) 3))

(s/defn game-over?
  :-
  s/Bool
  [game-state :- game/GameState]
  (or (>= (:keys (:player1 game-state)) 3)
      (>= (:keys (:player2 game-state)) 3)))

(s/defn get-winner
  :-
  (s/maybe (s/enum :player1 :player2))
  [game-state :- game/GameState]
  (cond (>= (:keys (:player1 game-state)) 3) :player1
        (>= (:keys (:player2 game-state)) 3) :player2
        :else nil))

(s/defn check-win-condition
  :-
  (s/maybe (s/enum :player1 :player2))
  [game-state :- game/GameState]
  (get-winner game-state))

(s/defn add-to-game-log
  :-
  game/GameState
  [game-state :- game/GameState message :- s/Str]
  (update game-state :game-log conj message))

(s/defn advance-phase
  :-
  game/GameState
  [game-state :- game/GameState next-phase :- game/GamePhase]
  (assoc game-state :phase next-phase))

(s/defn advance-turn
  :-
  game/GameState
  [game-state :- game/GameState]
  (update game-state :turn-count inc))

(s/defn set-phase
  :-
  game/GameState
  [game-state :- game/GameState phase :- game/GamePhase]
  (assoc game-state :phase phase))

(s/defn set-active-house
  :-
  game/GameState
  [game-state :- game/GameState house :- s/Keyword]
  (assoc game-state :active-house house))

(s/defn draw-card
  :-
  game/Player
  [player :- game/Player]
  (let [deck (:deck player)
        hand (:hand player)]
    (if (seq deck)
      (-> player
          (assoc :hand (conj hand (first deck)))
          (assoc :deck (vec (rest deck))))
      player)))

(s/defn draw-cards
  :-
  game/Player
  [player :- game/Player num-cards :- s/Int]
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

(s/defn shuffle-deck-with-hand
  :-
  game/Player
  "Mulligan: shuffle hand back into deck and draw new cards"
  [player :- game/Player cards-to-draw :- s/Int]
  (let [hand (:hand player)
        deck (:deck player)
        new-deck (shuffle (concat deck hand))
        new-hand (vec (take cards-to-draw new-deck))
        remaining-deck (vec (drop cards-to-draw new-deck))]
    (-> player
        (assoc :hand new-hand)
        (assoc :deck remaining-deck))))