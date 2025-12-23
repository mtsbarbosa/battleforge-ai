(ns battleforge-ai.logic.strategy-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schema.test :as schema-test]
            [battleforge-ai.logic.strategy :as strategy]))

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

(deftest test-get-creature-amber-potential
  (testing "get-creature-amber-potential returns correct values"
    (let [creature-with-expected
            (create-test-card "Strong Creature" :brobnar :creature 1 2.5)
          creature-without-expected
            (create-test-card "Basic Creature" :brobnar :creature 0 nil)
          action-card (create-test-card "Action Card" :brobnar :action 2 1.5)]
      ;; Should use expected-amber when available for creatures
      (is (= 2.5
             (strategy/get-creature-amber-potential creature-with-expected)))
      ;; Should fallback to 1.0 for creatures without expected-amber
      (is (= 1.0
             (strategy/get-creature-amber-potential creature-without-expected)))
      ;; Should return 0.0 for non-creatures
      (is (= 0.0 (strategy/get-creature-amber-potential action-card))))))

(deftest test-calculate-house-battleline-potential
  (testing "calculate-house-battleline-potential sums correctly"
    (let [brobnar-creature-1
            (create-test-card "Brobnar 1" :brobnar :creature 0 1.5)
          brobnar-creature-2
            (create-test-card "Brobnar 2" :brobnar :creature 1 2.0)
          dis-creature (create-test-card "Dis Creature" :dis :creature 0 1.0)
          brobnar-action
            (create-test-card "Brobnar Action" :brobnar :action 2 nil)
          battleline [brobnar-creature-1 brobnar-creature-2 dis-creature
                      brobnar-action]]
      ;; Should sum only creatures of the specified house
      (is (= 3.5
             (strategy/calculate-house-battleline-potential battleline
                                                            :brobnar)))
      (is (= 1.0
             (strategy/calculate-house-battleline-potential battleline :dis)))
      (is (= 0.0
             (strategy/calculate-house-battleline-potential battleline :logos)))
      ;; Should handle empty battleline
      (is (= 0.0
             (strategy/calculate-house-battleline-potential [] :brobnar))))))

(deftest test-calculate-battleline-delta
  (testing "calculate-battleline-delta computes difference correctly"
    (let [;; Player with 3 brobnar creatures (3.0), 2 dis creatures (2.0),
          ;; 1 logos creature (1.0)
          brobnar-creatures [(create-test-card "B1" :brobnar :creature 0 1.0)
                             (create-test-card "B2" :brobnar :creature 0 1.0)
                             (create-test-card "B3" :brobnar :creature 0 1.0)]
          dis-creatures [(create-test-card "D1" :dis :creature 0 1.0)
                         (create-test-card "D2" :dis :creature 0 1.0)]
          logos-creatures [(create-test-card "L1" :logos :creature 0 1.0)]
          battleline (concat brobnar-creatures dis-creatures logos-creatures)
          player
            (create-test-player "test" [:brobnar :dis :logos] [] battleline)]
      ;; Delta should be best (3.0) - second best (2.0) = 1.0
      (is (= 1.0 (strategy/calculate-battleline-delta player)))
      ;; Test with empty battleline
      (let [empty-player (create-test-player "empty" [:brobnar :dis :logos])]
        (is (= 0.0 (strategy/calculate-battleline-delta empty-player))))
      ;; Test with only one house having creatures
      (let [single-house-player (create-test-player "single"
                                                    [:brobnar :dis :logos]
                                                    []
                                                    brobnar-creatures)]
        (is (= 3.0
               (strategy/calculate-battleline-delta single-house-player)))))))

(deftest test-get-best-battleline-house
  (testing "get-best-battleline-house returns house with highest potential"
    (let [brobnar-creatures [(create-test-card "B1" :brobnar :creature 0 2.0)
                             (create-test-card "B2" :brobnar :creature 0 1.5)]
          dis-creatures [(create-test-card "D1" :dis :creature 0 1.0)]
          battleline (concat brobnar-creatures dis-creatures)
          player
            (create-test-player "test" [:brobnar :dis :logos] [] battleline)]
      ;; Should return brobnar (3.5 total)
      (is (= :brobnar (strategy/get-best-battleline-house player)))
      ;; Test with empty battleline
      (let [empty-player (create-test-player "empty" [:brobnar :dis :logos])]
        (is (nil? (strategy/get-best-battleline-house empty-player)))))))

(deftest test-calculate-hand-amber-potential
  (testing "calculate-hand-amber-potential sums correctly"
    (let [brobnar-card-1 (create-test-card "B1" :brobnar :action 2 1.5)
          brobnar-card-2 (create-test-card "B2" :brobnar :creature 1 nil) ; fallback
          ; to amber value
          dis-card (create-test-card "D1" :dis :action 1 2.0)
          hand [brobnar-card-1 brobnar-card-2 dis-card]]
      ;; Should sum expected-amber or fallback to amber value for brobnar
      ;; cards
      (is (= 2.5 (strategy/calculate-hand-amber-potential hand :brobnar))) ; 1.5
      ; + 1.0
      (is (= 2.0 (strategy/calculate-hand-amber-potential hand :dis)))
      (is (= 0.0 (strategy/calculate-hand-amber-potential hand :logos))))))

(deftest test-get-hand-house-potentials
  (testing "get-hand-house-potentials returns map of house to potential"
    (let [brobnar-card (create-test-card "B1" :brobnar :action 1 2.0)
          dis-card (create-test-card "D1" :dis :creature 0 1.5)
          hand [brobnar-card dis-card]
          player (create-test-player "test" [:brobnar :dis :logos] hand [])
          potentials (strategy/get-hand-house-potentials player)]
      (is (= 2.0 (:brobnar potentials)))
      (is (= 1.5 (:dis potentials)))
      (is (= 0.0 (:logos potentials))))))

(deftest test-should-stick-with-battleline-house
  (testing "should-stick-with-battleline-house? returns correct boolean"
    (let [;; Current player has strong brobnar battleline (3.0)
          current-battleline [(create-test-card "B1" :brobnar :creature 0 1.5)
                              (create-test-card "B2" :brobnar :creature 0 1.5)]
          current-player (create-test-player "current"
                                             [:brobnar :dis :logos]
                                             []
                                             current-battleline)
          ;; Opponent has weaker dis battleline (1.0)
          opponent-battleline [(create-test-card "D1" :dis :creature 0 1.0)]
          opponent (create-test-player "opponent"
                                       [:brobnar :dis :logos]
                                       []
                                       opponent-battleline)]
      ;; Should stick because current delta (3.0) > opponent delta (1.0)
      ;; and > 0
      (is (true? (strategy/should-stick-with-battleline-house? current-player
                                                               opponent)))
      ;; Should not stick if opponent has better battleline
      (is (false? (strategy/should-stick-with-battleline-house?
                    opponent
                    current-player)))
      ;; Should not stick if no creatures on battleline
      (let [empty-player (create-test-player "empty" [:brobnar :dis :logos])]
        (is (false? (strategy/should-stick-with-battleline-house?
                      empty-player
                      opponent)))))))

(deftest test-should-switch-for-hand-advantage
  (testing "should-switch-for-hand-advantage? implements exceptions correctly"
    (let [;; Player with some battleline presence
          battleline [(create-test-card "B1" :brobnar :creature 0 1.0)]
          ;; Exception (a): High hand potential with multiple cards
          hand-high-potential [(create-test-card "D1" :dis :action 1 2.0)
                               (create-test-card "D2" :dis :action 1 2.0)] ; 4.0
          ; total > 1.0 battleline
          player-a (create-test-player "test-a"
                                       [:brobnar :dis :logos]
                                       hand-high-potential
                                       battleline)
          ;; Exception (b): Low delta (≤3), no cards of best house, many
          ;; cards of other house
          hand-many-dis [(create-test-card "D1" :dis :action 1 1.0)
                         (create-test-card "D2" :dis :action 1 1.0)
                         (create-test-card "D3" :dis :action 1 1.0)] ; 3 cards,
          ; no brobnar
          battleline-low [(create-test-card "B1" :brobnar :creature 0 2.0)] ; delta
          ; = 2.0 ≤ 3
          player-b (create-test-player "test-b"
                                       [:brobnar :dis :logos]
                                       hand-many-dis
                                       battleline-low)
          opponent (create-test-player "opponent" [:brobnar :dis :logos])]
      ;; Test exception (a)
      (let [[should-switch? house]
              (strategy/should-switch-for-hand-advantage? player-a opponent)]
        (is (true? should-switch?))
        (is (= :dis house)))
      ;; Test exception (b)
      (let [[should-switch? house]
              (strategy/should-switch-for-hand-advantage? player-b opponent)]
        (is (true? should-switch?))
        (is (= :dis house)))
      ;; Test no switching when conditions not met
      (let [normal-hand [(create-test-card "B1" :brobnar :action 1 1.0)]
            normal-player (create-test-player "normal"
                                              [:brobnar :dis :logos]
                                              normal-hand
                                              battleline)
            [should-switch? house] (strategy/should-switch-for-hand-advantage?
                                     normal-player
                                     opponent)]
        (is (false? should-switch?))
        (is (nil? house))))))

(deftest test-choose-house-strategic
  (testing "choose-house-strategic makes correct strategic decisions"
    (let [;; Scenario 1: Should stick with battleline house
          strong-battleline [(create-test-card "B1" :brobnar :creature 0 2.0)
                             (create-test-card "B2" :brobnar :creature 0 1.0)]
          normal-hand [(create-test-card "B3" :brobnar :action 1 1.0)]
          current-player (create-test-player "current"
                                             [:brobnar :dis :logos]
                                             normal-hand
                                             strong-battleline)
          weak-battleline [(create-test-card "D1" :dis :creature 0 1.0)]
          opponent (create-test-player "opponent"
                                       [:brobnar :dis :logos]
                                       []
                                       weak-battleline)]
      ;; Should choose brobnar (best battleline house with advantage)
      (is (= :brobnar
             (strategy/choose-house-strategic current-player opponent)))
      ;; Scenario 2: Should stick with battleline even with good hand
      ;; (battleline advantage)
      (let [hand-advantage [(create-test-card "D1" :dis :action 1 3.0)
                            (create-test-card "D2" :dis :action 1 3.0)] ; 6.0
            ; total > 3.0 battleline
            switch-player (create-test-player "switch"
                                              [:brobnar :dis :logos]
                                              hand-advantage
                                              strong-battleline)]
        ;; New behavior: stick with battleline when we have significant
        ;; advantage
        (is (= :brobnar
               (strategy/choose-house-strategic switch-player opponent))))
      ;; Scenario 3: Switch when no battleline advantage but great hand
      ;; potential
      (let [no-battleline []
            great-hand [(create-test-card "D1" :dis :action 1 3.0)
                        (create-test-card "D2" :dis :action 1 3.0)
                        (create-test-card "L1" :logos :action 1 1.0)]
            switch-player (create-test-player "switch"
                                              [:brobnar :dis :logos]
                                              great-hand
                                              no-battleline)]
        ;; Should switch to dis (best hand potential with no battleline to
        ;; defend)
        (is (= :dis (strategy/choose-house-strategic switch-player opponent))))
      ;; Scenario 4: Fallback to most cards in hand when no clear advantage
      (let [balanced-hand [(create-test-card "L1" :logos :action 1 1.0)
                           (create-test-card "L2" :logos :action 1 1.0)
                           (create-test-card "L3" :logos :action 1 1.0)
                           (create-test-card "D1" :dis :action 1 1.0)]
            fallback-player (create-test-player "fallback"
                                                [:brobnar :dis :logos]
                                                balanced-hand
                                                [])]
        (is (= :logos
               (strategy/choose-house-strategic fallback-player opponent)))))))

;; ============================================================================
;; Strategic Decision Tests (Threat-Aware)
;; ============================================================================

(deftest test-should-stick-with-battleline-house-threat-aware
  (testing "battleline house decision considering relative opponent threat"
    (let [current-player
            (create-test-player
              "player1"
              [:brobnar :dis :sanctum]
              []
              [(create-test-card "creature1" :brobnar :creature 0 2.0)
               (create-test-card "creature2" :brobnar :creature 0 1.5)
               (create-test-card "creature3" :dis :creature 0 1.0)]) ; current
          ; delta = 2.5
          ;; Low relative threat (opponent weaker than us)
          low-threat-opponent
            (create-test-player
              "opponent"
              [:mars :shadows :logos]
              []
              [(create-test-card "enemy1" :mars :creature 0 1.0)]) ; opponent
          ; delta = 1.0, threat = 1.0 - 2.5 = -1.5
          ;; High relative threat (opponent much stronger)
          high-threat-opponent
            (create-test-player
              "opponent"
              [:mars :shadows :logos]
              []
              [(create-test-card "enemy1" :mars :creature 0 3.0)
               (create-test-card "enemy2" :mars :creature 0 3.0)
               (create-test-card "enemy3" :mars :creature 0 1.0)]) ; opponent
          ; delta = 6.0, threat = 6.0 - 2.5 = 3.5
         ]
      ;; Against low threat: should stick with advantage (we're stronger)
      (is (= true
             (strategy/should-stick-with-battleline-house?
               current-player
               low-threat-opponent)))
      ;; Against high threat: should be more cautious (they're much
      ;; stronger)
      (is (= false
             (strategy/should-stick-with-battleline-house?
               current-player
               high-threat-opponent))))))

(deftest test-should-switch-for-hand-advantage-threat-aware
  (testing "hand advantage switching with relative threat consideration"
    (let [;; Player with moderate battleline but good hand potential
          current-player
            (create-test-player
              "player1"
              [:brobnar :dis :sanctum]
              [(create-test-card "hand1" :dis :action 2 2.0)
               (create-test-card "hand2" :dis :action 1 1.5)
               (create-test-card "hand3" :dis :creature 0 1.0)] ; dis hand
              ; potential = 4.5
              [(create-test-card "creature1" :brobnar :creature 0 1.0)]) ; current
          ; delta = 1.0
          ;; Low relative threat opponent (we have advantage)
          low-threat-opponent
            (create-test-player
              "opponent"
              [:mars :shadows :logos]
              []
              [(create-test-card "enemy1" :mars :creature 0 0.5)]) ; opponent
          ; delta = 0.5, threat = 0.5 - 1.0 = -0.5
          ;; High relative threat opponent
          high-threat-opponent
            (create-test-player
              "opponent"
              [:mars :shadows :logos]
              []
              [(create-test-card "enemy1" :mars :creature 0 3.0)
               (create-test-card "enemy2" :mars :creature 0 2.0)]) ; opponent
          ; delta = 5.0, threat = 5.0 - 1.0 = 4.0
         ]
      ;; Against low threat: should switch easily (normal threshold 1.0x)
      (let [[should-switch? house] (strategy/should-switch-for-hand-advantage?
                                     current-player
                                     low-threat-opponent)]
        (is (= true should-switch?))
        (is (= :dis house)))
      ;; Against high threat: needs bigger advantage (threshold 1.5x), so
      ;; 4.5 > 1.5 * 1.0 still switches
      (let [[should-switch? house] (strategy/should-switch-for-hand-advantage?
                                     current-player
                                     high-threat-opponent)]
        (is (= true should-switch?))
        (is (= :dis house))))))

(deftest test-choose-house-strategic-relative-threat-response
  (testing "strategic house choice responds to relative opponent threat level"
    (let [;; Player with balanced options
          current-player
            (create-test-player
              "player1"
              [:brobnar :dis :sanctum]
              [(create-test-card "hand1" :dis :action 1 1.0) ; dis has
               ; moderate potential
               ; (1.0)
               (create-test-card "hand2" :sanctum :creature 0 1.0) ; sanctum
               ; has more cards
               (create-test-card "hand3" :sanctum :action 0 1.0)
               (create-test-card "hand4" :sanctum :artifact 0 1.0)
               (create-test-card "hand5" :brobnar :action 1 1.0)]  ; brobnar
              ; has least
              [(create-test-card "my-creature" :brobnar :creature 0 1.0)]) ; current
          ; delta = 1.0
          ;; Low relative threat opponent (we have advantage)
          low-threat-opponent
            (create-test-player
              "opponent"
              [:mars :shadows :logos]
              []
              [(create-test-card "enemy1" :mars :creature 0 0.5)]) ; opponent
          ; delta = 0.5, threat = 0.5 - 1.0 = -0.5
          ;; High relative threat opponent (threat > 3.0)
          high-threat-opponent
            (create-test-player
              "opponent"
              [:mars :shadows :logos]
              []
              [(create-test-card "enemy1" :mars :creature 0 2.5)
               (create-test-card "enemy2" :mars :creature 0 2.0)]) ; opponent
          ; delta = 4.5, threat = 4.5 - 1.0 = 3.5
         ]
      ;; Against low threat: should stick with battleline advantage
      ;; (brobnar has creatures on board)
      ;; Since we have advantage (1.0 vs 0.5), stick with brobnar instead
      ;; of falling back to card count
      (is (= :brobnar
             (strategy/choose-house-strategic current-player
                                              low-threat-opponent)))
      ;; Against high threat: should choose house with best amber potential
      ;; or most cards
      (let [result (strategy/choose-house-strategic current-player
                                                    high-threat-opponent)]
        ;; Should be either :sanctum or :dis (both reasonable for high
        ;; threat)
        (is (contains? #{:sanctum :dis} result))))))

(deftest test-relative-threat-levels
  (testing "relative threat level categorization affects strategy"
    (let [current-player
            (create-test-player
              "player1"
              [:brobnar :dis :sanctum]
              [(create-test-card "hand1" :dis :action 1 1.5)]
              [(create-test-card "creature1" :brobnar :creature 0 2.0)]) ; current
          ; delta = 2.0
          no-threat (create-test-player
                      "opponent"
                      [:mars :shadows :logos]
                      []
                      [(create-test-card "enemy1" :mars :creature 0 1.0)])
          high-threat (create-test-player
                        "opponent"
                        [:mars :shadows :logos]
                        []
                        [(create-test-card "enemy1" :mars :creature 0 4.0)
                         (create-test-card "enemy2" :mars :creature 0 2.0)])]
      (is (= true
             (strategy/should-stick-with-battleline-house? current-player
                                                           no-threat)))
      (is (= false
             (strategy/should-stick-with-battleline-house? current-player
                                                           high-threat))))))

(deftest test-can-forge-next-turn
  (testing "forge threat detection based on current amber and key cost"
    (let [;; Player with 6+ amber - can forge any key
          player-base
            (create-test-player "player" [:brobnar :dis :sanctum] [] [])
          player-6-amber-0-keys (assoc player-base
                                  :amber 6
                                  :keys 0)
          player-5-amber-0-keys (assoc player-base
                                  :amber 5
                                  :keys 0)
          ;; Player with 1 key and 6+ amber - can forge second key
          player-6-amber-1-key (assoc player-base
                                 :amber 6
                                 :keys 1)
          player-5-amber-1-key (assoc player-base
                                 :amber 5
                                 :keys 1)
          ;; Player with 2 keys and 6+ amber - can forge third key (game
          ;; over!)
          player-6-amber-2-keys (assoc player-base
                                  :amber 6
                                  :keys 2)
          player-5-amber-2-keys (assoc player-base
                                  :amber 5
                                  :keys 2)]
      ;; All keys cost 6 amber
      (is (= true (strategy/can-forge-next-turn? player-6-amber-0-keys)))
      (is (= false (strategy/can-forge-next-turn? player-5-amber-0-keys)))
      (is (= true (strategy/can-forge-next-turn? player-6-amber-1-key)))
      (is (= false (strategy/can-forge-next-turn? player-5-amber-1-key)))
      (is (= true (strategy/can-forge-next-turn? player-6-amber-2-keys)))
      (is (= false (strategy/can-forge-next-turn? player-5-amber-2-keys))))))

(deftest test-is-final-key
  (testing "final key detection"
    (let [player-0-keys
            (create-test-player "player" [:brobnar :dis :sanctum] [] [])
          player-1-key (assoc player-0-keys :keys 1)
          player-2-keys (assoc player-0-keys :keys 2)]
      (is (= false (strategy/is-final-key? player-0-keys)))
      (is (= false (strategy/is-final-key? player-1-key)))
      (is (= true (strategy/is-final-key? player-2-keys))))))

(deftest test-get-card-amber-control
  (testing "amber control value calculation for different card types"
    (let [action-with-amber (create-test-card "steal" :dis :action 2 1.0)
          action-no-amber (create-test-card "play" :dis :action 0 1.0)
          artifact (create-test-card "artifact" :logos :artifact 0 1.0)
          creature-with-amber
            (create-test-card "fighter" :brobnar :creature 1 2.0)
          creature-no-amber
            (create-test-card "vanilla" :brobnar :creature 0 1.0)]
      ;; Actions with amber pips have control
      (is (= 1.0 (strategy/get-card-amber-control action-with-amber)))
      (is (= 0.0 (strategy/get-card-amber-control action-no-amber)))
      ;; Artifacts have some control
      (is (= 0.5 (strategy/get-card-amber-control artifact)))
      ;; Creatures with amber pips have some control
      (is (= 0.5 (strategy/get-card-amber-control creature-with-amber)))
      (is (= 0.0 (strategy/get-card-amber-control creature-no-amber))))))

(deftest test-calculate-amber-control-potential
  (testing "amber control potential calculation for a house"
    (let [player
            (create-test-player
              "player"
              [:brobnar :dis :sanctum]
              ;; Hand: dis has action (1.0) + creature (0.5) = 1.5 control
              [(create-test-card "steal" :dis :action 2 1.0)
               (create-test-card "fighter" :dis :creature 1 2.0)
               (create-test-card "other" :brobnar :action 0 1.0)]
              ;; Battleline: dis has artifact (0.5) control
              [(create-test-card "dis-artifact" :dis :artifact 0 1.0)
               (create-test-card "brobnar-creature" :brobnar :creature 0 2.0)])]
      ;; Dis should have: hand(1.5) + battleline(0.5) = 2.0 control
      (is (= 2.0 (strategy/calculate-amber-control-potential player :dis)))
      ;; Brobnar should have: hand(0.0) + battleline(0.0) = 0.0 control
      (is (= 0.0 (strategy/calculate-amber-control-potential player :brobnar)))
      ;; Sanctum should have no cards = 0.0 control
      (is (= 0.0
             (strategy/calculate-amber-control-potential player :sanctum))))))

(deftest test-get-house-with-most-amber-control
  (testing "selection of house with highest amber control"
    (let [player
            (create-test-player
              "player"
              [:brobnar :dis :sanctum]
              [(create-test-card "steal1" :dis :action 2 1.0) ; dis = 1.0
               (create-test-card "steal2" :dis :action 1 1.0) ; dis = 2.0
               ; total
               (create-test-card "sanctum-artifact" :sanctum :artifact 0 1.0)] ; sanctum
              ; = 0.5
              [])]
      ;; Should choose dis (2.0 control) over sanctum (0.5) over brobnar
      ;; (0.0)
      (is (= :dis (strategy/get-house-with-most-amber-control player))))))

(deftest test-should-use-amber-control-strategy
  (testing "amber control strategy decision logic"
    (let [current-player
            (create-test-player
              "player"
              [:brobnar :dis :sanctum]
              [(create-test-card "hand1" :dis :action 2 2.0)]
              [(create-test-card "creature1" :brobnar :creature 0 1.0)])
          ;; Opponent about to forge final key (2 keys, 6+ amber)
          final-forge-threat
            (-> (create-test-player "opponent" [:mars :shadows :logos] [] [])
                (assoc :keys 2
                       :amber 6))
          ;; Opponent about to forge non-final key (1 key, 6+ amber)
          non-final-forge-threat
            (-> (create-test-player "opponent" [:mars :shadows :logos] [] [])
                (assoc :keys 1
                       :amber 6))
          ;; Opponent with no forge threat
          no-threat
            (-> (create-test-player "opponent" [:mars :shadows :logos] [] [])
                (assoc :keys 1
                       :amber 5))]
      ;; Final key threat: ALWAYS use amber control
      (is (= true
             (strategy/should-use-amber-control-strategy? current-player
                                                          final-forge-threat)))
      ;; Non-final key threat: depends on amber potential comparison
      ;; Our best potential: dis house = 2.0, opponent potential ~2.0
      ;; Since 2.0 < 1.5 * 2.0 = 3.0, should use amber control strategy
      (is (= true
             (strategy/should-use-amber-control-strategy?
               current-player
               non-final-forge-threat)))
      ;; No forge threat: use normal strategy
      (is (= false
             (strategy/should-use-amber-control-strategy? current-player
                                                          no-threat))))))

(deftest test-amber-control-integration
  (testing "amber control strategy integration in main house selection"
    (let [current-player
            (create-test-player
              "player"
              [:brobnar :dis :sanctum]
              [(create-test-card "steal" :dis :action 2 1.0) ; dis has amber
               ; control
               (create-test-card "creature" :brobnar :creature 0 2.0)
               (create-test-card "card" :sanctum :action 1 1.0)]
              [])
          ;; Opponent about to forge final key - MUST use amber control
          forge-threat
            (-> (create-test-player "opponent" [:mars :shadows :logos] [] [])
                (assoc :keys 2
                       :amber 6))
          ;; Opponent with no threat - use normal strategy
          no-threat
            (-> (create-test-player "opponent" [:mars :shadows :logos] [] [])
                (assoc :keys 1
                       :amber 3))]
      ;; Against forge threat: should choose dis (amber control house)
      (is (= :dis
             (strategy/choose-house-strategic current-player forge-threat)))
      ;; Against no threat: should use normal strategy. With no battleline
      ;; and equal card distribution, result may vary
      (let [result (strategy/choose-house-strategic current-player no-threat)]
        ;; Should be one of the reasonable choices
        (is (contains? #{:brobnar :dis :sanctum} result))))))

(deftest test-predict-our-amber-potential
  (testing "combines hand and battleline potential for a house"
    (let [hand [(create-test-card "action1" :brobnar :action 2 1.5)
                (create-test-card "action2" :brobnar :action 1 1.0)
                (create-test-card "dis-card" :dis :action 1 2.0)]
          battleline [(create-test-card "creature1" :brobnar :creature 0 2.0)
                      (create-test-card "creature2" :dis :creature 0 1.0)]
          player (create-test-player "player"
                                     [:brobnar :dis :logos]
                                     hand
                                     battleline)]
      ;; Brobnar: hand (2.5) + battleline (2.0) = 4.5
      (is (= 4.5 (strategy/predict-our-amber-potential player :brobnar)))
      ;; Dis: hand (2.0) + battleline (1.0) = 3.0
      (is (= 3.0 (strategy/predict-our-amber-potential player :dis)))
      ;; Logos: hand (0) + battleline (0) = 0.0
      (is (= 0.0 (strategy/predict-our-amber-potential player :logos))))))

(deftest test-predict-opponent-amber-potential
  (testing
    "estimates opponent's amber generation from battleline delta + hand estimate"
    (let [;; Opponent with strong battleline: 3 mars creatures (3.0), 1
          ;; shadows (1.0)
          ;; Delta = 3.0 - 1.0 = 2.0
          opponent-battleline [(create-test-card "m1" :mars :creature 0 1.0)
                               (create-test-card "m2" :mars :creature 0 1.0)
                               (create-test-card "m3" :mars :creature 0 1.0)
                               (create-test-card "s1" :shadows :creature 0 1.0)]
          opponent (create-test-player "opponent"
                                       [:mars :shadows :logos]
                                       []
                                       opponent-battleline)]
      ;; Prediction = battleline delta (2.0) + hand estimate (2.0 default)
      ;; = 4.0
      (is (= 4.0 (strategy/predict-opponent-amber-potential opponent)))))
  (testing "handles empty battleline"
    (let [opponent
            (create-test-player "opponent" [:mars :shadows :logos] [] [])]
      ;; No battleline = delta 0.0 + hand estimate 2.0 = 2.0
      (is (= 2.0 (strategy/predict-opponent-amber-potential opponent))))))

(defn create-creature-card
  "Create a test creature with specified power"
  [name house power]
  {:id (str "card-" name),
   :name name,
   :house house,
   :card-type :creature,
   :amber 0,
   :power power,
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
   :expected-amber nil})

(defn create-creature-with-amber
  "Create a test creature with specified power and expected amber"
  [name house power expected-amber]
  (assoc (create-creature-card name house power)
    :expected-amber expected-amber))

(deftest test-calculate-battleline-power
  (testing "sums power of all creatures"
    (let [creatures [(create-creature-card "c1" :brobnar 3)
                     (create-creature-card "c2" :dis 4)
                     (create-creature-card "c3" :logos 2)]]
      (is (= 9 (strategy/calculate-battleline-power creatures)))))
  (testing "handles empty battleline"
    (is (= 0 (strategy/calculate-battleline-power []))))
  (testing "handles nil power gracefully"
    (let [creatures [(create-creature-card "c1" :brobnar 3)
                     (assoc (create-creature-card "c2" :dis 0) :power nil)]]
      (is (= 3 (strategy/calculate-battleline-power creatures))))))

(deftest test-estimate-fight-casualties
  (testing "estimates losses when we're stronger"
    (let [our-creatures [(create-creature-with-amber "strong1" :brobnar 6 1.0)
                         (create-creature-with-amber "strong2" :brobnar 5 1.0)]
          enemy-creatures [(create-creature-with-amber "weak1" :mars 3 1.0)
                           (create-creature-with-amber "weak2" :mars 2 1.0)]
          {:keys [our-losses their-losses]}
            (strategy/estimate-fight-casualties our-creatures enemy-creatures)]
      ;; Our 6 vs their 3: we win (they lose 1.0), they lose
      ;; Our 5 vs their 2: we win (they lose 1.0), they lose
      ;; We lose nothing (6>3, 5>2), they lose 2.0
      (is (= 0.0 our-losses))
      (is (= 2.0 their-losses))))
  (testing "estimates losses when we're weaker"
    (let [our-creatures [(create-creature-with-amber "weak" :brobnar 2 1.0)]
          enemy-creatures [(create-creature-with-amber "strong" :mars 5 1.0)]
          {:keys [our-losses their-losses]}
            (strategy/estimate-fight-casualties our-creatures enemy-creatures)]
      ;; Our 2 vs their 5: we lose (5>=2), they survive (2<5)
      (is (= 1.0 our-losses))
      (is (= 0.0 their-losses))))
  (testing "estimates mutual destruction when equal power"
    (let [our-creatures [(create-creature-with-amber "equal" :brobnar 4 1.5)]
          enemy-creatures [(create-creature-with-amber "equal" :mars 4 2.0)]
          {:keys [our-losses their-losses]}
            (strategy/estimate-fight-casualties our-creatures enemy-creatures)]
      ;; Both 4 power: both die (4>=4 both ways)
      (is (= 1.5 our-losses))
      (is (= 2.0 their-losses))))
  (testing "handles empty lists"
    (let [{:keys [our-losses their-losses]}
            (strategy/estimate-fight-casualties [] [])]
      (is (= 0.0 our-losses))
      (is (= 0.0 their-losses)))))

(deftest test-calculate-delta-swing
  (testing "positive swing when we kill more value"
    (let [our-creatures [(create-creature-with-amber "strong" :brobnar 6 1.0)]
          enemy-creatures [(create-creature-with-amber "weak" :mars 3 2.0)]]
      ;; We lose 0.0, they lose 2.0 → swing = 2.0 - 0.0 = +2.0
      (is (= 2.0
             (strategy/calculate-delta-swing our-creatures enemy-creatures)))))
  (testing "negative swing when we lose more value"
    (let [our-creatures [(create-creature-with-amber "weak" :brobnar 2 2.0)]
          enemy-creatures [(create-creature-with-amber "strong" :mars 5 1.0)]]
      ;; We lose 2.0, they lose 0.0 → swing = 0.0 - 2.0 = -2.0
      (is (= -2.0
             (strategy/calculate-delta-swing our-creatures enemy-creatures)))))
  (testing "zero swing when equal trade"
    (let [our-creatures [(create-creature-with-amber "even" :brobnar 4 1.0)]
          enemy-creatures [(create-creature-with-amber "even" :mars 4 1.0)]]
      ;; Both lose 1.0 → swing = 1.0 - 1.0 = 0.0
      (is (= 0.0
             (strategy/calculate-delta-swing our-creatures enemy-creatures)))))
  (testing "favorable even when both die, if their value is higher"
    (let [our-creatures [(create-creature-with-amber "low-value" :brobnar
                                                     5 0.5)]
          enemy-creatures [(create-creature-with-amber "high-value" :mars
                                                       5 3.0)]]
      ;; Both die: we lose 0.5, they lose 3.0 → swing = 3.0 - 0.5 = +2.5
      (is (= 2.5
             (strategy/calculate-delta-swing our-creatures enemy-creatures))))))

(deftest test-should-fight-or-reap-no-enemies
  (testing "should reap when opponent has no creatures"
    (let [my-creatures [(create-creature-card "fighter1" :brobnar 3)
                        (create-creature-card "fighter2" :brobnar 4)]
          current-player
            (create-test-player "player" [:brobnar :dis :logos] [] my-creatures)
          opponent
            (create-test-player "opponent" [:mars :shadows :sanctum] [] [])] ; no
      ; creatures
      (is (= :reap
             (strategy/should-fight-or-reap? current-player
                                             opponent
                                             :brobnar))))))

(deftest test-should-fight-or-reap-no-fighters
  (testing
    "returns reap (no-op) when we have no creatures of active house to use"
    (let [my-creatures [(create-creature-card "dis-creature" :dis 3)] ; only
          ; dis creatures
          current-player
            (create-test-player "player" [:brobnar :dis :logos] [] my-creatures)
          enemy-creatures [(create-creature-card "enemy" :mars 4)]
          opponent (create-test-player "opponent"
                                       [:mars :shadows :sanctum]
                                       []
                                       enemy-creatures)]
      ;; Active house is brobnar but we only have dis creatures on
      ;; battleline Can't fight OR reap - returns :reap as a safe no-op
      ;; (reap with 0
      ;; creatures = 0 amber)
      (is (= :reap
             (strategy/should-fight-or-reap? current-player
                                             opponent
                                             :brobnar))))))

(deftest test-should-fight-or-reap-critical-opponent-near-win
  (testing "MUST fight when opponent has 2 keys and is near forging"
    (let [my-creatures [(create-creature-card "fighter" :brobnar 3)]
          current-player
            (create-test-player "player" [:brobnar :dis :logos] [] my-creatures)
          enemy-creatures [(create-creature-card "enemy" :mars 4)]
          opponent (-> (create-test-player "opponent"
                                           [:mars :shadows :sanctum]
                                           []
                                           enemy-creatures)
                       (assoc :keys 2
                              :amber 5))] ; 2 keys, 5 amber = about to win!
      (is (= :fight
             (strategy/should-fight-or-reap? current-player
                                             opponent
                                             :brobnar))))))

(deftest test-should-fight-or-reap-we-near-forge
  (testing "should reap when we're close to forging a key"
    (let [my-creatures [(create-creature-card "fighter" :brobnar 3)]
          current-player (-> (create-test-player "player"
                                                 [:brobnar :dis :logos]
                                                 []
                                                 my-creatures)
                             (assoc :amber 4)) ; 4 amber = only 2 away from
          ; key
          enemy-creatures [(create-creature-card "enemy" :mars 2)] ; weak
          ; enemy
          opponent (create-test-player "opponent"
                                       [:mars :shadows :sanctum]
                                       []
                                       enemy-creatures)]
      (is (= :reap
             (strategy/should-fight-or-reap? current-player
                                             opponent
                                             :brobnar))))))

(deftest test-should-fight-or-reap-opponent-high-amber
  (testing
    "should prefer fighting when opponent has high amber (forge threat bonus)"
    (let [my-creatures [(create-creature-card "fighter1" :brobnar 3)
                        (create-creature-card "fighter2" :brobnar 4)]
          current-player
            (create-test-player "player" [:brobnar :dis :logos] [] my-creatures)
          enemy-creatures [(create-creature-card "enemy" :mars 2)]
          opponent (-> (create-test-player "opponent"
                                           [:mars :shadows :sanctum]
                                           []
                                           enemy-creatures)
                       (assoc :amber 5))] ; high amber - threatens to forge
      ;; With opponent at 5 amber, forge threat bonus kicks in. Fighting
      ;; removes their reap potential which matters more now
      (is (= :fight
             (strategy/should-fight-or-reap? current-player
                                             opponent
                                             :brobnar))))))

(deftest test-should-fight-or-reap-favorable-delta-swing
  (testing
    "should fight when delta swing favors us (we kill more value than we lose)"
    (let [;; We have strong creatures (high power)
          my-creatures [(create-creature-card "strong1" :brobnar 6)
                        (create-creature-card "strong2" :brobnar 5)]
          current-player
            (create-test-player "player" [:brobnar :dis :logos] [] my-creatures)
          ;; Enemy has weaker creatures - we'll kill them without losing
          ;; ours
          enemy-creatures [(create-creature-card "weak1" :mars 3)
                           (create-creature-card "weak2" :mars 2)]
          opponent (create-test-player "opponent"
                                       [:mars :shadows :sanctum]
                                       []
                                       enemy-creatures)]
      ;; Delta swing: we kill 2.0 amber potential, lose 0 = +2.0 swing
      (is (= :fight
             (strategy/should-fight-or-reap? current-player
                                             opponent
                                             :brobnar))))))

(deftest test-should-fight-or-reap-unfavorable-delta-swing
  (testing
    "should reap when delta swing is against us (we'd lose more than them)"
    (let [;; We have weak creatures
          my-creatures [(create-creature-card "weak1" :brobnar 2)]
          current-player
            (create-test-player "player" [:brobnar :dis :logos] [] my-creatures)
          ;; Enemy has stronger creatures - they'll survive, we'll die
          enemy-creatures [(create-creature-card "strong1" :mars 5)]
          opponent (create-test-player "opponent"
                                       [:mars :shadows :sanctum]
                                       []
                                       enemy-creatures)]
      ;; Delta swing: we kill 0 (they're stronger), lose 1.0 = -1.0 swing
      ;; Better to reap and get guaranteed amber
      (is (= :reap
             (strategy/should-fight-or-reap? current-player
                                             opponent
                                             :brobnar))))))

(deftest test-should-fight-or-reap-severely-outmatched
  (testing "should reap when severely outpowered to avoid suicide"
    (let [;; Single weak creature
          weak-creatures [(create-creature-card "weak" :brobnar 2)]
          weak-player (create-test-player "player"
                                          [:brobnar :dis :logos]
                                          []
                                          weak-creatures)
          ;; Massively stronger enemy (power ratio < 0.5)
          strong-enemies [(create-creature-card "giant1" :mars 6)
                          (create-creature-card "giant2" :mars 6)]
          strong-opponent (create-test-player "opponent"
                                              [:mars :shadows :sanctum]
                                              []
                                              strong-enemies)]
      ;; Power ratio: 2/12 = 0.17 < 0.5 threshold = severely outmatched
      (is (= :reap
             (strategy/should-fight-or-reap? weak-player
                                             strong-opponent
                                             :brobnar))))))

(deftest test-should-fight-or-reap-balanced-trade
  (testing "balanced trade with equal power - delta swing determines outcome"
    (let [my-creatures [(create-creature-card "fighter" :brobnar 3)]
          current-player (-> (create-test-player "player"
                                                 [:brobnar :dis :logos]
                                                 []
                                                 my-creatures)
                             (assoc :amber 2)) ; not near forge
          enemy-creatures [(create-creature-card "enemy" :mars 3)]
          opponent (-> (create-test-player "opponent"
                                           [:mars :shadows :sanctum]
                                           []
                                           enemy-creatures)
                       (assoc :amber 2
                              :keys 0))] ; not threatening
      ;; Equal trade: both die, both lose 1.0 amber potential
      ;; Delta swing = 0, which is < 0.5 threshold = reap
      (is (= :reap
             (strategy/should-fight-or-reap? current-player
                                             opponent
                                             :brobnar))))))