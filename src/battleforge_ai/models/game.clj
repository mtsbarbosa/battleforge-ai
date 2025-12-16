(ns battleforge-ai.models.game
  (:require [schema.core :as s]
            [battleforge-ai.models.deck :as deck]))

(s/defschema Player
             {:id s/Str,
              :deck [deck/Card],
              :hand [deck/Card],
              :discard [deck/Card],
              :purged [deck/Card],
              :archive [deck/Card],
              :battleline [deck/Card],
              :artifacts [deck/Card],
              :houses [deck/House],
              :amber s/Int,
              :keys s/Int,
              :chains s/Int,
              :ready-amber s/Int})

(s/defschema GamePhase (s/enum :setup :forge :choose :play :ready :draw :end))

(s/defschema BattleMode (s/enum :simple :in-depth))

(s/defschema GameState
             {:id s/Str,
              :player1 Player,
              :player2 Player,
              :current-player (s/enum :player1 :player2),
              :turn-count s/Int,
              :phase GamePhase,
              :active-house (s/maybe deck/House),
              :first-turn? s/Bool,
              :game-log [s/Str],
              :started-at s/Inst,
              (s/optional-key :ended-at) s/Inst,
              (s/optional-key :battle-mode) BattleMode})

(s/defschema GameResult
             {:id s/Str,
              :player1-deck s/Str,
              :player2-deck s/Str,
              :winner (s/maybe (s/enum :player1 :player2)),
              :loser (s/maybe (s/enum :player1 :player2)),
              :victory-condition (s/enum :keys :timeout :forfeit :error),
              :turn-count s/Int,
              :duration-minutes s/Num,
              :player1-keys s/Int,
              :player2-keys s/Int,
              :player1-amber s/Int,
              :player2-amber s/Int,
              :started-at s/Inst,
              :ended-at s/Inst})

(s/defschema GameSummary
             {:player1-deck s/Str,
              :player2-deck s/Str,
              :winner (s/maybe (s/enum :player1 :player2)),
              :turn-count s/Int,
              :duration-minutes s/Num})

(s/defschema GameAction
             {:id s/Str,
              :player (s/enum :player1 :player2),
              :turn s/Int,
              :action-type (s/enum :play-card
                                   :use-card :fight
                                   :reap :forge-key
                                   :choose-house :end-turn),
              :card-id (s/maybe s/Str),
              :target-id (s/maybe s/Str),
              :house (s/maybe deck/House),
              :amber-change s/Int,
              :timestamp s/Inst})

(defn create-initial-game-state [_ _] nil)
(defn game-over? [_] false)
(defn calculate-duration [_] 0)
(defn winner [_] nil)