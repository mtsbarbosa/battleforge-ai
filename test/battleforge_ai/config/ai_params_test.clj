(ns battleforge-ai.config.ai-params-test
  (:require [clojure.test :refer [deftest is testing]]
            [battleforge-ai.config.ai-params :as ai-params]
            [schema.core :as s]))

(deftest test-default-params-schema
  (testing "default AI parameters match schema"
    (is (nil? (s/check ai-params/AIParams ai-params/default-ai-params)))))

(deftest test-get-ai-params-returns-valid-schema
  (testing "get-ai-params returns valid schema"
    (let [params (ai-params/get-ai-params)]
      (is (nil? (s/check ai-params/AIParams params))))))

(deftest test-threat-level-classification
  (testing "threat level classification with default thresholds"
    (let [moderate-threshold (:moderate-threat-threshold ai-params/default-ai-params)
          high-threshold (:high-threat-threshold ai-params/default-ai-params)]
      
      ;; Test low threat (below moderate threshold)
      (is (= false (ai-params/is-moderate-threat? (- moderate-threshold 0.1))))
      (is (= false (ai-params/is-high-threat? (- moderate-threshold 0.1))))
      
      ;; Test moderate threat (between moderate and high thresholds)
      (let [moderate-value (+ moderate-threshold 0.1)]
        (is (= true (ai-params/is-moderate-threat? moderate-value)))
        (is (= false (ai-params/is-high-threat? moderate-value))))
      
      ;; Test high threat (above high threshold)
      (let [high-value (+ high-threshold 0.1)]
        (is (= false (ai-params/is-moderate-threat? high-value)))
        (is (= true (ai-params/is-high-threat? high-value)))))))

(deftest test-advantage-thresholds
  (testing "advantage thresholds return correct values based on threat level"
    (let [params ai-params/default-ai-params
          low-threat 1.0
          moderate-threat 2.5  ; Between 2.0 and 3.0
          high-threat 4.0]     ; Above 3.0
      
      (is (= (:low-threat-advantage-threshold params)
             (ai-params/get-advantage-threshold low-threat)))
      (is (= (:moderate-threat-advantage-threshold params)
             (ai-params/get-advantage-threshold moderate-threat)))
      (is (= (:high-threat-advantage-threshold params)
             (ai-params/get-advantage-threshold high-threat))))))

(deftest test-delta-thresholds  
  (testing "delta thresholds return correct values based on threat level"
    (let [params ai-params/default-ai-params
          low-threat 1.0
          moderate-threat 2.5  ; Between 2.0 and 3.0
          high-threat 4.0]     ; Above 3.0
      
      (is (= (:low-threat-delta-threshold params)
             (ai-params/get-delta-threshold low-threat)))
      (is (= (:moderate-threat-delta-threshold params)
             (ai-params/get-delta-threshold moderate-threat)))
      (is (= (:high-threat-delta-threshold params)
             (ai-params/get-delta-threshold high-threat))))))

(deftest test-config-file-fallback
  (testing "config loading falls back to defaults gracefully"
    ;; This test verifies the config loading doesn't crash
    ;; The actual file loading is tested implicitly by other tests working
    (let [params (ai-params/load-config-file)]
      (is (map? params))
      (is (contains? params :default-creature-amber))
      (is (number? (:default-creature-amber params))))))

(deftest test-all-required-params-present
  (testing "all required parameters are present in defaults"
    (let [params ai-params/default-ai-params
          required-keys [:default-creature-amber
                        :opponent-hand-amber-estimate
                        :key-cost
                        :amber-control-action-with-pips
                        :amber-control-artifact
                        :amber-control-creature-with-pips
                        :amber-control-default
                        :moderate-threat-threshold
                        :high-threat-threshold
                        :opponent-potential-multiplier
                        :high-threat-advantage-threshold
                        :moderate-threat-advantage-threshold
                        :low-threat-advantage-threshold
                        :high-threat-delta-threshold
                        :moderate-threat-delta-threshold
                        :low-threat-delta-threshold
                        :min-cards-for-hand-advantage
                        :min-cards-for-house-switch
                        :battleline-advantage-vs-high-threat]]
      
      (doseq [key required-keys]
        (is (contains? params key) (str "Missing required key: " key))
        (is (number? (get params key)) (str "Key should be numeric: " key))))))