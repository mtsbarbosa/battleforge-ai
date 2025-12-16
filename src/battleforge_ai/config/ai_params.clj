(ns battleforge-ai.config.ai-params
  (:require [schema.core :as s]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(s/defschema AIParams
  {:default-creature-amber s/Num
   :opponent-hand-amber-estimate s/Num
   :key-cost s/Num
   :amber-control-action-with-pips s/Num
   :amber-control-artifact s/Num  
   :amber-control-creature-with-pips s/Num
   :amber-control-default s/Num
   :moderate-threat-threshold s/Num
   :high-threat-threshold s/Num
   :opponent-potential-multiplier s/Num
   :high-threat-advantage-threshold s/Num
   :moderate-threat-advantage-threshold s/Num
   :low-threat-advantage-threshold s/Num
   :high-threat-delta-threshold s/Num
   :moderate-threat-delta-threshold s/Num  
   :low-threat-delta-threshold s/Num
   :min-cards-for-hand-advantage s/Num
   :min-cards-for-house-switch s/Num
   :battleline-advantage-vs-high-threat s/Num
   :fight-min-delta-swing s/Num
   :fight-forge-threat-amber s/Num
   :fight-forge-threat-bonus s/Num
   :reap-amber-to-forge-threshold s/Num
   :fight-min-power-ratio s/Num})

(def default-ai-params
  {:default-creature-amber 1.0
   :opponent-hand-amber-estimate 2.0
   :key-cost 6
   :amber-control-action-with-pips 1.0
   :amber-control-artifact 0.5
   :amber-control-creature-with-pips 0.5
   :amber-control-default 0.0
   :moderate-threat-threshold 2.0
   :high-threat-threshold 3.0
   :opponent-potential-multiplier 1.5
   :high-threat-advantage-threshold 1.5
   :moderate-threat-advantage-threshold 1.2
   :low-threat-advantage-threshold 1.0
   :high-threat-delta-threshold 4.0
   :moderate-threat-delta-threshold 3.5
   :low-threat-delta-threshold 3.0
   :min-cards-for-hand-advantage 2
   :min-cards-for-house-switch 3
   :battleline-advantage-vs-high-threat 2.0
   :fight-min-delta-swing 0.5
   :fight-forge-threat-amber 4
   :fight-forge-threat-bonus 1.5
   :reap-amber-to-forge-threshold 3
   :fight-min-power-ratio 0.5})

(defn load-config-file []
  (try
    (let [config-path "config/ai-tuning.edn"]
      (if (.exists (io/file config-path))
        (edn/read-string (slurp config-path))
        default-ai-params))
    (catch Exception _
      default-ai-params)))

(def ^:private cached-ai-params (delay (load-config-file)))

(s/defn get-ai-params :- AIParams []
  @cached-ai-params)

(defn reload-ai-params! []
  (alter-var-root #'cached-ai-params (constantly (delay (load-config-file))))
  @cached-ai-params)

(s/defn is-moderate-threat? :- s/Bool
  [threat-level :- s/Num]
  (let [params (get-ai-params)]
    (and (> threat-level (:moderate-threat-threshold params))
         (<= threat-level (:high-threat-threshold params)))))

(s/defn is-high-threat? :- s/Bool
  [threat-level :- s/Num] 
  (let [params (get-ai-params)]
    (> threat-level (:high-threat-threshold params))))

(s/defn get-advantage-threshold :- s/Num
  [threat-level :- s/Num]
  (let [params (get-ai-params)]
    (cond
      (is-high-threat? threat-level) (:high-threat-advantage-threshold params)
      (is-moderate-threat? threat-level) (:moderate-threat-advantage-threshold params)
      :else (:low-threat-advantage-threshold params))))

(s/defn get-delta-threshold :- s/Num
  [threat-level :- s/Num]
  (let [params (get-ai-params)]
    (cond
      (is-high-threat? threat-level) (:high-threat-delta-threshold params)
      (is-moderate-threat? threat-level) (:moderate-threat-delta-threshold params)
      :else (:low-threat-delta-threshold params))))