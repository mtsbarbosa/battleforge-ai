(ns battleforge-ai.adapters.deck-storage
  (:require [clojure.string :as str]
            [java-time.api :as time]))

;; ============================================================================
;; Data Transformation Functions (Pure Adapters)
;; ============================================================================

(defn safe-filename
  "Convert deck name to safe filename"
  [deck-name]
  (let [cleaned (-> deck-name
                    str/lower-case
                    (str/replace #"[^a-zA-Z0-9\-_\s]" "")
                    (str/replace #"\s+" "-")
                    (str/replace #"-+" "-"))]
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
      ;; Handle date fields
      (update :fetched-at #(if % (time/instant %) (time/instant)))
      (update :last-updated #(when % (time/instant %)))
      ;; Provide default values for missing fields
      (update :win-rate #(or % 0.0))
      (update :sas-rating #(or % {}))  ; SAS rating should be a map
      (update :is-alliance? #(or % false))
      (update :usage-count #(or % 0))
      (update :verified? #(or % false))
      (update :identity #(or % (:name json-map)))  ; Use name as identity if missing
      (update :uuid #(or % (str (java.util.UUID/randomUUID))))  ; Generate UUID if missing
      ;; Convert fields to proper types
      (update :houses #(mapv keyword %))
      (update :source #(if % (keyword %) :manual))  ; Default to :manual if nil
      (update :expansion #(if (string? %) 1 %))  ; Convert string to number, default to 1
      (update :cards 
              (fn [cards]
                (mapv (fn [card]
                        (cond-> card
                          true (update :house keyword)
                          true (update :card-type keyword)
                          true (update :rarity keyword)
                          ;; Provide defaults for missing required fields
                          (nil? (:number card)) (assoc :number "000")
                          (nil? (:expansion card)) (assoc :expansion 1)  ; Default card expansion to 1
                          (nil? (:image card)) (assoc :image "")
                          (:maverick-house card) (update :maverick-house keyword)
                          (:anomaly-house card) (update :anomaly-house keyword)
                          (nil? (:maverick-house card)) (dissoc :maverick-house)
                          (nil? (:anomaly-house card)) (dissoc :anomaly-house)))
                      cards)))))