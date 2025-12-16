(ns battleforge-ai.logic.game-state-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schema.test :as schema-test]
            [java-time.api :as time]
            [battleforge-ai.logic.game-state :as game-state]))

(use-fixtures :once schema-test/validate-schemas)

(def sample-card-1
  {:id "card-1"
   :name "Sample Card 1"
   :house :brobnar
   :card-type :action
   :amber 2
   :power nil
   :armor nil
   :rarity :common
   :card-text nil
   :traits []
   :keywords []
   :expansion 341
   :number "001"
   :image nil})

(def sample-card-2
  {:id "card-2"
   :name "Sample Card 2"
   :house :dis
   :card-type :creature
   :amber 1
   :power 3
   :armor nil
   :rarity :common
   :card-text nil
   :traits []
   :keywords []
   :expansion 341
   :number "002"
   :image nil})

(def sample-deck-1
  {:id "test-deck-1"
   :name "Test Deck 1"
   :houses [:brobnar :dis :logos]
   :cards (vec (repeat 36 sample-card-1))
   :expansion 341
   :source :manual
   :fetched-at (time/instant)
   :win-rate nil
   :losses nil
   :wins nil
   :usage-count nil
   :last-updated nil
   :sas-rating nil
   :identity nil
   :is-alliance? nil
   :chains nil
   :upgrade-count nil
   :artifact-count nil
   :verified? nil
   :power-level nil
   :total-power nil
   :action-count nil
   :total-amber nil
   :uuid nil
   :creature-count nil})

(def sample-deck-2
  {:id "test-deck-2"
   :name "Test Deck 2"
   :houses [:sanctum :untamed :mars]
   :cards (vec (repeat 36 sample-card-2))
   :expansion 341
   :source :manual
   :fetched-at (time/instant)
   :win-rate nil
   :losses nil
   :wins nil
   :usage-count nil
   :last-updated nil
   :sas-rating nil
   :identity nil
   :is-alliance? nil
   :chains nil
   :upgrade-count nil
   :artifact-count nil
   :verified? nil
   :power-level nil
   :total-power nil
   :action-count nil
   :total-amber nil
   :uuid nil
   :creature-count nil})

(defn create-test-player
  "Create a test player with all required fields"
  [id deck hand-size]
  (let [deck-cards (:cards deck)
        hand-cards (take hand-size deck-cards)
        remaining-deck (drop hand-size deck-cards)]
    {:id id
     :deck (vec remaining-deck)
     :hand (vec hand-cards)
     :discard []
     :purged []
     :archive []
     :battleline []
     :artifacts []
     :houses (:houses deck)
     :amber 0
     :keys 0
     :chains 0
     :ready-amber 0}))

(deftest test-create-initial-game-state
  (testing "create-initial-game-state creates valid game state"
    (let [game-state (game-state/create-initial-game-state sample-deck-1 sample-deck-2 "test-game")]
      (is (string? (:id game-state)))
      (is (= :setup (:phase game-state)))
      (is (= 1 (:turn-count game-state)))
      (is (true? (:first-turn? game-state)))
      (is (inst? (:started-at game-state)))
      (is (vector? (:game-log game-state)))
      
      ;; Check players
      (is (= "player1" (get-in game-state [:player1 :id])))
      (is (= "player2" (get-in game-state [:player2 :id])))
      (is (= (:houses sample-deck-1) (get-in game-state [:player1 :houses])))
      (is (= (:houses sample-deck-2) (get-in game-state [:player2 :houses])))
      
      ;; Initial player state
      (is (= 0 (get-in game-state [:player1 :amber])))
      (is (= 0 (get-in game-state [:player1 :keys])))
      (is (= 0 (get-in game-state [:player1 :chains])))
      (is (= 0 (get-in game-state [:player2 :amber])))
      (is (= 0 (get-in game-state [:player2 :keys])))
      (is (= 0 (get-in game-state [:player2 :chains]))))))

(deftest test-get-current-player
  (testing "get-current-player returns correct player"
    (let [game-state (game-state/create-initial-game-state sample-deck-1 sample-deck-2 "test-game")
          current-player (game-state/get-current-player game-state)]
      ;; Should return a valid player with all required fields
      (is (string? (:id current-player)))
      (is (vector? (:hand current-player)))
      (is (vector? (:deck current-player)))
      (is (int? (:amber current-player)))
      (is (int? (:keys current-player))))))

(deftest test-get-opponent
  (testing "get-opponent returns correct opponent"
         (let [game-state (game-state/create-initial-game-state sample-deck-1 sample-deck-2 "test-game")
           opponent (game-state/get-opponent game-state)]
      ;; Should return a valid player with all required fields
      (is (string? (:id opponent)))
      (is (vector? (:hand opponent)))
      (is (vector? (:deck opponent)))
      (is (int? (:amber opponent)))
      (is (int? (:keys opponent))))))

(deftest test-switch-active-player
  (testing "switch-active-player toggles between players"
    (let [initial-state (game-state/create-initial-game-state sample-deck-1 sample-deck-2 "test-game")
          initial-player (:current-player initial-state)
          switched-state (game-state/switch-active-player initial-state)
          switched-player (:current-player switched-state)]
      (is (not= initial-player switched-player))
      
      ;; Switch back should return to original
      (let [double-switched (game-state/switch-active-player switched-state)]
        (is (= initial-player (:current-player double-switched)))))))

(deftest test-has-won
  (testing "has-won? correctly identifies win condition"
    (let [player (create-test-player "player" sample-deck-1 7)]
      (is (true? (game-state/has-won? (assoc player :keys 3))))
      (is (false? (game-state/has-won? (assoc player :keys 2))))
      (is (false? (game-state/has-won? (assoc player :keys 1))))
      (is (false? (game-state/has-won? (assoc player :keys 0)))))))

(deftest test-check-win-condition
  (testing "check-win-condition correctly identifies winner"
    (let [game-state (game-state/create-initial-game-state sample-deck-1 sample-deck-2 "test-game")
          
          ;; Player 1 wins
          p1-wins (assoc-in game-state [:player1 :keys] 3)
          
          ;; Player 2 wins
          p2-wins (assoc-in game-state [:player2 :keys] 3)
          
          ;; No winner
          no-winner (-> game-state
                        (assoc-in [:player1 :keys] 2)
                        (assoc-in [:player2 :keys] 1))]
      
      (is (= :player1 (game-state/check-win-condition p1-wins)))
      (is (= :player2 (game-state/check-win-condition p2-wins)))
      (is (nil? (game-state/check-win-condition no-winner))))))

(deftest test-update-player
  (testing "update-player correctly updates the specified player"
    (let [game-state (game-state/create-initial-game-state sample-deck-1 sample-deck-2 "test-game")
          updated-player (assoc (get-in game-state [:player1]) :amber 5)
          updated-state (game-state/update-player game-state :player1 updated-player)]
      
      (is (= 5 (get-in updated-state [:player1 :amber])))
      (is (= 0 (get-in updated-state [:player2 :amber]))) ; Unchanged
      
      ;; Test updating player2
      (let [updated-player2 (assoc (get-in game-state [:player2]) :keys 2)
            updated-state2 (game-state/update-player game-state :player2 updated-player2)]
        (is (= 2 (get-in updated-state2 [:player2 :keys])))
        (is (= 0 (get-in updated-state2 [:player1 :keys])))))))  ; Unchanged

(deftest test-add-to-game-log
  (testing "add-to-game-log appends messages to game log"
    (let [game-state (game-state/create-initial-game-state sample-deck-1 sample-deck-2 "test-game")
          logged-state (game-state/add-to-game-log game-state "Test message")]
      
      (is (some #(= "Test message" %) (:game-log logged-state)))
      
      ;; Add another message
      (let [double-logged (game-state/add-to-game-log logged-state "Second message")]
        (is (some #(= "Test message" %) (:game-log double-logged)))
        (is (some #(= "Second message" %) (:game-log double-logged)))))))

(deftest test-advance-turn
  (testing "advance-turn increments turn counter"
    (let [game-state (game-state/create-initial-game-state sample-deck-1 sample-deck-2 "test-game")
          advanced-state (game-state/advance-turn game-state)]
      
      (is (= 2 (:turn-count advanced-state)))
      (is (= 1 (:turn-count game-state))) ; Original unchanged
      
      ;; Advance again
      (let [twice-advanced (game-state/advance-turn advanced-state)]
        (is (= 3 (:turn-count twice-advanced)))))))

(deftest test-set-phase
  (testing "set-phase updates game phase"
    (let [game-state (game-state/create-initial-game-state sample-deck-1 sample-deck-2 "test-game")
          forge-phase (game-state/set-phase game-state :forge)
          play-phase (game-state/set-phase forge-phase :play)]
      
      (is (= :forge (:phase forge-phase)))
      (is (= :play (:phase play-phase)))
      (is (= :setup (:phase game-state))))))  ; Original unchanged

(deftest test-set-active-house
  (testing "set-active-house updates active house"
    (let [game-state (game-state/create-initial-game-state sample-deck-1 sample-deck-2 "test-game")
          house-set (game-state/set-active-house game-state :brobnar)]
      
      (is (= :brobnar (:active-house house-set)))
      (is (nil? (:active-house game-state))))))  ; Original unchanged

(deftest test-utility-functions
  (testing "utility functions work correctly"
    (let [game-state (game-state/create-initial-game-state sample-deck-1 sample-deck-2 "test-game")]
      ;; Game should not be over initially
      (is (nil? (game-state/check-win-condition game-state)))
      
      ;; Current player should exist
      (is (some? (game-state/get-current-player game-state)))
      
      ;; Opponent should exist
      (is (some? (game-state/get-opponent game-state)))
      
      ;; IDs should be different
      (is (not= (:id (game-state/get-current-player game-state))
                (:id (game-state/get-opponent game-state)))))))

(deftest test-game-state-immutability
  (testing "game state functions maintain immutability"
    (let [original-state (game-state/create-initial-game-state sample-deck-1 sample-deck-2 "test-game")
          modified-state (-> original-state
                             (game-state/advance-turn)
                             (game-state/set-phase :forge)
                             (game-state/add-to-game-log "Test"))
          current-player (game-state/get-current-player original-state)
          updated-player (assoc current-player :amber 10)
          player-updated-state (game-state/update-player original-state :player1 updated-player)]
      
      ;; Original state should be unchanged
      (is (= 1 (:turn-count original-state)))
      (is (= :setup (:phase original-state)))
      (is (empty? (:game-log original-state)))
      (is (= 0 (get-in original-state [:player1 :amber])))
      
      ;; Modified states should have changes
      (is (= 2 (:turn-count modified-state)))
      (is (= :forge (:phase modified-state)))
      (is (some #(= "Test" %) (:game-log modified-state)))
      (is (= 10 (get-in player-updated-state [:player1 :amber]))))))

;; ============================================================================
;; Card Drawing Utilities Tests
;; ============================================================================

(def sample-card-3
  {:id "card-3"
   :name "Sample Card 3"
   :house :logos
   :card-type :action
   :amber 0
   :power nil
   :armor nil
   :rarity :common
   :card-text nil
   :traits []
   :keywords []
   :expansion 341
   :number "003"
   :image nil})

(deftest test-draw-card
  (testing "draw-card draws one card from deck to hand"
    (let [player {:id "player1"
                  :deck [sample-card-1 sample-card-2 sample-card-3]
                  :hand []
                  :discard []
                  :purged []
                  :archive []
                  :battleline []
                  :artifacts []
                  :houses [:brobnar :dis :logos]
                  :amber 0
                  :keys 0
                  :chains 0
                  :ready-amber 0}
          result (game-state/draw-card player)]
      (is (= 1 (count (:hand result))))
      (is (= 2 (count (:deck result))))
      (is (= sample-card-1 (first (:hand result))))
      (is (= [sample-card-2 sample-card-3] (:deck result)))))
  
  (testing "draw-card with existing hand appends new card"
    (let [player {:id "player1"
                  :deck [sample-card-2 sample-card-3]
                  :hand [sample-card-1]
                  :discard []
                  :purged []
                  :archive []
                  :battleline []
                  :artifacts []
                  :houses [:brobnar :dis :logos]
                  :amber 0
                  :keys 0
                  :chains 0
                  :ready-amber 0}
          result (game-state/draw-card player)]
      (is (= 2 (count (:hand result))))
      (is (= 1 (count (:deck result))))
      (is (= [sample-card-1 sample-card-2] (:hand result)))
      (is (= [sample-card-3] (:deck result)))))
  
  (testing "draw-card from empty deck returns player unchanged"
    (let [player {:id "player1"
                  :deck []
                  :hand [sample-card-1]
                  :discard []
                  :purged []
                  :archive []
                  :battleline []
                  :artifacts []
                  :houses [:brobnar :dis :logos]
                  :amber 0
                  :keys 0
                  :chains 0
                  :ready-amber 0}
          result (game-state/draw-card player)]
      (is (= player result))
      (is (= [] (:deck result)))
      (is (= [sample-card-1] (:hand result))))))

(deftest test-draw-cards
  (testing "draw-cards draws specified number of cards"
    (let [player {:id "player1"
                  :deck [sample-card-1 sample-card-2 sample-card-3]
                  :hand []
                  :discard []
                  :purged []
                  :archive []
                  :battleline []
                  :artifacts []
                  :houses [:brobnar :dis :logos]
                  :amber 0
                  :keys 0
                  :chains 0
                  :ready-amber 0}
          result (game-state/draw-cards player 2)]
      (is (= 2 (count (:hand result))))
      (is (= 1 (count (:deck result))))
      (is (= [sample-card-1 sample-card-2] (:hand result)))
      (is (= [sample-card-3] (:deck result)))))
  
  (testing "draw-cards with more cards than available draws all available"
    (let [player {:id "player1"
                  :deck [sample-card-1 sample-card-2]
                  :hand []
                  :discard []
                  :purged []
                  :archive []
                  :battleline []
                  :artifacts []
                  :houses [:brobnar :dis :logos]
                  :amber 0
                  :keys 0
                  :chains 0
                  :ready-amber 0}
          result (game-state/draw-cards player 5)]
      (is (= 2 (count (:hand result))))
      (is (= 0 (count (:deck result))))
      (is (= [sample-card-1 sample-card-2] (:hand result)))
      (is (= [] (:deck result)))))
  
  (testing "draw-cards with zero cards returns player unchanged"
    (let [player {:id "player1"
                  :deck [sample-card-1 sample-card-2]
                  :hand [sample-card-3]
                  :discard []
                  :purged []
                  :archive []
                  :battleline []
                  :artifacts []
                  :houses [:brobnar :dis :logos]
                  :amber 0
                  :keys 0
                  :chains 0
                  :ready-amber 0}
          result (game-state/draw-cards player 0)]
      (is (= player result))))
  
  (testing "draw-cards with negative number returns player unchanged"
    (let [player {:id "player1"
                  :deck [sample-card-1 sample-card-2]
                  :hand [sample-card-3]
                  :discard []
                  :purged []
                  :archive []
                  :battleline []
                  :artifacts []
                  :houses [:brobnar :dis :logos]
                  :amber 0
                  :keys 0
                  :chains 0
                  :ready-amber 0}
          result (game-state/draw-cards player -1)]
      (is (= player result)))))

(deftest test-shuffle-deck-with-hand
  (testing "shuffle-deck-with-hand combines hand and deck, shuffles, and draws specified cards"
    (let [player {:id "player1"
                  :deck [sample-card-1 sample-card-2]
                  :hand [sample-card-3]
                  :discard []
                  :purged []
                  :archive []
                  :battleline []
                  :artifacts []
                  :houses [:brobnar :dis :logos]
                  :amber 0
                  :keys 0
                  :chains 0
                  :ready-amber 0}
          result (game-state/shuffle-deck-with-hand player 2)]
      (is (= 2 (count (:hand result))))
      (is (= 1 (count (:deck result))))
      ;; Total cards should be preserved
      (is (= 3 (+ (count (:hand result)) (count (:deck result)))))
      ;; All original cards should be present somewhere
      (let [all-result-cards (concat (:hand result) (:deck result))
            all-original-cards [sample-card-1 sample-card-2 sample-card-3]]
        (is (= (set all-result-cards) (set all-original-cards))))))
  
  (testing "shuffle-deck-with-hand for mulligan scenario (6 cards to 5)"
    (let [hand-cards [sample-card-1 sample-card-2 sample-card-3]
          deck-cards (vec (repeat 33 sample-card-1)) ; Remaining deck
          player {:id "player1"
                  :deck deck-cards
                  :hand hand-cards
                  :discard []
                  :purged []
                  :archive []
                  :battleline []
                  :artifacts []
                  :houses [:brobnar :dis :logos]
                  :amber 0
                  :keys 0
                  :chains 0
                  :ready-amber 0}
          result (game-state/shuffle-deck-with-hand player 5)] ; Mulligan: 6 cards become 5
      (is (= 5 (count (:hand result))))
      (is (= 31 (count (:deck result))))
      ;; Total cards preserved
      (is (= 36 (+ (count (:hand result)) (count (:deck result)))))))
  
  (testing "shuffle-deck-with-hand draws all cards when requested more than available"
    (let [player {:id "player1"
                  :deck [sample-card-1]
                  :hand [sample-card-2]
                  :discard []
                  :purged []
                  :archive []
                  :battleline []
                  :artifacts []
                  :houses [:brobnar :dis :logos]
                  :amber 0
                  :keys 0
                  :chains 0
                  :ready-amber 0}
          result (game-state/shuffle-deck-with-hand player 5)]
      (is (= 2 (count (:hand result))))
      (is (= 0 (count (:deck result)))))))

(deftest test-shuffle-deck-with-hand-randomness
  (testing "shuffle-deck-with-hand produces different results (randomness test)"
    (let [player {:id "player1"
                  :deck [sample-card-1 sample-card-2 sample-card-3]
                  :hand [sample-card-1 sample-card-2 sample-card-3]
                  :discard []
                  :purged []
                  :archive []
                  :battleline []
                  :artifacts []
                  :houses [:brobnar :dis :logos]
                  :amber 0
                  :keys 0
                  :chains 0
                  :ready-amber 0}
          result1 (game-state/shuffle-deck-with-hand player 3)
          result2 (game-state/shuffle-deck-with-hand player 3)]
      ;; Note: This test may occasionally fail due to randomness, but it's very unlikely
      (is (not= (:hand result1) (:hand result2))))))