(ns battleforge-ai.models.game
  (:require [schema.core :as s]
            [battleforge-ai.models.deck :as deck]))

;; ============================================================================
;; Game State Models
;; ============================================================================

(s/defschema Player
  "Player state during a game"
  {:id            s/Str
   :deck          [deck/Card]    ; Changed from deck/Deck to list of cards
   :hand          [deck/Card]
   :discard       [deck/Card]
   :purged        [deck/Card]
   :archive       [deck/Card]
   :battleline    [deck/Card]
   :artifacts     [deck/Card]
   :houses        [deck/House]   ; Added houses from the deck
   :amber         s/Int
   :keys          s/Int
   :chains        s/Int
   :ready-amber   s/Int})

(s/defschema GamePhase
  "Current phase of the game"
  (s/enum :setup :forge :choose :play :ready :draw :end))

(s/defschema GameState
  "Complete state of an ongoing game"
  {:id              s/Str
   :player1         Player
   :player2         Player
   :current-player  (s/enum :player1 :player2)
   :turn-count      s/Int
   :phase           GamePhase
   :active-house    (s/maybe deck/House)
   :first-turn?     s/Bool
   :game-log        [s/Str]
   :started-at      s/Inst
   (s/optional-key :ended-at) s/Inst})

;; ============================================================================
;; Game Result Models
;; ============================================================================

(s/defschema GameResult
  "Final result of a completed game"
  {:id              s/Str
   :player1-deck    s/Str  ; deck ID
   :player2-deck    s/Str  ; deck ID
   :winner          (s/maybe (s/enum :player1 :player2))
   :loser           (s/maybe (s/enum :player1 :player2))
   :victory-condition (s/enum :keys :timeout :forfeit :error)
   :turn-count      s/Int
   :duration-minutes s/Num
   :player1-keys    s/Int
   :player2-keys    s/Int
   :player1-amber   s/Int
   :player2-amber   s/Int
   :started-at      s/Inst
   :ended-at        s/Inst})

(s/defschema GameSummary
  "Condensed game information for analysis"
  {:player1-deck    s/Str
   :player2-deck    s/Str
   :winner          (s/maybe (s/enum :player1 :player2))
   :turn-count      s/Int
   :duration-minutes s/Num})

;; ============================================================================
;; Game Action Models
;; ============================================================================

(s/defschema GameAction
  "Represents a single action taken during a game"
  {:id          s/Str
   :player      (s/enum :player1 :player2)
   :turn        s/Int
   :action-type (s/enum :play-card :use-card :fight :reap :forge-key :choose-house :end-turn)
   :card-id     (s/maybe s/Str)
   :target-id   (s/maybe s/Str)
   :house       (s/maybe deck/House)
   :amber-change s/Int
   :timestamp   s/Inst})

;; ============================================================================
;; Utility Functions (Pure - No Implementation Yet)
;; ============================================================================

(defn create-initial-game-state
  "Create initial game state from two decks"
  [deck1 deck2]
  ;; TODO: Implement initial game state creation
  ;; - Shuffle decks
  ;; - Draw initial hands
  ;; - Set starting player
  ;; - Initialize all counters
  nil)

(defn game-over?
  "Check if game has ended"
  [game-state]
  ;; TODO: Implement game end condition checking
  ;; - 3 keys forged
  ;; - Timeout reached
  ;; - Player forfeited
  false)

(defn calculate-duration
  "Calculate game duration in minutes"
  [game-result]
  ;; TODO: Implement duration calculation
  0)

(defn winner
  "Determine winner from game state"
  [game-state]
  ;; TODO: Implement winner determination logic
  nil) 