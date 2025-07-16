(ns battleforge-ai.adapters.deck-storage-test
  (:require [clojure.test :refer :all]
            [schema.test :as schema-test]
            [clojure.java.io :as io]
            [java-time :as time]
            [battleforge-ai.adapters.deck-storage :as storage]
            [battleforge-ai.models.deck :as deck]))

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
    (is (= "test-deck" (storage/safe-filename "Test Deck")))
    (is (= "complex-deck-name" (storage/safe-filename "Complex: Deck! Name?")))
    (is (= "deck-with-spaces" (storage/safe-filename "Deck   with   spaces")))))

(deftest test-ensure-directory
  (testing "creates directory if it doesn't exist"
    (let [test-dir "./test-temp-dir"]
      (storage/ensure-directory test-dir)
      (is (.exists (io/file test-dir)))
      (.delete (io/file test-dir)))))

(deftest test-deck-serialization
  (testing "deck can be saved and loaded"
    (let [temp-dir "./test-decks"
          file-path (storage/save-deck! sample-deck temp-dir)
          loaded-deck (storage/load-deck file-path)]
      
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
      (is (not (storage/deck-exists? "Test Deck" temp-dir)))
      (storage/save-deck! sample-deck temp-dir)
      (is (storage/deck-exists? "Test Deck" temp-dir))
      
      ; Cleanup
      (storage/delete-deck! "Test Deck" temp-dir)
      (.delete (io/file temp-dir)))))