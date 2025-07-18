(ns battleforge-ai.adapters.decks-of-keyforge-api
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [schema.core :as s]
            [battleforge-ai.models.deck :as deck]
            [java-time :as time]))

;; ============================================================================
;; API Configuration  
;; ============================================================================

(def ^:private api-base-url "https://decksofkeyforge.com/public-api")
(def ^:private api-version "v3")
(def ^:private request-timeout 30000) ; 30 seconds
(def ^:private rate-limit-delay 1000) ; 1 second between requests

;; API key management
(def ^:private ^:dynamic *api-key* nil)

(defn set-api-key!
  "Set the DoK API key"
  [key]
  (alter-var-root #'*api-key* (constantly key))
  (log/info "DoK API key configured"))

(defn get-api-key
  "Get current API key"
  []
  *api-key*)

(defn load-config-api-key!
  "Load API key from environment variable or config.edn file"
  ([]
   (load-config-api-key! "config.edn"))
  ([config-path]
   (try
     ;; First try environment variable
     (if-let [env-key (System/getenv "DOK_API_KEY")]
       (do
         (set-api-key! env-key)
         (log/info "Loaded DoK API key from DOK_API_KEY environment variable"))
       ;; Fall back to config file
       (when (.exists (io/file config-path))
         (let [config (edn/read-string (slurp config-path))
               api-key (get-in config [:decks-of-keyforge :api-key])]
           (when api-key
             (set-api-key! api-key)
             (log/info "Loaded DoK API key from configuration file")))))
     (catch Exception e
       (log/warn "Could not load configuration from" config-path ":" (.getMessage e))))))

;; Initialize API key from config on namespace load
(load-config-api-key!)

;; ============================================================================
;; HTTP Utilities
;; ============================================================================

(defn- make-request
  "Make HTTP request with error handling and rate limiting"
  [url options]
  (Thread/sleep rate-limit-delay) ; Simple rate limiting
  (try
    (let [api-key (get-api-key)
          headers (cond-> {:accept "application/json"}
                    api-key (assoc "Api-Key" api-key))]
      (log/debug "Making DoK API request to:" url)
      (if api-key
        (log/debug "Using API key:" (str (subs api-key 0 8) "..."))
        (log/debug "No API key configured"))
      (let [response (http/get url (merge {:timeout request-timeout
                                           :headers headers
                                           :as :json} 
                                          options))]
        (:body response)))
    (catch Exception e
      (log/error e "Failed to make request to" url)
      (throw (ex-info "API request failed" 
                      {:url url :error (.getMessage e)})))))

;; ============================================================================
;; Card Transformation
;; ============================================================================

(defn- parse-keywords
  "Extract keywords from card text"
  [card-text]
  (if (nil? card-text)
    []
    (let [valid-keywords #{:elusive :skirmish :taunt :deploy :alpha :omega 
                           :hazardous :assault :poison :splash-attack}
          lines (clojure.string/split card-text #"[\r\n]")
          potential-keywords (mapcat #(clojure.string/split % #"\.") lines)
          normalized-keywords (map #(-> % 
                                        clojure.string/lower-case 
                                        clojure.string/trim
                                        (clojure.string/replace #"\s+" "-")
                                        keyword) 
                                   potential-keywords)]
      (->> normalized-keywords
           (filter valid-keywords)
           (into [])))))

(defn- transform-dok-card
  "Transform Decks of Keyforge API card to our internal format"
  [api-card house sas-data]
  (let [card-id (-> (:cardTitle api-card)
                    clojure.string/lower-case
                    (clojure.string/replace #"[?.!,]" "")
                    (clojure.string/replace #"[\s']" "-"))
        enhanced? (:enhanced api-card)]
    {:id card-id
     :name (:cardTitle api-card)
     :house (deck/normalize-house-name house)
     :card-type (when (:cardType api-card)
                  (deck/normalize-card-type (:cardType api-card)))
     :amber (or (:amber api-card) (:bonusAember api-card) 0)
     :power (:power api-card)
     :armor (:armor api-card)
     :rarity (when (:rarity api-card)
               (deck/normalize-rarity (:rarity api-card)))
     :card-text (:cardText api-card)
     :traits (or (:traits api-card) [])
     :keywords (parse-keywords (:cardText api-card))
     :expansion (:expansion api-card)
     :number (:cardNumber api-card)
     :image (or (:frontImage api-card) (:cardTitleUrl api-card))
     :count (or (:copies sas-data) 1)
     :enhanced? enhanced?
     :maverick? (:maverick api-card)
     :maverick-house (when (:maverick api-card)
                       (deck/normalize-house-name house))
     :anomaly? (:anomaly api-card)
     :anomaly-house (when (:anomaly api-card)
                      (deck/normalize-house-name house))
     ;; Enhancement data from bonuses
     :enhancements (when enhanced?
                     (cond-> []
                       (> (or (:bonusAember api-card) 0) 0) (conj {:type :amber :value (:bonusAember api-card)})
                       (> (or (:bonusCapture api-card) 0) 0) (conj {:type :capture :value (:bonusCapture api-card)})
                       (> (or (:bonusDamage api-card) 0) 0) (conj {:type :damage :value (:bonusDamage api-card)})
                       (> (or (:bonusDraw api-card) 0) 0) (conj {:type :draw :value (:bonusDraw api-card)})
                       (> (or (:bonusDiscard api-card) 0) 0) (conj {:type :discard :value (:bonusDiscard api-card)})))
     ;; SAS scoring fields from synergyDetails
     :aerc-score (:aercScore sas-data)
     :expected-amber (:expectedAmber sas-data)
     :amber-control (:amberControl sas-data)
     :creature-control (:creatureControl sas-data)
     :artifact-control (:artifactControl sas-data)
     :efficiency (:efficiency sas-data)
     :recursion (:recursion sas-data)
     :effective-power (:effectivePower sas-data)
     :creature-protection (:creatureProtection sas-data)
     :disruption (:disruption sas-data)
     :other (:other sas-data)
     :net-synergy (:netSynergy sas-data)
     :synergies (:synergies sas-data)
     :copies (:copies sas-data)}))

;; ============================================================================
;; Deck Fetching (v3 API)
;; ============================================================================

(defn fetch-deck-v3
  "Fetch a deck using the v3 API with SAS ratings"
  [deck-uuid]
  (log/info "Fetching deck from DoK v3 API:" deck-uuid)
  (let [url (str api-base-url "/" api-version "/decks/" deck-uuid)]
    (if (get-api-key)
      (do
        (log/debug "Using authenticated DoK v3 API")
        (make-request url {}))
      (do
        (log/warn "No DoK API key configured - v3 API requires authentication")
        (throw (ex-info "DoK API key required for v3 API" 
                        {:deck-uuid deck-uuid}))))))

;; ============================================================================
;; Legacy Deck Search and Fetching (DEPRECATED - old API structure)
;; ============================================================================

(defn search-decks
  "Search for decks by name or other criteria (DEPRECATED - uses old API)"
  [query & {:keys [page page-size] :or {page 1 page-size 20}}]
  (log/warn "Using deprecated search API - will likely fail")
  (log/info "Searching decks on Decks of Keyforge:" query)
  (let [url "https://decksofkeyforge.com/api/decks"
        response (make-request url {:query-params {:search query
                                                   :page page
                                                   :page_size page-size}})]
    (log/debug "Found" (count (:data response)) "decks for query:" query)
    (:data response)))

(defn fetch-deck-by-keyforge-id
  "Fetch a deck from Decks of Keyforge API using Keyforge UUID (DEPRECATED - uses old API)"
  [keyforge-id]
  (log/warn "Using deprecated DoK API - will likely fail. Consider using v3 API with API key.")
  (log/info "Fetching deck from Decks of Keyforge by Keyforge ID:" keyforge-id)
  (let [url (str "https://decksofkeyforge.com/api/decks/" keyforge-id)
        response (make-request url {})]
    (log/debug "Received deck response for Keyforge ID" keyforge-id)
    response))

(defn fetch-deck-by-dok-id
  "Fetch a deck from Decks of Keyforge API using their internal ID (DEPRECATED - uses old API)"
  [dok-id]
  (log/warn "Using deprecated DoK API - will likely fail. Consider using v3 API with API key.")
  (log/info "Fetching deck from Decks of Keyforge by DoK ID:" dok-id)
  (let [url (str "https://decksofkeyforge.com/api/decks/" dok-id)
        response (make-request url {})]
    (log/debug "Received deck response for DoK ID" dok-id)
    response))

;; ============================================================================
;; Deck Transformation
;; ============================================================================

(defn- transform-dok-deck
  "Transform Decks of Keyforge API deck response to our internal format"
  [api-response]
  (let [deck-data api-response
        cards (map (fn [card] 
                     (transform-dok-card card (:house card) {})) 
                   (:cards deck-data))
        houses (mapv deck/normalize-house-name (:houses deck-data))]
    {:id (str (:id deck-data))
     :name (:name deck-data)
     :uuid (:keyforge_id deck-data)
     :identity (-> (:name deck-data)
                   clojure.string/lower-case
                   (clojure.string/replace #"[?.!,]" "")
                   (clojure.string/replace #"[\s']" "-"))
     :houses houses
     :cards cards
     :expansion (:expansion deck-data)
     :source :decks-of-keyforge
     :power-level (:power_level deck-data)
     :sas-rating (when-let [sas-data (:sas_rating deck-data)]
                   {:sas-rating (:sas_rating sas-data)
                    :amber (:amber sas-data)
                    :expected-amber (:expected_amber sas-data)
                    :artifact-control (:artifact_control sas-data)
                    :creature-control (:creature_control sas-data)
                    :efficiency (:efficiency sas-data)
                    :recursion (:recursion sas-data)
                    :creature-protection (:creature_protection sas-data)
                    :disruption (:disruption sas-data)
                    :other (:other sas-data)
                    :effective-power (:effective_power sas-data)
                    :raw-amber (:raw_amber sas-data)
                    :synergy-rating (:synergy_rating sas-data)
                    :antisynergy-rating (:antisynergy_rating sas-data)})
     :chains (:chains deck-data)
     :wins (:wins deck-data)
     :losses (:losses deck-data)
     :win-rate (when (and (:wins deck-data) (:losses deck-data))
                 (let [total-games (+ (:wins deck-data) (:losses deck-data))]
                   (if (> total-games 0)
                     (/ (:wins deck-data) total-games)
                     0.0)))
     :usage-count nil
     :verified? nil
     :is-alliance? false
     :last-updated (when (:last_update deck-data)
                     (time/instant (:last_update deck-data)))
     :fetched-at (time/instant)
     :total-power (deck/calculate-deck-power {:cards cards})
     :total-amber (deck/calculate-total-amber {:cards cards})
     :creature-count (deck/count-by-type {:cards cards} :creature)
     :action-count (deck/count-by-type {:cards cards} :action)
     :artifact-count (deck/count-by-type {:cards cards} :artifact)
     :upgrade-count (deck/count-by-type {:cards cards} :upgrade)}))

;; ============================================================================
;; Public API
;; ============================================================================

(defn- transform-dok-v3-deck
  "Transform v3 API deck response to our internal format"
  [api-response deck-uuid]
  (log/debug "DoK v3 API response keys:" (keys api-response))
  (let [deck-data (:deck api-response)
        sas-version (:sasVersion api-response)
        deck-name (or (:name deck-data) (:deck_name deck-data) "Unknown Deck")
        houses-and-cards (:housesAndCards deck-data)
        synergy-details (:synergyDetails deck-data)
        
        ;; Create lookup map for SAS data by card name (handling enhanced versions)
        sas-lookup (reduce (fn [acc sas-entry]
                             (let [card-name (:cardName sas-entry)
                                   base-name (clojure.string/replace card-name #" Enhanced$" "")]
                               (assoc acc card-name sas-entry)))
                           {}
                           synergy-details)
        
        ;; Process all cards with their SAS data
        cards (mapcat (fn [{:keys [house cards]}]
                        (map (fn [card]
                               (let [card-title (:cardTitle card)
                                     enhanced? (:enhanced card)
                                     ;; Try enhanced name first, then base name
                                     sas-key (if enhanced?
                                               (str card-title " Enhanced")
                                               card-title)
                                     sas-data (or (get sas-lookup sas-key)
                                                  (get sas-lookup card-title)
                                                  {})]
                                 (transform-dok-card card house sas-data)))
                             cards))
                      houses-and-cards)
        houses (mapv (comp deck/normalize-house-name :house) houses-and-cards)]
    
    (log/debug "Deck data keys:" (keys deck-data))
    (log/debug "SAS version:" sas-version)
    (log/debug "Houses found:" houses)
    (log/debug "Total cards found:" (count cards))
    (log/debug "SAS lookup keys:" (take 10 (keys sas-lookup)))
    
    {:id (str (or (:id deck-data) deck-uuid))
     :name deck-name
     :uuid deck-uuid
     :identity (-> deck-name
                   clojure.string/lower-case
                   (clojure.string/replace #"[?.!,]" "")
                   (clojure.string/replace #"[\s']" "-"))
     :houses houses
     :cards cards
     :expansion (:expansion deck-data)
     :source :decks-of-keyforge
     :power-level (:totalPower deck-data)
     :sas-rating (when (:sasRating deck-data)
                   {:sas-rating (:sasRating deck-data)
                    :sas-version sas-version
                    :amber (:amberControl deck-data)
                    :expected-amber (:expectedAmber deck-data)
                    :artifact-control (:artifactControl deck-data)
                    :creature-control (:creatureControl deck-data)
                    :efficiency (:efficiency deck-data)
                    :recursion (:recursion deck-data)
                    :creature-protection (:creatureProtection deck-data)
                    :disruption (:disruption deck-data)
                    :other (:other deck-data)
                    :effective-power (:effectivePower deck-data)
                    :raw-amber (:rawAmber deck-data)
                    :synergy-rating (:synergyRating deck-data)
                    :antisynergy-rating (:antisynergyRating deck-data)
                    :aerc-score (:aercScore deck-data)})
     :chains (:chains deck-data)
     :wins (:wins deck-data)
     :losses (:losses deck-data)
     :win-rate (when (and (:wins deck-data) (:losses deck-data))
                 (let [total-games (+ (:wins deck-data) (:losses deck-data))]
                   (if (> total-games 0)
                     (/ (:wins deck-data) total-games)
                     0.0)))
     :usage-count nil
     :verified? nil
     :is-alliance? false
     :last-updated (when (:lastSasUpdate deck-data)
                     (try (time/local-date (:lastSasUpdate deck-data))
                          (catch Exception _ nil)))
     :fetched-at (time/instant)
     :total-power (deck/calculate-deck-power {:cards cards})
     :total-amber (deck/calculate-total-amber {:cards cards})
     :creature-count (deck/count-by-type {:cards cards} :creature)
     :action-count (deck/count-by-type {:cards cards} :action)
     :artifact-count (deck/count-by-type {:cards cards} :artifact)
     :upgrade-count (deck/count-by-type {:cards cards} :upgrade)}))

(s/defn fetch-deck :- deck/Deck
  "Fetch a deck from Decks of Keyforge API - uses v3 API if API key available, falls back to legacy"
  [deck-id :- s/Str]
  (if (and (get-api-key) (re-matches #"^[a-fA-F0-9-]{36}$" deck-id))
    ;; Use v3 API for Keyforge UUIDs when API key is available
    (let [response (fetch-deck-v3 deck-id)]
      (transform-dok-v3-deck response deck-id))
    ;; Fall back to legacy API (will likely fail but maintains compatibility)
    (let [response (if (re-matches #"^[a-fA-F0-9-]{36}$" deck-id)
                     (fetch-deck-by-keyforge-id deck-id)
                     (fetch-deck-by-dok-id deck-id))]
      (transform-dok-deck response))))

(defn test-api-key
  "Test if the DoK API key is working"
  []
  (if (get-api-key)
    (try
      (log/info "Testing DoK API key...")
      ;; Use a well-known deck UUID for testing
      (let [test-uuid "938ded7f-4ea0-4698-b47c-2cdabb44e76c"
            url (str api-base-url "/" api-version "/decks/" test-uuid)]
        (make-request url {})
        (log/info "DoK API key is working!")
        true)
      (catch Exception e
        (log/error "DoK API key test failed:" (.getMessage e))
        false))
    (do
      (log/warn "No DoK API key configured")
      false)))

(defn search-and-fetch-first
  "Search for decks by name and return the first match (DEPRECATED - uses old API)"
  [deck-name]
  (log/warn "search-and-fetch-first uses deprecated API - will likely fail")
  (log/info "Searching for deck:" deck-name)
  (let [search-results (search-decks deck-name)
        first-result (first search-results)]
    (if first-result
      (do
        (log/info "Found deck, fetching details:" (:name first-result))
        (fetch-deck (str (:id first-result))))
      (throw (ex-info "No decks found" {:query deck-name})))))

(defn test-v3-transformation 
  "Test function to verify v3 API transformation with the provided deck UUID"
  []
  (let [test-deck-uuid "938ded7f-4ea0-4698-b47c-2cdabb44e76c"]
    (log/info "Testing v3 API transformation with deck:" test-deck-uuid)
    (try
      (let [result (fetch-deck test-deck-uuid)]
        (log/info "Transformation successful!")
        (log/info "Deck name:" (:name result))
        (log/info "Houses:" (:houses result))
        (log/info "Total cards:" (count (:cards result)))
        (log/info "Cards with SAS data:" 
                  (count (filter #(some? (:aerc-score %)) (:cards result))))
        (log/info "Sample card with SAS data:" 
                  (-> (filter #(some? (:aerc-score %)) (:cards result))
                      first
                      (select-keys [:name :house :aerc-score :creature-control :amber-control])))
        result)
      (catch Exception e
        (log/error "Transformation failed:" (.getMessage e))
        (throw e)))))