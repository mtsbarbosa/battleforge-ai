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

(defn- parse-datetime
  "Parse a datetime string, handling both full instants and date-only formats"
  [s]
  (when s
    (try
      (time/instant s)
      (catch Exception _
        (try
          (-> (time/local-date s)
              (time/local-date-time 0 0 0)
              (time/zoned-date-time "UTC")
              time/instant)
          (catch Exception _ (time/instant)))))))

(defn json-map->deck
  [json-map]
  (-> json-map
      (update :fetched-at #(or (parse-datetime %) (time/instant)))
      (update :last-updated parse-datetime)
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
                          true (update :card-type #(if % (keyword %) :action))
                          true (update :rarity #(if % (keyword %) :common))
                          (nil? (:number card)) (assoc :number "000")
                          (nil? (:expansion card)) (assoc :expansion 1)
                          (nil? (:image card)) (assoc :image "")
                          (:enhancements card) (update :enhancements
                                                       (fn [enhancements]
                                                         (mapv #(update % :type keyword) enhancements)))
                          (:maverick-house card) (update :maverick-house keyword)
                          (:anomaly-house card) (update :anomaly-house keyword)
                          (nil? (:maverick-house card)) (dissoc :maverick-house)
                          (nil? (:anomaly-house card)) (dissoc :anomaly-house)))
                      cards)))))