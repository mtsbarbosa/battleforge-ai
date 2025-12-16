(ns battleforge-ai.adapters.deck-storage-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schema.test :as schema-test]
            [clojure.java.io :as io]
            [java-time.api :as time]
            [battleforge-ai.adapters.deck-storage :as deck-adapter]
            [battleforge-ai.diplomat.file-storage :as file-diplomat]))

(use-fixtures :once schema-test/validate-schemas)

(def sample-deck
  {:id "test-deck-123"
   :name "Test Deck"
   :uuid "550e8400-e29b-41d4-a716-446655440000"
   :identity "test-deck"
   :houses [:brobnar :dis :logos]
   :cards [{:id "test-card"
            :name "Test Card"
            :house :brobnar
            :card-type :creature
            :amber 1
            :power 5
            :armor 0
            :rarity :common
            :card-text "A test card"
            :traits []
            :keywords []
            :expansion 341
            :number "001"
            :image nil
            :count 1}]
   :expansion 341
   :source :keyforge-api
   :power-level nil
   :sas-rating nil
   :chains nil
   :wins nil
   :losses nil
   :win-rate nil
   :usage-count nil
   :verified? nil
   :is-alliance? false
   :last-updated nil
   :fetched-at (time/instant)
   :total-power 5
   :total-amber 1
   :creature-count 1
   :action-count 0
   :artifact-count 0
   :upgrade-count 0})

(deftest test-safe-filename
  (testing "converts deck names to safe filenames"
    (is (= "test-deck" (deck-adapter/safe-filename "Test Deck")))
    (is (= "complex-deck-name" (deck-adapter/safe-filename "Complex: Deck! Name?")))
    (is (= "deck-with-spaces" (deck-adapter/safe-filename "Deck   with   spaces")))))

(deftest test-deck-serialization-adapter
  (testing "adapter can transform deck to/from JSON"
    (let [json-map (deck-adapter/deck->json-map sample-deck)
          restored-deck (deck-adapter/json-map->deck json-map)]
      
      (is (= (:name sample-deck) (:name restored-deck)))
      (is (= (:houses sample-deck) (:houses restored-deck)))
      (is (= (count (:cards sample-deck)) (count (:cards restored-deck)))))))

(deftest test-deck-file-operations
  (testing "diplomat can save and load deck files"
    (let [temp-dir "./test-decks"
          file-path (file-diplomat/save-deck! sample-deck temp-dir)
          loaded-deck (file-diplomat/load-deck file-path)]
      
      (is (.exists (io/file file-path)))
      (is (= (:name sample-deck) (:name loaded-deck)))
      (is (= (:houses sample-deck) (:houses loaded-deck)))
      (is (= (count (:cards sample-deck)) (count (:cards loaded-deck))))
      
      ; Cleanup
      (.delete (io/file file-path))
      (.delete (io/file temp-dir)))))

(deftest test-deck-exists
  (testing "deck existence check works"
    (let [temp-dir "./test-decks"]
      (is (not (file-diplomat/deck-exists? "Test Deck" temp-dir)))
      (file-diplomat/save-deck! sample-deck temp-dir)
      (is (file-diplomat/deck-exists? "Test Deck" temp-dir))
      
      ; Cleanup
      (file-diplomat/delete-deck! "Test Deck" temp-dir)
      (.delete (io/file temp-dir)))))