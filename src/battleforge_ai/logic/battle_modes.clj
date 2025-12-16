(ns battleforge-ai.logic.battle-modes
  (:require [schema.core :as s]))

(s/defschema BattleMode
  (s/enum :simple :in-depth))

(defprotocol BattleModeHandler
  (execute-play-phase [this game-state])
  (play-card [this player card])
  (get-mode-name [this])
  (get-mode-description [this]))

(def ^:private battle-mode-registry
  {:simple (delay ((requiring-resolve 'battleforge-ai.logic.simple-battle/simple-battle-handler)))
   :in-depth (delay ((requiring-resolve 'battleforge-ai.logic.in-depth-battle/in-depth-battle-handler)))})

(defn get-battle-mode-handler [mode]
  @(get battle-mode-registry mode))

(s/defn list-available-modes :- [BattleMode]
  []
  (vec (keys battle-mode-registry)))

(s/defn get-mode-info :- {:mode BattleMode :name s/Str :description s/Str}
  [mode :- BattleMode]
  (let [handler (get-battle-mode-handler mode)]
    {:mode mode
     :name (get-mode-name handler)
     :description (get-mode-description handler)}))

(s/defn get-all-modes-info :- [{:mode BattleMode :name s/Str :description s/Str}]
  []
  (map get-mode-info (list-available-modes)))

(s/defn get-default-battle-mode :- BattleMode
  []
  :simple)