(ns battleforge-ai.controllers.battle-test
  (:require [clojure.test :refer :all]
            [schema.test :as schema-test]
            [java-time :as time]
            [battleforge-ai.controllers.battle :as battle-controller]
            [battleforge-ai.models.deck :as deck]
            [battleforge-ai.models.battle :as battle]))

(use-fixtures :once schema-test/validate-schemas)

(def sample-card
  {:id "card-1"
   :name "Sample Card"
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

(def sample-deck-1
  {:id "test-deck-1"
   :name "Test Deck 1"
   :houses [:brobnar :dis :logos]
   :cards (vec (repeat 36 sample-card))
   :expansion 341
   :source :manual
   :fetched-at (time/instant)
   :win-rate nil
   :last-updated nil
   :sas-rating nil
   :identity nil
   :is-alliance? nil
   :chains nil
   :upgrade-count nil
   :artifact-count nil
   :usage-count nil
   :verified? nil
   :power-level nil
   :total-power nil
   :action-count nil
   :total-amber nil
   :losses nil
   :uuid nil
   :wins nil
   :creature-count nil})

(def sample-deck-2
  {:id "test-deck-2"
   :name "Test Deck 2"
   :houses [:mars :sanctum :shadows]
   :cards (vec (repeat 36 sample-card))
   :expansion 341
   :source :manual
   :fetched-at (time/instant)
   :win-rate nil
   :last-updated nil
   :sas-rating nil
   :identity nil
   :is-alliance? nil
   :chains nil
   :upgrade-count nil
   :artifact-count nil
   :usage-count nil
   :verified? nil
   :power-level nil
   :total-power nil
   :action-count nil
   :total-amber nil
   :losses nil
   :uuid nil
   :wins nil
   :creature-count nil})

(deftest test-create-game-id
  (testing "create-game-id generates unique IDs"
    (let [id1 (battle-controller/create-game-id)
          id2 (battle-controller/create-game-id)]
      
      (is (string? id1))
      (is (string? id2))
      (is (not= id1 id2))
      (is (re-find #"^game-" id1))
      (is (re-find #"^game-" id2)))))

(deftest test-validate-battle-params-success
  (testing "validate-battle-params! succeeds with valid params"
    (let [params {:deck1 sample-deck-1
                  :deck2 sample-deck-2
                  :num-games 10}]
      
      (is (true? (battle-controller/validate-battle-params! params))))))

(deftest test-validate-battle-params-missing-deck1
  (testing "validate-battle-params! throws error when deck1 is missing"
    (let [params {:deck2 sample-deck-2
                  :num-games 10}]
      
      (is (thrown-with-msg? clojure.lang.ExceptionInfo 
                           #"Deck1 is required"
                           (battle-controller/validate-battle-params! params))))))

(deftest test-validate-battle-params-missing-deck2
  (testing "validate-battle-params! throws error when deck2 is missing"
    (let [params {:deck1 sample-deck-1
                  :num-games 10}]
      
      (is (thrown-with-msg? clojure.lang.ExceptionInfo 
                           #"Deck2 is required"
                           (battle-controller/validate-battle-params! params))))))

(deftest test-validate-battle-params-invalid-num-games
  (testing "validate-battle-params! throws error for invalid num-games"
    (let [params {:deck1 sample-deck-1
                  :deck2 sample-deck-2
                  :num-games 0}]
      
      (is (thrown-with-msg? clojure.lang.ExceptionInfo 
                           #"Number of games must be positive"
                           (battle-controller/validate-battle-params! params))))
    
    (let [params {:deck1 sample-deck-1
                  :deck2 sample-deck-2
                  :num-games -5}]
      
      (is (thrown-with-msg? clojure.lang.ExceptionInfo 
                           #"Number of games must be positive"
                           (battle-controller/validate-battle-params! params))))))

(deftest test-simulate-single-game
  (testing "simulate-single-game produces valid game result"
    (let [game-id "test-game-123"
          result (battle-controller/simulate-single-game sample-deck-1 sample-deck-2 game-id)]
      
      (is (= game-id (:id result)))
      (is (= (:id sample-deck-1) (:player1-deck result)))
      (is (= (:id sample-deck-2) (:player2-deck result)))
      (is (contains? #{:player1 :player2 nil} (:winner result)))
      (is (= :keys (:victory-condition result)))
      (is (>= (:turn-count result) 1))
      (is (>= (:duration-minutes result) 0))
      (is (>= (:player1-keys result) 0))
      (is (>= (:player2-keys result) 0))
      (is (>= (:player1-amber result) 0))
      (is (>= (:player2-amber result) 0))
      (is (instance? java.util.Date (:started-at result)))
      (is (instance? java.util.Date (:ended-at result))))))

(deftest test-simulate-battle-series-single-game
  (testing "simulate-battle-series! with single game"
    (let [params {:deck1 sample-deck-1
                  :deck2 sample-deck-2
                  :num-games 1}
          result (battle-controller/simulate-battle-series! params)]
      
      (is (string? (:id result)))
      (is (= (:id sample-deck-1) (:deck1-id result)))
      (is (= (:id sample-deck-2) (:deck2-id result)))
      (is (= 1 (:total-games result)))
      (is (= 1 (count (:games result))))
      (is (>= (:deck1-wins result) 0))
      (is (>= (:deck2-wins result) 0))
      (is (>= (:ties result) 0))
      (is (= 1 (+ (:deck1-wins result) (:deck2-wins result) (:ties result))))
      (is (>= (:deck1-win-rate result) 0.0))
      (is (<= (:deck1-win-rate result) 1.0))
      (is (>= (:avg-game-length result) 0.0))
      (is (>= (:avg-turn-count result) 1.0))
      (is (instance? java.util.Date (:started-at result)))
      (is (instance? java.util.Date (:completed-at result))))))

(deftest test-simulate-battle-series-multiple-games
  (testing "simulate-battle-series! with multiple games"
    (let [params {:deck1 sample-deck-1
                  :deck2 sample-deck-2
                  :num-games 3}
          result (battle-controller/simulate-battle-series! params)]
      
      (is (= 3 (:total-games result)))
      (is (= 3 (count (:games result))))
      (is (= 3 (+ (:deck1-wins result) (:deck2-wins result) (:ties result))))
      
      ;; Check that all games have the correct deck IDs
      (doseq [game (:games result)]
        (is (= (:id sample-deck-1) (:player1-deck game)))
        (is (= (:id sample-deck-2) (:player2-deck game)))))))

(deftest test-format-battle-summary
  (testing "format-battle-summary produces readable output"
    (let [battle-result {:id "test-battle-123"
                        :deck1-id "deck-a"
                        :deck2-id "deck-b"
                        :total-games 100
                        :deck1-wins 60
                        :deck2-wins 35
                        :ties 5
                        :deck1-win-rate 0.6
                        :deck2-win-rate 0.35
                        :avg-game-length 15.5
                        :avg-turn-count 25.3
                        :config {:deck1-id "deck-a"
                                :deck2-id "deck-b"
                                :num-games 100
                                :timeout-minutes 30
                                :parallel-games 1
                                :random-seed nil}
                        :started-at (java.util.Date.)
                        :completed-at (java.util.Date.)
                        :duration-minutes 15.5
                        :games []}
          summary (battle-controller/format-battle-summary battle-result)]
      
      (is (string? summary))
      (is (re-find #"=== Battle Results ===" summary))
      (is (re-find #"deck-a vs deck-b" summary))
      (is (re-find #"Games Played: 100" summary))
      (is (re-find #"deck-a Wins: 60 \(60.0%\)" summary))
      (is (re-find #"deck-b Wins: 35 \(35.0%\)" summary))
      (is (re-find #"Ties: 5" summary))
      (is (re-find #"Average Game Length: 15.5 minutes" summary))
      (is (re-find #"Average Turn Count: 25.3 turns" summary)))))

(deftest test-battle-statistics-calculation
  (testing "battle statistics are calculated correctly"
    (let [params {:deck1 sample-deck-1
                  :deck2 sample-deck-2
                  :num-games 5}
          result (battle-controller/simulate-battle-series! params)]
      
      ;; Verify win rates sum correctly (accounting for ties)
      (let [total-rate (+ (:deck1-win-rate result) (:deck2-win-rate result))
            expected-rate (/ (+ (:deck1-wins result) (:deck2-wins result)) 
                           (:total-games result))]
        (is (< (Math/abs (- total-rate expected-rate)) 0.001)))
      
      ;; Verify win counts
      (is (= (:deck1-wins result) 
             (count (filter #(= :player1 (:winner %)) (:games result)))))
      (is (= (:deck2-wins result) 
             (count (filter #(= :player2 (:winner %)) (:games result)))))
      (is (= (:ties result) 
             (count (filter #(nil? (:winner %)) (:games result))))))))

(deftest test-battle-config
  (testing "battle config is set correctly"
    (let [params {:deck1 sample-deck-1
                  :deck2 sample-deck-2
                  :num-games 2}
          result (battle-controller/simulate-battle-series! params)
          config (:config result)]
      
      (is (= (:id sample-deck-1) (:deck1-id config)))
      (is (= (:id sample-deck-2) (:deck2-id config)))
      (is (= 2 (:num-games config)))
      (is (= 30 (:timeout-minutes config)))
      (is (= 1 (:parallel-games config)))
      (is (nil? (:random-seed config))))))