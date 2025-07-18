(ns battleforge-ai.logic.game-flow-test
  (:require [clojure.test :refer :all]
            [schema.test :as schema-test]
            [battleforge-ai.logic.game-flow :as game-flow]
            [battleforge-ai.logic.game-state :as game-state]
            [java-time :as time]))

(use-fixtures :once schema-test/validate-schemas)

(deftest test-immediate-victory-on-third-key
  (testing "Game ends immediately when player forges their 3rd key"
    (let [;; Create a player with 2 keys and enough amber to forge the 3rd
          winning-player {:id "winner"
                          :deck []
                          :hand []
                          :discard []
                          :purged []
                          :archive []
                          :battleline []
                          :artifacts []
                          :houses [:brobnar :dis :logos]
                          :amber 6  ; Enough to forge a key
                          :keys 2   ; Already has 2 keys
                          :chains 0
                          :ready-amber 0}
          other-player {:id "opponent"
                        :deck []
                        :hand []
                        :discard []
                        :purged []
                        :archive []
                        :battleline []
                        :artifacts []
                        :houses [:mars :sanctum :untamed]
                        :amber 0
                        :keys 0
                        :chains 0
                        :ready-amber 0}
          initial-state {:id "test-game"
                         :player1 winning-player
                         :player2 other-player
                         :current-player :player1
                         :turn-count 1
                         :phase :forge
                         :active-house nil
                         :first-turn? false
                         :game-log []
                         :started-at (java.util.Date.)}
          
          ;; Execute forge phase
          result (game-flow/execute-forge-phase initial-state)]
      
      ;; Game should be over
      (is (= :end (:phase result)))
      (is (some? (:ended-at result)))
      
      ;; Winner should have 3 keys
      (is (= 3 (:keys (:player1 result))))
      (is (= :player1 (game-state/get-winner result)))
      
      ;; Game log should contain victory message
      (is (some #(re-find #"wins the game with 3 keys" %) (:game-log result))))))

(deftest test-forge-phase-continues-when-no-victory
  (testing "Game continues normally when player forges key but doesn't win"
    (let [;; Create a player with 1 key who will forge their 2nd key
          player {:id "player"
                  :deck []
                  :hand []
                  :discard []
                  :purged []
                  :archive []
                  :battleline []
                  :artifacts []
                  :houses [:brobnar :dis :logos]
                  :amber 6  ; Enough to forge a key
                  :keys 1   ; Has 1 key
                  :chains 0
                  :ready-amber 0}
          other-player {:id "opponent"
                        :deck []
                        :hand []
                        :discard []
                        :purged []
                        :archive []
                        :battleline []
                        :artifacts []
                        :houses [:mars :sanctum :untamed]
                        :amber 0
                        :keys 0
                        :chains 0
                        :ready-amber 0}
          initial-state {:id "test-game"
                         :player1 player
                         :player2 other-player
                         :current-player :player1
                         :turn-count 1
                         :phase :forge
                         :active-house nil
                         :first-turn? false
                         :game-log []
                         :started-at (java.util.Date.)}
          
          ;; Execute forge phase
          result (game-flow/execute-forge-phase initial-state)]
      
      ;; Game should continue to choose phase
      (is (= :choose (:phase result)))
      (is (nil? (:ended-at result)))
      
      ;; Player should have 2 keys now
      (is (= 2 (:keys (:player1 result))))
      (is (nil? (game-state/get-winner result)))
      
      ;; Game should not be over
      (is (false? (game-state/game-over? result))))))