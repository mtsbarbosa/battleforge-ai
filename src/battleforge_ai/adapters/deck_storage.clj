(ns battleforge-ai.adapters.deck-storage
  (:require [battleforge-ai.models.deck :as deck]
            [java-time :as time]))

;; ============================================================================
;; Data Transformation Functions (Pure Adapters)
;; ============================================================================

(defn safe-filename
  "Convert deck name to safe filename"
  [deck-name]
  (let [cleaned (-> deck-name
                    clojure.string/lower-case
                    (clojure.string/replace #"[^a-zA-Z0-9\-_\s]" "")
                    (clojure.string/replace #"\s+" "-")
                    (clojure.string/replace #"-+" "-"))]
    (subs cleaned 0 (min 50 (count cleaned)))))

(defn deck->json-map
  "Convert deck to JSON-serializable map"
  [deck]
  (-> deck
      (update :fetched-at str)
      (update :last-updated #(when % (str %)))
      (update :houses #(mapv name %))
      (update :source name)
      ;; SAS rating is already a map of numbers/nils, no conversion needed
      (update :cards 
              (fn [cards]
                (mapv (fn [card]
                        (-> card
                            (update :house name)
                            (update :card-type #(when % (name %)))
                            (update :rarity #(when % (name %)))
                            (update :maverick-house #(when % (name %)))
                            (update :anomaly-house #(when % (name %)))))
                      cards)))))

(defn json-map->deck
  "Convert JSON map back to deck"
  [json-map]
  (-> json-map
      (update :fetched-at time/instant)
      (update :last-updated #(when % (time/instant %)))
      (update :houses #(mapv keyword %))
      (update :source keyword)
      (update :cards 
              (fn [cards]
                (mapv (fn [card]
                        (cond-> card
                          true (update :house keyword)
                          true (update :card-type keyword)
                          true (update :rarity keyword)
                          (:maverick-house card) (update :maverick-house keyword)
                          (:anomaly-house card) (update :anomaly-house keyword)
                          (nil? (:maverick-house card)) (dissoc :maverick-house)
                          (nil? (:anomaly-house card)) (dissoc :anomaly-house)))
                      cards)))))