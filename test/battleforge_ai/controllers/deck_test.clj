(ns battleforge-ai.controllers.deck-test
  (:require [clojure.test :refer :all]
            [battleforge-ai.controllers.deck :as deck-controller]))

(deftest test-validate-fetch-deck-params!
  (testing "validates required parameters"
    (is (thrown-with-msg? 
          clojure.lang.ExceptionInfo 
          #"Must provide either deck ID or deck name"
          (deck-controller/validate-fetch-deck-params! {}))))
  
  (testing "validates keyforge source requires deck-id"
    (is (thrown-with-msg? 
          clojure.lang.ExceptionInfo 
          #"Deck ID required for Keyforge source"
          (deck-controller/validate-fetch-deck-params! 
            {:source "keyforge" :deck-name "test"}))))
  
  (testing "validates search source requires deck-name"
    (is (thrown-with-msg? 
          clojure.lang.ExceptionInfo 
          #"Deck name required for search source"
          (deck-controller/validate-fetch-deck-params! 
            {:source "search" :deck-id "123"}))))
  
  (testing "passes validation for valid params"
    (is (deck-controller/validate-fetch-deck-params! 
          {:source "keyforge" :deck-id "test-id"}))
    (is (deck-controller/validate-fetch-deck-params! 
          {:source "search" :deck-name "test-name"}))))

(deftest test-handle-deck-storage
  (testing "returns already-exists status when deck exists and no overwrite"
    (with-redefs [battleforge-ai.diplomat.file-storage/deck-exists? (constantly true)]
      (let [result (deck-controller/handle-deck-storage 
                     {:name "test-deck"} 
                     {:output-dir "test" :overwrite? false})]
        (is (= :already-exists (:status result)))
        (is (= "test-deck" (get-in result [:deck :name]))))))
  
  (testing "saves deck when overwrite is true"
    (with-redefs [battleforge-ai.diplomat.file-storage/deck-exists? (constantly true)
                  battleforge-ai.diplomat.file-storage/save-deck! (constantly "test/path")]
      (let [result (deck-controller/handle-deck-storage 
                     {:name "test-deck"} 
                     {:output-dir "test" :overwrite? true})]
        (is (= :saved (:status result)))
        (is (= "test/path" (:file-path result)))))))

(deftest test-placeholder-functions
  (testing "future controller functions return not-implemented status"
    (let [result (deck-controller/simulate-battle! {})]
      (is (= :not-implemented (:status result))))
    
    (let [result (deck-controller/run-mass-simulation! {})]
      (is (= :not-implemented (:status result))))
    
    (let [result (deck-controller/generate-statistics! {})]
      (is (= :not-implemented (:status result))))))

;; TODO: Add integration tests for fetch-deck! when we want to test 
;; the full orchestration with real diplomat calls