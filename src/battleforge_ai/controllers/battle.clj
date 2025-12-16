(ns battleforge-ai.controllers.battle
  (:require [clojure.tools.logging :as log]
            [schema.core :as s]
            [battleforge-ai.models.deck :as deck]
            [battleforge-ai.models.game :as game]
            [battleforge-ai.models.battle :as battle]
            [battleforge-ai.logic.game-state :as game-state]
            [battleforge-ai.logic.game-flow :as game-flow]
            [java-time.api :as time]))

(s/defn create-game-id :- s/Str
  []
  (str "game-" (System/currentTimeMillis) "-" (rand-int 10000)))

(s/defn validate-battle-params!
  [{:keys [deck1 deck2 num-games]}]
  (cond
    (nil? deck1)
    (throw (ex-info "Deck1 is required" {:type :validation-error}))
    
    (nil? deck2)
    (throw (ex-info "Deck2 is required" {:type :validation-error}))
    
    (not (s/validate deck/Deck deck1))
    (throw (ex-info "Deck1 is invalid" {:type :validation-error}))
    
    (not (s/validate deck/Deck deck2))
    (throw (ex-info "Deck2 is invalid" {:type :validation-error}))
    
    (or (nil? num-games) (<= num-games 0))
    (throw (ex-info "Number of games must be positive" {:type :validation-error}))
    
    :else
    true))

(s/defn simulate-single-game :- game/GameResult
  [deck1 :- deck/Deck
   deck2 :- deck/Deck
   game-id :- s/Str
   battle-mode]
  (log/info "Starting game simulation:" game-id)
  
  (try
    (let [initial-state (-> (game-state/create-initial-game-state deck1 deck2 game-id)
                           (assoc :battle-mode battle-mode))
          final-state (game-flow/simulate-game initial-state)
          winner (game-state/get-winner final-state)
          duration-ms (- (.getTime (:ended-at final-state))
                        (.getTime (:started-at final-state)))
          duration-minutes (/ duration-ms 60000.0)]
      
      (log/info "Game completed:" game-id "Winner:" winner "Turns:" (:turn-count final-state))
      
      {:id              game-id
       :player1-deck    (:id deck1)
       :player2-deck    (:id deck2)
       :winner          winner
       :loser           (case winner
                          :player1 :player2
                          :player2 :player1
                          nil)
       :victory-condition :keys
       :turn-count      (:turn-count final-state)
       :duration-minutes duration-minutes
       :player1-keys    (:keys (:player1 final-state))
       :player2-keys    (:keys (:player2 final-state))
       :player1-amber   (:amber (:player1 final-state))
       :player2-amber   (:amber (:player2 final-state))
       :started-at      (:started-at final-state)
       :ended-at        (:ended-at final-state)})
    
    (catch Exception e
      (log/error e "Error simulating game:" game-id)
      (throw e))))

(s/defn simulate-battle-series! :- battle/BattleResult
  [{:keys [deck1 deck2 num-games battle-mode] :or {battle-mode :simple} :as params}]
  (log/info "Starting battle series simulation" 
            "Deck1:" (:name deck1) 
            "Deck2:" (:name deck2)
            "Games:" num-games)
  
  (try
    (validate-battle-params! params)
    
    (let [battle-id (str "battle-" (System/currentTimeMillis))
          started-at (java.util.Date/from (time/instant))
          games (doall
                  (for [i (range num-games)]
                    (let [game-id (str battle-id "-game-" (inc i))]
                      (simulate-single-game deck1 deck2 game-id battle-mode))))
          completed-at (java.util.Date/from (time/instant))
          duration-ms (- (.getTime completed-at) (.getTime started-at))
          duration-minutes (/ duration-ms 60000.0)
          deck1-wins (count (filter #(= (:winner %) :player1) games))
          deck2-wins (count (filter #(= (:winner %) :player2) games))
          ties (count (filter #(nil? (:winner %)) games))
          
          deck1-win-rate (if (> num-games 0) (/ (double deck1-wins) num-games) 0.0)
          deck2-win-rate (if (> num-games 0) (/ (double deck2-wins) num-games) 0.0)
          
          avg-game-length (if (seq games)
                           (double (/ (reduce + (map :duration-minutes games)) (count games)))
                           0.0)
          avg-turn-count (if (seq games)
                          (double (/ (reduce + (map :turn-count games)) (count games)))
                          0.0)
          
          battle-config {:deck1-id (:id deck1)
                        :deck2-id (:id deck2)
                        :num-games num-games
                        :timeout-minutes 30
                        :parallel-games 1
                        :random-seed nil}]
      
      (log/info "Battle series completed"
                "Total games:" num-games
                "Deck1 wins:" deck1-wins
                "Deck2 wins:" deck2-wins
                "Ties:" ties)
      
      {:id               battle-id
       :deck1-id         (:id deck1)
       :deck2-id         (:id deck2)
       :config           battle-config
       :games            games
       :total-games      num-games
       :deck1-wins       deck1-wins
       :deck2-wins       deck2-wins
       :ties             ties
       :deck1-win-rate   deck1-win-rate
       :deck2-win-rate   deck2-win-rate
       :avg-game-length  avg-game-length
       :avg-turn-count   avg-turn-count
       :started-at       started-at
       :completed-at     completed-at
       :duration-minutes duration-minutes})
    
    (catch Exception e
      (log/error e "Error in battle series simulation")
      (throw e))))

(s/defn format-battle-summary :- s/Str
  [battle-result :- battle/BattleResult]
  (let [{:keys [deck1-id deck2-id total-games deck1-wins deck2-wins ties
                deck1-win-rate deck2-win-rate avg-game-length avg-turn-count]} battle-result]
    
    (format
      "=== Battle Results ===\n%s vs %s\n\nGames Played: %d\n%s Wins: %d (%.1f%%)\n%s Wins: %d (%.1f%%)\nTies: %d\n\nAverage Game Length: %.1f minutes\nAverage Turn Count: %.1f turns"
      deck1-id deck2-id total-games
      deck1-id deck1-wins (double (* deck1-win-rate 100))
      deck2-id deck2-wins (double (* deck2-win-rate 100))
      ties (double avg-game-length) (double avg-turn-count)))) 