(ns battleforge-ai.models.battle
  (:require [schema.core :as s]
            [battleforge-ai.models.game :as game]
            [battleforge-ai.models.deck :as deck]))

(s/defschema BattleConfig
             {:deck1-id s/Str,
              :deck2-id s/Str,
              :num-games s/Int,
              :timeout-minutes s/Int,
              :parallel-games s/Int,
              :random-seed (s/maybe s/Int)})

(s/defschema BattleResult
             {:id s/Str,
              :deck1-id s/Str,
              :deck2-id s/Str,
              :config BattleConfig,
              :games [game/GameResult],
              :total-games s/Int,
              :deck1-wins s/Int,
              :deck2-wins s/Int,
              :ties s/Int,
              :deck1-win-rate s/Num,
              :deck2-win-rate s/Num,
              :avg-game-length s/Num,
              :avg-turn-count s/Num,
              :started-at s/Inst,
              :completed-at s/Inst,
              :duration-minutes s/Num})

(s/defschema MatchupResult
             {:deck1-id s/Str, :deck2-id s/Str, :battle-result BattleResult})

(s/defschema TournamentConfig
             {:deck-ids [s/Str],
              :format (s/enum :round-robin :single-elimination
                              :double-elimination :swiss),
              :games-per-match s/Int,
              :timeout-minutes s/Int,
              :parallel-matches s/Int,
              :random-seed (s/maybe s/Int)})

(s/defschema TournamentResult
             {:id s/Str,
              :config TournamentConfig,
              :matchups [MatchupResult],
              :deck-standings [{:deck-id s/Str,
                                :wins s/Int,
                                :losses s/Int,
                                :win-rate s/Num,
                                :avg-game-length s/Num}],
              :started-at s/Inst,
              :completed-at s/Inst,
              :duration-minutes s/Num})

(s/defschema DeckPerformance
             {:deck-id s/Str,
              :total-games s/Int,
              :wins s/Int,
              :losses s/Int,
              :ties s/Int,
              :win-rate s/Num,
              :avg-game-length s/Num,
              :avg-turn-count s/Num,
              :best-matchup-deck (s/maybe s/Str),
              :worst-matchup-deck (s/maybe s/Str),
              :house-win-rates {deck/House s/Num}})

(s/defschema MatchupStatistics
             {:deck1-id s/Str,
              :deck2-id s/Str,
              :total-games s/Int,
              :deck1-wins s/Int,
              :deck2-wins s/Int,
              :deck1-win-rate s/Num,
              :deck2-win-rate s/Num,
              :avg-game-length s/Num,
              :confidence-interval {:lower s/Num, :upper s/Num}})

(defn calculate-win-rate
  [wins total-games]
  (if (zero? total-games) 0.0 (/ (double wins) total-games)))

(defn calculate-confidence-interval [_ _ _] {:lower 0.0, :upper 1.0})

(defn aggregate-battle-results [_] nil)

(defn create-tournament-brackets [_ _] nil)