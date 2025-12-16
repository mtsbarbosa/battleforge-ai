(ns battleforge-ai.controllers.deck
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [battleforge-ai.diplomat.http-out.keyforge-api :as keyforge-diplomat]
    [battleforge-ai.diplomat.http-out.decks-of-keyforge-api :as dok-diplomat]
    [battleforge-ai.diplomat.file-storage :as file-diplomat]))

(defn validate-fetch-deck-params!
  "Validate parameters for deck fetching operation"
  [{:keys [deck-id deck-name source]}]
  (cond (and (not deck-id) (not deck-name))
          (throw (ex-info "Must provide either deck ID or deck name"
                          {:type :validation-error}))
        (and (= source "keyforge") (not deck-id))
          (throw (ex-info "Deck ID required for Keyforge source"
                          {:type :validation-error}))
        (and (= source "search") (not deck-name))
          (throw (ex-info "Deck name required for search source"
                          {:type :validation-error}))
        :else true))

(defn fetch-deck-from-source
  "Fetch deck from the specified source"
  [{:keys [deck-id deck-name source]}]
  (log/info "Fetching deck from source:" source)
  (case source
    "keyforge" (keyforge-diplomat/fetch-deck! deck-id)
    "dok" (dok-diplomat/fetch-deck! deck-id)
    "search" (dok-diplomat/search-and-fetch-first! deck-name)
    (throw (ex-info "Invalid source"
                    {:source source, :type :validation-error}))))

(defn handle-deck-storage
  "Handle deck storage logic with overwrite checks"
  [deck {:keys [output-dir overwrite?]}]
  (let [existing? (file-diplomat/deck-exists? (:name deck) output-dir)]
    (if (and existing? (not overwrite?))
      {:status :already-exists,
       :message "Deck already exists. Use --overwrite to replace it.",
       :deck deck}
      (let [file-path (file-diplomat/save-deck! deck output-dir)]
        {:status :saved,
         :message "Deck saved successfully!",
         :file-path file-path,
         :deck deck}))))

(defn format-success-output
  "Format success output for deck fetch operation"
  [{:keys [status message file-path deck]}]
  (case status
    :already-exists (println "⚠️ " message)
    :saved (do (println "✅" message)
               (println "📁 File:" file-path)
               (println "🎴 Name:" (:name deck))
               (println "🏠 Houses:" (str/join ", " (map name (:houses deck))))
               (println "📦 Expansion:" (:expansion deck))
               (println "🃏 Cards:" (count (:cards deck)))
               (when-let [sas-rating (get-in deck [:sas-rating :sas-rating])]
                 (println "📊 SAS Rating:" sas-rating))
               (when (:power-level deck)
                 (println "⚡ Power Level:" (:power-level deck))))))

(defn fetch-deck!
  "Orchestrate deck fetching operation: validate, fetch, store, report"
  [params]
  (log/info "Starting deck fetch operation" params)
  (try
    ;; Step 1: Validate parameters
    (validate-fetch-deck-params! params)
    ;; Step 2: Fetch deck from source
    (let [deck (fetch-deck-from-source params)]
      (log/info "Successfully fetched deck:" (:name deck))
      ;; Step 3: Handle storage
      (let [storage-result (handle-deck-storage deck params)]
        ;; Step 4: Format and display output
        (format-success-output storage-result)
        ;; Return result for potential programmatic use
        storage-result))
    (catch Exception e
      (log/error e "Error in deck fetch operation")
      (throw e))))

;; Future controller functions for other operations
(defn simulate-battle!
  "Orchestrate battle simulation between two decks"
  [params]
  (log/info "Battle simulation not yet implemented" params)
  {:status :not-implemented, :message "Battle simulation feature coming soon"})

(defn run-mass-simulation!
  "Orchestrate mass simulation with multiple decks"
  [params]
  (log/info "Mass simulation not yet implemented" params)
  {:status :not-implemented, :message "Mass simulation feature coming soon"})

(defn generate-statistics!
  "Orchestrate statistics generation from simulation results"
  [params]
  (log/info "Statistics generation not yet implemented" params)
  {:status :not-implemented,
   :message "Statistics generation feature coming soon"})