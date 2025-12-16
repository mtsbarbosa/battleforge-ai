(ns battleforge-ai.logic.game-flow-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schema.test :as schema-test]
            [battleforge-ai.logic.game-flow :as game-flow]
            [battleforge-ai.logic.game-state :as game-state]
            [battleforge-ai.logic.simple-battle :as simple-battle]))

(use-fixtures :once schema-test/validate-schemas)

(defn create-test-card
  "Create a test card with specified properties"
  ([name house card-type] (create-test-card name house card-type 0 nil))
  ([name house card-type amber]
   (create-test-card name house card-type amber nil))
  ([name house card-type amber expected-amber]
   {:id (str "card-" name),
    :name name,
    :house house,
    :card-type card-type,
    :amber amber,
    :power (when (= card-type :creature) 3),
    :armor nil,
    :rarity :common,
    :card-text nil,
    :traits [],
    :keywords [],
    :expansion 1,
    :number "001",
    :image nil,
    :count 1,
    :enhanced? false,
    :maverick? false,
    :anomaly? false,
    :expected-amber expected-amber}))

(defn create-test-player
  "Create a test player with specified properties"
  ([id houses] (create-test-player id houses [] []))
  ([id houses hand battleline]
   {:id id,
    :deck [],
    :hand hand,
    :discard [],
    :purged [],
    :archive [],
    :battleline battleline,
    :artifacts [],
    :houses houses,
    :amber 0,
    :keys 0,
    :chains 0,
    :ready-amber 0}))

(deftest test-play-card-simple
  (testing "play-card-simple moves cards to correct locations"
    (let [creature-card (create-test-card "Test Creature" :brobnar :creature 1)
          action-card (create-test-card "Test Action" :brobnar :action 2)
          hand [creature-card action-card]
          player (create-test-player "test" [:brobnar :dis :logos] hand [])]
      ;; Playing a creature should move it to battleline
      (let [result (simple-battle/play-card-simple player creature-card)]
        (is (= [action-card] (:hand result))) ; creature removed from hand
        (is (= [creature-card] (:battleline result))) ; creature added to
                                                      ; battleline
        (is (empty? (:discard result))) ; nothing in discard
        (is (= 1 (:amber result)))) ; amber gained
      ;; Playing an action should move it to discard
      (let [result (simple-battle/play-card-simple player action-card)]
        (is (= [creature-card] (:hand result))) ; action removed from hand
        (is (empty? (:battleline result))) ; nothing added to battleline
        (is (= [action-card] (:discard result))) ; action added to discard
        (is (= 2 (:amber result))))))) ; amber gained

(deftest test-immediate-victory-on-third-key
  (testing "Game ends immediately when player forges their 3rd key"
    (let [;; Create a player with 2 keys and enough amber to forge the 3rd
          winning-player {:id "winner",
                          :deck [],
                          :hand [],
                          :discard [],
                          :purged [],
                          :archive [],
                          :battleline [],
                          :artifacts [],
                          :houses [:brobnar :dis :logos],
                          :amber 6, ; Enough to forge a key
                          :keys 2,  ; Already has 2 keys
                          :chains 0,
                          :ready-amber 0}
          other-player {:id "opponent",
                        :deck [],
                        :hand [],
                        :discard [],
                        :purged [],
                        :archive [],
                        :battleline [],
                        :artifacts [],
                        :houses [:mars :sanctum :untamed],
                        :amber 0,
                        :keys 0,
                        :chains 0,
                        :ready-amber 0}
          initial-state {:id "test-game",
                         :player1 winning-player,
                         :player2 other-player,
                         :current-player :player1,
                         :turn-count 1,
                         :phase :forge,
                         :active-house nil,
                         :first-turn? false,
                         :game-log [],
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
      (is (some #(re-find #"wins the game with 3 keys" %)
                (:game-log result))))))

(deftest test-forge-phase-continues-when-no-victory
  (testing "Game continues normally when player forges key but doesn't win"
    (let [;; Create a player with 1 key who will forge their 2nd key
          player {:id "player",
                  :deck [],
                  :hand [],
                  :discard [],
                  :purged [],
                  :archive [],
                  :battleline [],
                  :artifacts [],
                  :houses [:brobnar :dis :logos],
                  :amber 6, ; Enough to forge a key
                  :keys 1,  ; Has 1 key
                  :chains 0,
                  :ready-amber 0}
          other-player {:id "opponent",
                        :deck [],
                        :hand [],
                        :discard [],
                        :purged [],
                        :archive [],
                        :battleline [],
                        :artifacts [],
                        :houses [:mars :sanctum :untamed],
                        :amber 0,
                        :keys 0,
                        :chains 0,
                        :ready-amber 0}
          initial-state {:id "test-game",
                         :player1 player,
                         :player2 other-player,
                         :current-player :player1,
                         :turn-count 1,
                         :phase :forge,
                         :active-house nil,
                         :first-turn? false,
                         :game-log [],
                         :started-at (java.util.Date.)}
          ;; Execute forge phase
          result (game-flow/execute-forge-phase initial-state)]
      ;; Game should continue to choose phase
      (is (= :choose (:phase result)))
      (is (nil? (:ended-at result)))
      ;; Player should have 2 keys now
      (is (= 2 (:keys (:player1 result))))
      (is (nil? (game-state/get-winner result)))
      (is (false? (game-state/game-over? result))))))