(ns battleforge-ai.adapters.deck-storage
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [schema.core :as s]
            [battleforge-ai.models.deck :as deck]
            [java-time :as time])
  (:import [java.io File]))

;; ============================================================================
;; File System Utilities
;; ============================================================================

(defn ensure-directory
  "Ensure directory exists, creating if necessary"
  [dir-path]
  (let [dir (io/file dir-path)]
    (when-not (.exists dir)
      (.mkdirs dir))
    dir))

(defn safe-filename
  "Convert deck name to safe filename"
  [deck-name]
  (let [cleaned (-> deck-name
                    clojure.string/lower-case
                    (clojure.string/replace #"[^a-zA-Z0-9\-_\s]" "")
                    (clojure.string/replace #"\s+" "-")
                    (clojure.string/replace #"-+" "-"))]
    (subs cleaned 0 (min 50 (count cleaned)))))

;; ============================================================================
;; JSON Serialization
;; ============================================================================

(defn- deck->json-map
  "Convert deck to JSON-serializable map"
  [deck]
  (-> deck
      (update :fetched-at str)
      (update :last-updated #(when % (str %)))
      (update :houses #(mapv name %))
      (update :source name)
      (update :cards 
              (fn [cards]
                (mapv (fn [card]
                        (-> card
                            (update :house name)
                            (update :card-type name)
                            (update :rarity name)
                            (update :maverick-house #(when % (name %)))
                            (update :anomaly-house #(when % (name %)))))
                      cards)))))

(defn- json-map->deck
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

;; ============================================================================
;; Storage Operations
;; ============================================================================

(s/defn save-deck! :- s/Str
  "Save deck to JSON file and return the file path"
  [deck :- deck/Deck
   output-dir :- s/Str]
  (let [dir (ensure-directory output-dir)
        filename (str (safe-filename (:name deck)) ".json")
        file-path (.getPath (io/file dir filename))
        json-data (deck->json-map deck)]
    (log/info "Saving deck to" file-path)
    (with-open [writer (io/writer file-path)]
      (json/generate-stream json-data writer {:pretty true}))
    (log/info "Deck saved successfully:" (:name deck))
    file-path))

(s/defn load-deck :- deck/Deck
  "Load deck from JSON file"
  [file-path :- s/Str]
  (log/info "Loading deck from" file-path)
  (let [file (io/file file-path)]
    (if (.exists file)
      (let [json-data (json/parse-string (slurp file-path) true)]
        (log/info "Deck loaded successfully:" (:name json-data))
        (json-map->deck json-data))
      (throw (ex-info "Deck file not found" {:file-path file-path})))))

(defn list-deck-files
  "List all JSON deck files in a directory"
  [dir-path]
  (let [dir (io/file dir-path)]
    (if (.exists dir)
      (->> (.listFiles dir)
           (filter #(.isFile %))
           (filter #(.endsWith (.getName %) ".json"))
           (mapv #(.getPath %)))
      [])))

(defn load-all-decks
  "Load all deck files from a directory"
  [dir-path]
  (log/info "Loading all decks from" dir-path)
  (let [deck-files (list-deck-files dir-path)]
    (log/info "Found" (count deck-files) "deck files")
    (mapv load-deck deck-files)))

;; ============================================================================
;; Deck Management
;; ============================================================================

(defn deck-exists?
  "Check if a deck file already exists"
  [deck-name output-dir]
  (let [filename (str (safe-filename deck-name) ".json")
        file-path (.getPath (io/file output-dir filename))]
    (.exists (io/file file-path))))

(defn delete-deck!
  "Delete a deck file"
  [deck-name output-dir]
  (let [filename (str (safe-filename deck-name) ".json")
        file-path (.getPath (io/file output-dir filename))
        file (io/file file-path)]
    (if (.exists file)
      (do
        (.delete file)
        (log/info "Deleted deck file:" file-path)
        true)
      (do
        (log/warn "Deck file not found:" file-path)
        false))))

(defn get-deck-info
  "Get basic information about stored decks without loading full data"
  [dir-path]
  (let [deck-files (list-deck-files dir-path)]
    (mapv (fn [file-path]
            (let [file (io/file file-path)
                  json-data (json/parse-string (slurp file-path) true)]
              {:file-path file-path
               :name (:name json-data)
               :uuid (:uuid json-data)
               :houses (:houses json-data)
               :source (:source json-data)
               :expansion (:expansion json-data)
               :fetched-at (:fetched-at json-data)
               :file-size (.length file)
               :last-modified (time/instant (.lastModified file))}))
          deck-files)))