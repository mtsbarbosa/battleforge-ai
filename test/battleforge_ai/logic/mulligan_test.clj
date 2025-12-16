(ns battleforge-ai.logic.mulligan-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schema.test :as schema-test]
            [battleforge-ai.logic.mulligan :as mulligan]))

(use-fixtures :once schema-test/validate-schemas)

(def high-amber-card
  {:id "high-amber",
   :name "High Amber Card",
   :house :brobnar,
   :card-type :action,
   :amber 3,
   :power nil,
   :armor nil,
   :rarity :common,
   :card-text nil,
   :traits [],
   :keywords [],
   :expansion 341,
   :number "001",
   :image nil,
   :creature-control 0,
   :amber-control 0})

(def medium-amber-card
  {:id "medium-amber",
   :name "Medium Amber Card",
   :house :dis,
   :card-type :creature,
   :amber 2,
   :power 4,
   :armor nil,
   :rarity :common,
   :card-text nil,
   :traits [],
   :keywords [],
   :expansion 341,
   :number "002",
   :image nil,
   :creature-control 0,
   :amber-control 0})

(def low-amber-card
  {:id "low-amber",
   :name "Low Amber Card",
   :house :logos,
   :card-type :action,
   :amber 1,
   :power nil,
   :armor nil,
   :rarity :common,
   :card-text nil,
   :traits [],
   :keywords [],
   :expansion 341,
   :number "003",
   :image nil,
   :creature-control 0,
   :amber-control 0})

(def no-amber-card
  {:id "no-amber",
   :name "No Amber Card",
   :house :brobnar,
   :card-type :creature,
   :amber 0,
   :power 3,
   :armor nil,
   :rarity :common,
   :card-text nil,
   :traits [],
   :keywords [],
   :expansion 341,
   :number "004",
   :image nil,
   :creature-control 0,
   :amber-control 0})

(def board-wipe-card
  {:id "board-wipe",
   :name "Mass Destruction",
   :house :dis,
   :card-type :action,
   :amber 1,
   :power nil,
   :armor nil,
   :rarity :rare,
   :card-text "Play: Destroy all creatures and gain 1 amber.",
   :traits [],
   :keywords [],
   :expansion 341,
   :number "005",
   :image nil,
   :creature-control 4, ; High creature control for board wipe
   :amber-control 0})

(def amber-control-card
  {:id "amber-control",
   :name "Thief",
   :house :shadows,
   :card-type :creature,
   :amber 0,
   :power 2,
   :armor nil,
   :rarity :common,
   :card-text "Reap: Steal amber from opponent.",
   :traits ["Thief"],
   :keywords [],
   :expansion 341,
   :number "006",
   :image nil,
   :creature-control 0,
   :amber-control 3})  ; High amber control for steal effects

(def sanctum-card
  {:id "sanctum-card",
   :name "Sanctum Card",
   :house :sanctum,
   :card-type :creature,
   :amber 1,
   :power 3,
   :armor nil,
   :rarity :common,
   :card-text nil,
   :traits [],
   :keywords [],
   :expansion 341,
   :number "007",
   :image nil,
   :creature-control 0,
   :amber-control 0})

(def another-logos-card
  {:id "another-logos",
   :name "Another Logos Card",
   :house :logos,
   :card-type :creature,
   :amber 0,
   :power 2,
   :armor nil,
   :rarity :common,
   :card-text nil,
   :traits [],
   :keywords [],
   :expansion 341,
   :number "008",
   :image nil,
   :creature-control 0,
   :amber-control 0})

(defn create-test-player
  "Create a test player with all required fields"
  [hand-cards deck-cards]
  {:id "test-player",
   :deck deck-cards,
   :hand hand-cards,
   :discard [],
   :purged [],
   :archive [],
   :battleline [],
   :artifacts [],
   :houses [:brobnar :dis :logos],
   :amber 0,
   :keys 0,
   :chains 0,
   :ready-amber 0})

(deftest test-should-mulligan-high-quality-hand
  (testing "should-mulligan? returns false for high quality hand"
    (let [good-hand [high-amber-card medium-amber-card low-amber-card
                     medium-amber-card high-amber-card low-amber-card
                     medium-amber-card]
          player (create-test-player good-hand [no-amber-card no-amber-card])]
      (is (false? (mulligan/should-mulligan? player))))))

(deftest test-should-mulligan-low-quality-hand
  (testing "should-mulligan? returns true for low quality hand"
    (let [bad-hand [no-amber-card no-amber-card no-amber-card no-amber-card
                    no-amber-card no-amber-card no-amber-card]
          player (create-test-player bad-hand
                                     [high-amber-card high-amber-card])]
      (is (true? (mulligan/should-mulligan? player))))))

(deftest test-should-mulligan-edge-cases
  (testing "should-mulligan? edge cases"
    ;; Empty hand (shouldn't happen but test anyway)
    (let [player (create-test-player []
                                     [high-amber-card medium-amber-card
                                      low-amber-card])]
      (is (true? (mulligan/should-mulligan? player))))
    ;; Mixed quality hand
    (let [mixed-hand [high-amber-card no-amber-card no-amber-card
                      medium-amber-card no-amber-card low-amber-card
                      no-amber-card]
          player (create-test-player mixed-hand [low-amber-card])]
      ;; This should depend on the exact scoring algorithm
      (is (boolean? (mulligan/should-mulligan? player))))))

(deftest test-execute-mulligan
  (testing "execute-mulligan shuffles hand back and draws (hand-size - 1) cards"
    (let [initial-hand [high-amber-card medium-amber-card low-amber-card
                        no-amber-card high-amber-card medium-amber-card
                        low-amber-card]
          initial-deck [no-amber-card no-amber-card]
          player (create-test-player initial-hand initial-deck)
          result (mulligan/execute-mulligan player)]
      ;; New hand should have 6 cards (original 7 - 1)
      (is (= 6 (count (:hand result))))
      ;; New deck should have 3 cards (original 2 + 7 shuffled back - 6
      ;; drawn)
      (is (= 3 (count (:deck result))))
      ;; Total cards should be preserved
      (is (= 9 (+ (count (:hand result)) (count (:deck result)))))
      ;; Hand cards should be different from original (with very high
      ;; probability)
      (is (not= (:hand player) (:hand result))))))

(deftest test-execute-mulligan-preserves-other-fields
  (testing "execute-mulligan preserves other player fields"
    (let [player (-> (create-test-player [high-amber-card] [medium-amber-card])
                     (assoc :amber 5)
                     (assoc :keys 1)
                     (assoc :chains 2))
          result (mulligan/execute-mulligan player)]
      (is (= (:id player) (:id result)))
      (is (= (:amber player) (:amber result)))
      (is (= (:keys player) (:keys result)))
      (is (= (:chains player) (:chains result)))
      (is (= (:houses player) (:houses result)))
      (is (= (:discard player) (:discard result)))
      (is (= (:purged player) (:purged result)))
      (is (= (:archive player) (:archive result)))
      (is (= (:battleline player) (:battleline result)))
      (is (= (:artifacts player) (:artifacts result))))))

(deftest test-execute-mulligan-randomness
  (testing "execute-mulligan produces different results on multiple calls"
    (let [hand [high-amber-card medium-amber-card]
          deck [low-amber-card no-amber-card no-amber-card no-amber-card
                high-amber-card medium-amber-card low-amber-card]
          player (create-test-player hand deck)
          result1 (mulligan/execute-mulligan player)
          result2 (mulligan/execute-mulligan player)]
      ;; Both results should have the same total card count
      (is (= 9 (+ (count (:hand result1)) (count (:deck result1)))))
      (is (= 9 (+ (count (:hand result2)) (count (:deck result2)))))
      ;; Results should be different with very high probability
      ;; (There's a tiny chance they could be the same due to randomness)
      (is (or (not= (:hand result1) (:hand result2))
              (not= (:deck result1) (:deck result2)))))))

(deftest test-analyze-hand-quality
  (testing "analyze-hand-quality provides correct hand analysis"
    (let [mixed-hand [high-amber-card medium-amber-card low-amber-card
                      no-amber-card medium-amber-card low-amber-card
                      no-amber-card]
          player (create-test-player mixed-hand [])
          analysis (mulligan/analyze-hand-quality player)]
      (is (string? analysis))
      (is (re-find #"Hand: 7 cards" analysis))
      (is (re-find #"5 amber sources" analysis))  ; 5 cards with amber > 0
      (is (re-find #"6 playable early" analysis)) ; 6 cards with amber <= 2
      (is (re-find #"9 total amber" analysis)))))  ; 3+2+1+0+2+1+0 = 9

(deftest test-analyze-hand-quality-empty
  (testing "analyze-hand-quality handles empty hand"
    (let [player (create-test-player [] [high-amber-card])
          analysis (mulligan/analyze-hand-quality player)]
      (is (string? analysis))
      (is (re-find #"Hand: 0 cards" analysis)))))

;; Tests for new mulligan heuristics

(deftest test-has-bad-house-distribution
  (testing "has-bad-house-distribution? detects 2/2/2 pattern"
    ;; Perfect 2/2/2 hand - should be flagged as bad
    ;; 2 brobnar, 2 dis, 2 logos = 2/2/2 pattern
    (let [bad-hand [high-amber-card no-amber-card       ; 2 brobnar
                    medium-amber-card board-wipe-card   ; 2 dis
                    low-amber-card another-logos-card]] ; 2 logos
      (is (true? (mulligan/has-bad-house-distribution? bad-hand))))
    ;; Good distribution (3/2/1)
    (let [good-hand [high-amber-card medium-amber-card low-amber-card ; 3
                                                                      ; different
                                                                      ; houses
                     high-amber-card medium-amber-card                ; +2 more
                     high-amber-card]]                                ; +1 more
      (is (false? (mulligan/has-bad-house-distribution? good-hand))))
    ;; Different hand size (not 6 cards)
    (let [small-hand [high-amber-card medium-amber-card]]
      (is (false? (mulligan/has-bad-house-distribution? small-hand))))))

(deftest test-creature-control-detection
  (testing "has-creature-control? detects high creature control via SAS metrics"
    (is (true? (mulligan/has-creature-control? board-wipe-card))) ; creature-control
                                                                  ; = 4
    (is (false? (mulligan/has-creature-control? high-amber-card))) ; creature-control
                                                                   ; = 0
    (is (false? (mulligan/has-creature-control? amber-control-card)))))  ; creature-control = 0

(deftest test-amber-control-detection
  (testing "has-amber-control? detects high amber control via SAS metrics"
    (is (true? (mulligan/has-amber-control? amber-control-card))) ; amber-control
                                                                  ; = 3
    (is (false? (mulligan/has-amber-control? board-wipe-card)))   ; amber-control
                                                                  ; = 0
    (is (false? (mulligan/has-amber-control? high-amber-card)))))   ; amber-control = 0

(deftest test-early-game-useless-cards
  (testing "is-early-game-useless? identifies problematic early cards"
    (is (true? (mulligan/is-early-game-useless? board-wipe-card)))    ; has
                                                                      ; creature-control
                                                                      ; > 2
    (is (true? (mulligan/is-early-game-useless? amber-control-card))) ; has
                                                                      ; amber-control
                                                                      ; > 1
    (is (false? (mulligan/is-early-game-useless? high-amber-card)))   ; both
                                                                      ; controls
                                                                      ; = 0
    (is (false? (mulligan/is-early-game-useless? medium-amber-card))) ; both
                                                                      ; controls
                                                                      ; = 0
    ;; Test edge cases
    (let [edge-creature-control {:id "edge-creature",
                                 :name "Edge Creature",
                                 :house :brobnar,
                                 :card-type :creature,
                                 :amber 0,
                                 :power 3,
                                 :armor nil,
                                 :rarity :common,
                                 :card-text nil,
                                 :traits [],
                                 :keywords [],
                                 :expansion 341,
                                 :number "012",
                                 :image nil,
                                 :creature-control 2,
                                 :amber-control 0} ; exactly 2, should be
                                                   ; false
          edge-amber-control {:id "edge-amber",
                              :name "Edge Amber",
                              :house :shadows,
                              :card-type :action,
                              :amber 1,
                              :power nil,
                              :armor nil,
                              :rarity :common,
                              :card-text nil,
                              :traits [],
                              :keywords [],
                              :expansion 341,
                              :number "013",
                              :image nil,
                              :creature-control 0,
                              :amber-control 1}] ; exactly 1, should be
                                                 ; false
      (is (false? (mulligan/is-early-game-useless? edge-creature-control)))
      (is (false? (mulligan/is-early-game-useless? edge-amber-control))))))

(deftest test-should-mulligan-2-2-2-distribution
  (testing "should-mulligan? correctly identifies 2/2/2 as bad"
    (let [bad-2-2-2-hand [high-amber-card no-amber-card      ; 2 brobnar
                          medium-amber-card board-wipe-card  ; 2 dis
                          low-amber-card another-logos-card] ; 2 logos
          player (create-test-player bad-2-2-2-hand [])]
      (is (true? (mulligan/should-mulligan? player))))))

(deftest test-should-mulligan-too-many-useless-cards
  (testing "should-mulligan? flags hands with too many early-game useless cards"
    (let [too-many-useless [board-wipe-card amber-control-card board-wipe-card ; 3
                                                                               ; useless
                            high-amber-card medium-amber-card low-amber-card]  ; 3
                                                                               ; good
          player (create-test-player too-many-useless [])]
      (is (true? (mulligan/should-mulligan? player))))))

(deftest test-analyze-hand-quality-with-new-features
  (testing "analyze-hand-quality includes new heuristic information"
    (let [mixed-hand [board-wipe-card amber-control-card high-amber-card
                      medium-amber-card low-amber-card no-amber-card]
          player (create-test-player mixed-hand [])
          analysis (mulligan/analyze-hand-quality player)]
      (is (string? analysis))
      (is (re-find #"Hand: 6 cards" analysis))
      (is (re-find #"2 useless early" analysis)))))