(ns battleforge-ai.models.battle
  (:require [schema.core :as s]
            [battleforge-ai.models.game :as game]
            [battleforge-ai.models.deck :as deck]))

;; ============================================================================
;; Battle Series Models
;; ============================================================================

(s/defschema BattleConfig
  "Configuration for a battle series"
  {:deck1-id         s/Str
   :deck2-id         s/Str
   :num-games        s/Int
   :timeout-minutes  s/Int
   :parallel-games   s/Int
   :random-seed      (s/maybe s/Int)})

(s/defschema BattleResult
  "Results of a battle series between two decks"
  {:id               s/Str
   :deck1-id         s/Str
   :deck2-id         s/Str
   :config           BattleConfig
   :games            [game/GameResult]
   :total-games      s/Int
   :deck1-wins       s/Int
   :deck2-wins       s/Int
   :ties             s/Int
   :deck1-win-rate   s/Num
   :deck2-win-rate   s/Num
   :avg-game-length  s/Num
   :avg-turn-count   s/Num
   :started-at       s/Inst
   :completed-at     s/Inst
   :duration-minutes s/Num})

;; ============================================================================
;; Tournament Models
;; ============================================================================

(s/defschema MatchupResult
  "Result of a single matchup in a tournament"
  {:deck1-id         s/Str
   :deck2-id         s/Str
   :battle-result    BattleResult})

(s/defschema TournamentConfig
  "Configuration for a tournament simulation"
  {:deck-ids         [s/Str]
   :format           (s/enum :round-robin :single-elimination :double-elimination :swiss)
   :games-per-match  s/Int
   :timeout-minutes  s/Int
   :parallel-matches s/Int
   :random-seed      (s/maybe s/Int)})

(s/defschema TournamentResult
  "Complete tournament results"
  {:id               s/Str
   :config           TournamentConfig
   :matchups         [MatchupResult]
   :deck-standings   [{:deck-id s/Str
                       :wins s/Int
                       :losses s/Int
                       :win-rate s/Num
                       :avg-game-length s/Num}]
   :started-at       s/Inst
   :completed-at     s/Inst
   :duration-minutes s/Num})

;; ============================================================================
;; Statistics Models
;; ============================================================================

(s/defschema DeckPerformance
  "Performance statistics for a single deck"
  {:deck-id              s/Str
   :total-games          s/Int
   :wins                 s/Int
   :losses               s/Int
   :ties                 s/Int
   :win-rate             s/Num
   :avg-game-length      s/Num
   :avg-turn-count       s/Num
   :best-matchup-deck    (s/maybe s/Str)
   :worst-matchup-deck   (s/maybe s/Str)
   :house-win-rates      {deck/House s/Num}})

(s/defschema MatchupStatistics
  "Head-to-head statistics between two decks"
  {:deck1-id         s/Str
   :deck2-id         s/Str
   :total-games      s/Int
   :deck1-wins       s/Int
   :deck2-wins       s/Int
   :deck1-win-rate   s/Num
   :deck2-win-rate   s/Num
   :avg-game-length  s/Num
   :confidence-interval {:lower s/Num :upper s/Num}})

;; ============================================================================
;; Utility Functions (Pure - No Implementation Yet)
;; ============================================================================

(defn calculate-win-rate
  "Calculate win rate from wins and total games"
  [wins total-games]
  ;; TODO: Implement win rate calculation with proper handling of edge cases
  (if (zero? total-games)
    0.0
    (/ (double wins) total-games)))

(defn calculate-confidence-interval
  "Calculate confidence interval for win rate"
  [wins total-games confidence-level]
  ;; TODO: Implement confidence interval calculation using binomial distribution
  {:lower 0.0 :upper 1.0})

(defn aggregate-battle-results
  "Aggregate multiple battle results into summary statistics"
  [battle-results]
  ;; TODO: Implement aggregation logic
  ;; - Combine results from multiple battles
  ;; - Calculate overall statistics
  ;; - Handle different deck combinations
  nil)

(defn create-tournament-brackets
  "Create tournament bracket structure based on format"
  [deck-ids format]
  ;; TODO: Implement bracket creation for different tournament formats
  nil) 