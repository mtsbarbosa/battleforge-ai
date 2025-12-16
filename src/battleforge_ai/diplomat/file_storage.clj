(ns battleforge-ai.diplomat.file-storage
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [schema.core :as s]
            [battleforge-ai.models.deck :as deck]
            [battleforge-ai.adapters.deck-storage :as deck-adapter]
            [java-time.api :as time]))

(defn- ensure-directory
  [dir-path]
  (let [dir (io/file dir-path)]
    (when-not (.exists dir)
      (.mkdirs dir))
    dir))

(defn- write-json-file!
  "Write JSON data to file"
  [file-path json-data]
  (log/info "Writing JSON file to" file-path)
  (with-open [writer (io/writer file-path)]
    (json/generate-stream json-data writer {:pretty true}))
  file-path)

(defn- read-json-file
  "Read JSON data from file"
  [file-path]
  (log/info "Reading JSON file from" file-path)
  (let [file (io/file file-path)]
    (if (.exists file)
      (json/parse-string (slurp file-path) true)
      (throw (ex-info "File not found" {:file-path file-path})))))

(defn- file-exists?
  "Check if file exists"
  [file-path]
  (.exists (io/file file-path)))

(defn- delete-file!
  "Delete a file"
  [file-path]
  (let [file (io/file file-path)]
    (if (.exists file)
      (do
        (.delete file)
        (log/info "Deleted file:" file-path)
        true)
      (do
        (log/warn "File not found:" file-path)
        false))))

(defn- list-json-files
  "List all JSON files in a directory"
  [dir-path]
  (let [dir (io/file dir-path)]
    (if (.exists dir)
      (->> (.listFiles dir)
           (filter #(.isFile %))
           (filter #(.endsWith (.getName %) ".json"))
           (mapv #(.getPath %)))
      [])))

(defn- get-file-metadata
  "Get file metadata (size, last modified)"
  [file-path]
  (let [file (io/file file-path)]
    (when (.exists file)
      {:file-size (.length file)
       :last-modified (.lastModified file)})))

;; ============================================================================
;; Public Diplomat API (Called by Controllers)
;; ============================================================================

(s/defn save-deck! :- s/Str
  "Save deck to JSON file and return the file path"
  [deck :- deck/Deck
   output-dir :- s/Str]
  (ensure-directory output-dir)
  (let [filename (str (deck-adapter/safe-filename (:name deck)) ".json")
        file-path (str output-dir "/" filename)
        json-data (deck-adapter/deck->json-map deck)]
    (write-json-file! file-path json-data)
    (log/info "Deck saved successfully:" (:name deck))
    file-path))

(s/defn load-deck :- deck/Deck
  "Load deck from JSON file"
  [file-path :- s/Str]
  (let [json-data (read-json-file file-path)
        deck (deck-adapter/json-map->deck json-data)]
    (log/info "Deck loaded successfully:" (:name deck))
    deck))

(defn deck-exists?
  "Check if a deck file already exists"
  [deck-name output-dir]
  (let [filename (str (deck-adapter/safe-filename deck-name) ".json")
        file-path (str output-dir "/" filename)]
    (file-exists? file-path)))

(defn delete-deck!
  "Delete a deck file"
  [deck-name output-dir]
  (let [filename (str (deck-adapter/safe-filename deck-name) ".json")
        file-path (str output-dir "/" filename)]
    (delete-file! file-path)))

(defn list-deck-files
  "List all JSON deck files in a directory"
  [dir-path]
  (list-json-files dir-path))

(defn load-all-decks
  "Load all deck files from a directory"
  [dir-path]
  (log/info "Loading all decks from" dir-path)
  (let [deck-files (list-deck-files dir-path)]
    (log/info "Found" (count deck-files) "deck files")
    (mapv load-deck deck-files)))

(defn get-deck-info
  "Get basic information about stored decks without loading full data"
  [dir-path]
  (let [deck-files (list-deck-files dir-path)]
    (mapv (fn [file-path]
            (let [json-data (read-json-file file-path)
                  metadata (get-file-metadata file-path)]
              (merge
                {:file-path file-path
                 :name (:name json-data)
                 :uuid (:uuid json-data)
                 :houses (:houses json-data)
                 :source (:source json-data)
                 :expansion (:expansion json-data)
                 :fetched-at (:fetched-at json-data)}
                metadata
                {:last-modified (time/instant (:last-modified metadata))})))
          deck-files)))