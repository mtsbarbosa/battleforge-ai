(ns battleforge-ai.diplomat.cli-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [battleforge-ai.diplomat.cli :as cli]))

(deftest test-validate-args
  (testing "help flag returns help message"
    (let [result (cli/validate-args ["--help"])]
      (is (:ok? result))
      (is (:exit-message result))))
  
  (testing "empty args returns usage"
    (let [result (cli/validate-args [])]
      (is (not (:ok? result)))
      (is (:exit-message result))))
  
  (testing "valid command with args"
    (let [result (cli/validate-args ["battle" "--deck1" "deck1.json" "--deck2" "deck2.json"])]
      (is (= "battle" (:command result)))
      (is (some? (:command-args result)))
      (is (some? (:options result))))))

(deftest test-cli-options
  (testing "battle options parsing"
    (is (some? cli/battle-options)))
  
  (testing "simulate options parsing"
    (is (some? cli/simulate-options)))
  
  (testing "stats options parsing"
    (is (some? cli/stats-options)))
  
  (testing "fetch-deck options parsing"
    (is (some? cli/fetch-deck-options))))

(deftest test-usage-and-error-functions
  (testing "usage function generates help text"
    (let [usage-text (cli/usage "test summary")]
      (is (string? usage-text))
      (is (str/includes? usage-text "BattleForge AI"))))

  (testing "error-msg function formats errors"
    (let [error-text (cli/error-msg ["error1" "error2"])]
      (is (string? error-text))
      (is (str/includes? error-text "error1")))))

;; TODO: Add more comprehensive tests when functionality is implemented
(deftest test-placeholder-functions
  (testing "execute functions exist and don't crash"
    (is (some? (cli/execute-battle [] {})))
    (is (some? (cli/execute-simulate [] {})))
    (is (some? (cli/execute-stats [] {})))))

;; Note: execute-fetch-deck is more complex and involves controller calls,
;; so we'll test it separately or with mock controllers when needed