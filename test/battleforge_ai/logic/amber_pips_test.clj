(ns battleforge-ai.logic.amber-pips-test
  (:require [clojure.test :refer :all]
            [schema.test :as schema-test]
            [battleforge-ai.logic.amber-pips :as amber-pips]
            [battleforge-ai.models.game :as game]))

(use-fixtures :once schema-test/validate-schemas)

(def high-amber-card
  {:id "high-amber"
   :name "High Amber Card"
   :house :brobnar
   :card-type :action
   :amber 3
   :power nil
   :armor nil
   :rarity :common
   :card-text nil
   :traits []
   :keywords []
   :expansion 341
   :number "001"
   :image nil})

(def medium-amber-card
  {:id "medium-amber"
   :name "Medium Amber Card"
   :house :dis
   :card-type :creature
   :amber 2
   :power 4
   :armor nil
   :rarity :common
   :card-text nil
   :traits []
   :keywords []
   :expansion 341
   :number "002"
   :image nil})

(def low-amber-card
  {:id "low-amber"
   :name "Low Amber Card"
   :house :logos
   :card-type :action
   :amber 1
   :power nil
   :armor nil
   :rarity :common
   :card-text nil
   :traits []
   :keywords []
   :expansion 341
   :number "003"
   :image nil})

(def no-amber-card
  {:id "no-amber"
   :name "No Amber Card"
   :house :brobnar
   :card-type :creature
   :amber 0
   :power 3
   :armor nil
   :rarity :common
   :card-text nil
   :traits []
   :keywords []
   :expansion 341
   :number "004"
   :image nil})

(defn create-test-player
  "Create a test player with all required fields"
  [hand-cards current-amber]
  {:id "test-player"
   :deck []
   :hand hand-cards
   :discard []
   :purged []
   :archive []
   :battleline []
   :artifacts []
   :houses [:brobnar :dis :logos]
   :amber current-amber
   :keys 0
   :chains 0
   :ready-amber 0})

(deftest test-get-amber-value
  (testing "get-amber-value returns correct amber value from card"
    (is (= 3 (amber-pips/get-amber-value high-amber-card)))
    (is (= 2 (amber-pips/get-amber-value medium-amber-card)))
    (is (= 1 (amber-pips/get-amber-value low-amber-card)))
    (is (= 0 (amber-pips/get-amber-value no-amber-card)))))

(deftest test-resolve-amber-pips
  (testing "resolve-amber-pips adds amber to player"
    (let [player (create-test-player [] 5)
          result (amber-pips/resolve-amber-pips player high-amber-card)]
      (is (= 8 (:amber result)))  ; 5 + 3 = 8
      (is (= (:id player) (:id result)))
      (is (= (:hand player) (:hand result))))))

(deftest test-resolve-amber-pips-zero-amber
  (testing "resolve-amber-pips works with zero amber cards"
    (let [player (create-test-player [] 2)
          result (amber-pips/resolve-amber-pips player no-amber-card)]
      (is (= 2 (:amber result)))  ; 2 + 0 = 2
      (is (= player result)))))

(deftest test-resolve-amber-pips-preserves-other-fields
  (testing "resolve-amber-pips preserves other player fields"
    (let [player (-> (create-test-player [low-amber-card] 1)
                     (assoc :keys 2)
                     (assoc :chains 1))
          result (amber-pips/resolve-amber-pips player medium-amber-card)]
      (is (= 3 (:amber result)))  ; 1 + 2 = 3
      (is (= (:id player) (:id result)))
      (is (= (:keys player) (:keys result)))
      (is (= (:chains player) (:chains result)))
      (is (= (:houses player) (:houses result)))
      (is (= (:deck player) (:deck result)))
      (is (= (:hand player) (:hand result)))
      (is (= (:discard player) (:discard result)))
      (is (= (:purged player) (:purged result)))
      (is (= (:archive player) (:archive result)))
      (is (= (:battleline player) (:battleline result)))
      (is (= (:artifacts player) (:artifacts result))))))

(deftest test-calculate-card-priority
  (testing "calculate-card-priority returns amber value as priority"
    (is (= 3 (amber-pips/calculate-card-priority high-amber-card)))
    (is (= 2 (amber-pips/calculate-card-priority medium-amber-card)))
    (is (= 1 (amber-pips/calculate-card-priority low-amber-card)))
    (is (= 0 (amber-pips/calculate-card-priority no-amber-card)))))

(deftest test-choose-card-to-play-highest-amber
  (testing "choose-card-to-play selects card with highest amber value"
    (let [hand [no-amber-card low-amber-card high-amber-card medium-amber-card]
          player (create-test-player hand 0)
          chosen-card (amber-pips/choose-card-to-play player)]
      (is (= high-amber-card chosen-card)))))

(deftest test-choose-card-to-play-tie-first
  (testing "choose-card-to-play selects first card when tied for highest"
    (let [hand [medium-amber-card high-amber-card medium-amber-card]
          player (create-test-player hand 0)
          chosen-card (amber-pips/choose-card-to-play player)]
      (is (= high-amber-card chosen-card)))))

(deftest test-choose-card-to-play-empty-hand
  (testing "choose-card-to-play returns nil for empty hand"
    (let [player (create-test-player [] 0)
          chosen-card (amber-pips/choose-card-to-play player)]
      (is (nil? chosen-card)))))

(deftest test-ai-card-selection-strategy
  (testing "AI consistently chooses highest amber cards"
    (let [cards [no-amber-card low-amber-card medium-amber-card high-amber-card]
          player (create-test-player cards 0)]
      ;; AI should pick high-amber-card (3 amber)
      (is (= high-amber-card (amber-pips/choose-card-to-play player)))
      
      ;; Test with different order
      (let [player2 (create-test-player (reverse cards) 0)]
        (is (= high-amber-card (amber-pips/choose-card-to-play player2)))))))

(deftest test-amber-accumulation
  (testing "Multiple amber pip resolutions accumulate correctly"
    (let [player (create-test-player [] 0)
          step1 (amber-pips/resolve-amber-pips player high-amber-card)     ; +3 = 3
          step2 (amber-pips/resolve-amber-pips step1 medium-amber-card)   ; +2 = 5
          step3 (amber-pips/resolve-amber-pips step2 low-amber-card)]     ; +1 = 6
      (is (= 3 (:amber step1)))
      (is (= 5 (:amber step2)))
      (is (= 6 (:amber step3))))))

(deftest test-analyze-amber-potential
  (testing "analyze-amber-potential provides correct hand analysis"
    (let [hand [high-amber-card medium-amber-card low-amber-card no-amber-card]
          player (create-test-player hand 2)
          analysis (amber-pips/analyze-amber-potential player)]
      (is (string? analysis))
      (is (re-find #"Current amber: 2" analysis))
      (is (re-find #"Hand amber potential: 6" analysis))  ; 3+2+1+0 = 6
      (is (re-find #"4 cards" analysis)))))

(deftest test-analyze-amber-potential-empty
  (testing "analyze-amber-potential handles empty hand"
    (let [player (create-test-player [] 5)
          analysis (amber-pips/analyze-amber-potential player)]
      (is (string? analysis))
      (is (re-find #"Current amber: 5" analysis))
      (is (re-find #"Hand amber potential: 0" analysis))
      (is (re-find #"0 cards" analysis))))) 