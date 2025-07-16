(ns battleforge-ai.core-test
  (:require [clojure.test :refer :all]
            [battleforge-ai.core :as core]))

(deftest test-validate-args
  (testing "help flag returns help message"
    (let [result (core/validate-args ["--help"])]
      (is (:ok? result))
      (is (:exit-message result))))
  
  (testing "empty args returns usage"
    (let [result (core/validate-args [])]
      (is (not (:ok? result)))
      (is (:exit-message result))))
  
  (testing "valid command with args"
    (let [result (core/validate-args ["battle" "--deck1" "deck1.json" "--deck2" "deck2.json"])]
      (is (= "battle" (:command result)))
      (is (some? (:command-args result)))
      (is (some? (:options result))))))

(deftest test-cli-options
  (testing "battle options parsing"
    (is (some? core/battle-options)))
  
  (testing "simulate options parsing"
    (is (some? core/simulate-options)))
  
  (testing "stats options parsing"
    (is (some? core/stats-options))))

;; TODO: Add more comprehensive tests when functionality is implemented
(deftest test-placeholder-functions
  (testing "execute functions exist and don't crash"
    (is (some? (core/execute-battle [] {})))
    (is (some? (core/execute-simulate [] {})))
    (is (some? (core/execute-stats [] {}))))) 