(ns battleforge-ai.logic.key-forging-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schema.test :as schema-test]
            [battleforge-ai.logic.key-forging :as key-forging]))

(use-fixtures :once schema-test/validate-schemas)

(defn create-test-player
  "Create a test player with all required fields"
  [amber keys]
  {:id "test-player",
   :deck [],
   :hand [],
   :discard [],
   :purged [],
   :archive [],
   :battleline [],
   :artifacts [],
   :houses [:brobnar :dis :logos],
   :amber amber,
   :keys keys,
   :chains 0,
   :ready-amber 0})

(deftest test-get-current-key-cost
  (testing "get-current-key-cost returns correct cost"
    (let [player (create-test-player 10 1)]
      (is (= 6 (key-forging/get-current-key-cost player))))))

(deftest test-can-forge-key-sufficient-amber
  (testing
    "can-forge-key? returns true when player has enough amber and less than 3 keys"
    (let [player (create-test-player 6 0)]
      (is (true? (key-forging/can-forge-key? player))))
    (let [player (create-test-player 10 2)]
      (is (true? (key-forging/can-forge-key? player))))))

(deftest test-can-forge-key-insufficient-amber
  (testing "can-forge-key? returns false when player doesn't have enough amber"
    (let [player (create-test-player 5 0)]
      (is (false? (key-forging/can-forge-key? player))))
    (let [player (create-test-player 0 1)]
      (is (false? (key-forging/can-forge-key? player))))))

(deftest test-can-forge-key-max-keys
  (testing "can-forge-key? returns false when player already has 3 keys"
    (let [player (create-test-player 20 3)]
      (is (false? (key-forging/can-forge-key? player))))))

(deftest test-forge-key-success
  (testing "forge-key successfully forges a key when conditions are met"
    (let [player (create-test-player 8 1)
          result (key-forging/forge-key player)]
      (is (= 2 (:keys result)))  ; Keys incremented
      (is (= 2 (:amber result))) ; Amber reduced by 6
      (is (= (:id player) (:id result)))
      (is (= (:deck player) (:deck result)))
      (is (= (:hand player) (:hand result))))))

(deftest test-forge-key-exact-amount
  (testing "forge-key works with exact amber amount"
    (let [player (create-test-player 6 0)
          result (key-forging/forge-key player)]
      (is (= 1 (:keys result)))
      (is (= 0 (:amber result))))))

(deftest test-forge-key-insufficient-amber
  (testing "forge-key returns unchanged player when insufficient amber"
    (let [player (create-test-player 5 0)
          result (key-forging/forge-key player)]
      (is (= player result)))))

(deftest test-forge-key-max-keys
  (testing "forge-key returns unchanged player when already at max keys"
    ;; Lots of amber but max keys
    (let [player (create-test-player 20 3)
          result (key-forging/forge-key player)]
      (is (= player result)))))

(deftest test-process-key-forging-ai-forges
  (testing "process-key-forging forges when AI decides to"
    (let [player (create-test-player 8 0)
          result (key-forging/process-key-forging player)]
      (is (= 1 (:keys result)))
      (is (= 2 (:amber result))))))

(deftest test-process-key-forging-ai-doesnt-forge
  (testing "process-key-forging doesn't forge when AI decides not to"
    (let [player (create-test-player 5 0)
          result (key-forging/process-key-forging player)]
      (is (= player result)))))

(deftest test-key-forging-preserves-other-fields
  (testing "key forging preserves other player fields"
    (let [player (-> (create-test-player 10 1)
                     (assoc :chains 2)
                     (assoc :ready-amber 3))
          result (key-forging/forge-key player)]
      (is (= (:id player) (:id result)))
      (is (= (:chains player) (:chains result)))
      (is (= (:ready-amber player) (:ready-amber result)))
      (is (= (:houses player) (:houses result)))
      (is (= (:deck player) (:deck result)))
      (is (= (:hand player) (:hand result)))
      (is (= (:discard player) (:discard result)))
      (is (= (:purged player) (:purged result)))
      (is (= (:archive player) (:archive result)))
      (is (= (:battleline player) (:battleline result)))
      (is (= (:artifacts player) (:artifacts result))))))