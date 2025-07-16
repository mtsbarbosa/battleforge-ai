(ns battleforge-ai.models.deck-test
  (:require [clojure.test :refer :all]
            [schema.test :as schema-test]
            [java-time :as time]
            [battleforge-ai.models.deck :as deck]))

(use-fixtures :once schema-test/validate-schemas)

(def sample-card
  {:id "card-123"
   :name "Brobnar Bash"
   :house :brobnar
   :card-type :action
   :amber 1
   :power nil
   :armor nil
   :rarity :common
   :card-text "Deal 3 damage to target creature."
   :traits ["Brobnar"]
   :keywords []
   :expansion 341
   :number "001"
   :image nil})

(def sample-deck
  {:id "deck-456"
   :name "Mighty Brobnar Deck"
   :uuid "550e8400-e29b-41d4-a716-446655440000"
   :identity "mighty-brobnar-deck"
   :houses [:brobnar :dis :logos]
   :cards [sample-card]
   :expansion 341
   :source :manual
   :power-level 75
   :chains 0
   :wins 5
   :losses 3
   :win-rate 0.625
   :usage-count nil
   :verified? nil
   :is-alliance? false
   :last-updated nil
   :fetched-at (time/instant)
   :total-power 120
   :total-amber 18
   :creature-count 12
   :action-count 15
   :artifact-count 6
   :upgrade-count 3})

(deftest test-card-schema
  (testing "valid card conforms to schema"
    (is (nil? (schema.core/check deck/Card sample-card))))
  
  (testing "invalid card fails schema validation"
    (let [invalid-card (assoc sample-card :house :invalid-house)]
      (is (some? (schema.core/check deck/Card invalid-card))))))

(deftest test-deck-schema
  (testing "valid deck conforms to schema"
    (is (nil? (schema.core/check deck/Deck sample-deck))))
  
  (testing "deck with invalid house fails"
    (let [invalid-deck (assoc sample-deck :houses [:invalid :house :names])]
      (is (some? (schema.core/check deck/Deck invalid-deck))))))

(deftest test-house-enum
  (testing "all keyforge houses are supported"
    (let [houses [:brobnar :dis :logos :mars :sanctum :shadows :untamed
                  :star-alliance :saurian :unfathomable :ekwidon :geistoid]]
      (doseq [house houses]
        (is (nil? (schema.core/check deck/House house)))))))

;; TODO: Add tests for utility functions when implemented
(deftest test-utility-functions
  (testing "deck->houses function exists"
    (is (function? deck/deck->houses)))
  
  (testing "count-by-type function exists"
    (is (function? deck/count-by-type)))
  
  (testing "calculate-deck-power function exists"
    (is (function? deck/calculate-deck-power)))
  
  (testing "validate-deck function exists"
    (is (function? deck/validate-deck)))) 