(ns battleforge-ai.adapters.decks-of-keyforge-api
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [schema.core :as s]
            [battleforge-ai.models.deck :as deck]
            [java-time :as time]))

;; ============================================================================
;; API Configuration  
;; ============================================================================

(def ^:private api-base-url "https://decksofkeyforge.com/api")
(def ^:private request-timeout 30000) ; 30 seconds
(def ^:private rate-limit-delay 1000) ; 1 second between requests

;; ============================================================================
;; HTTP Utilities
;; ============================================================================

(defn- make-request
  "Make HTTP request with error handling and rate limiting"
  [url options]
  (Thread/sleep rate-limit-delay) ; Simple rate limiting
  (try
    (let [response (http/get url (merge {:timeout request-timeout
                                         :accept :json
                                         :as :json} 
                                        options))]
      (:body response))
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
  [api-card]
  (let [card-id (-> (:cardTitle api-card)
                    clojure.string/lower-case
                    (clojure.string/replace #"[?.!,]" "")
                    (clojure.string/replace #"[\s']" "-"))]
    {:id card-id
     :name (:cardTitle api-card)
     :house (deck/normalize-house-name (:house api-card))
     :card-type (deck/normalize-card-type (:cardType api-card))
     :amber (or (:amber api-card) 0)
     :power (:power api-card)
     :armor (:armor api-card)
     :rarity (deck/normalize-rarity (:rarity api-card))
     :card-text (:cardText api-card)
     :traits (or (:traits api-card) [])
     :keywords (parse-keywords (:cardText api-card))
     :expansion (:expansion api-card)
     :number (:cardNumber api-card)
     :image (:frontImage api-card)
     :count 1
     :enhanced? (:enhanced api-card)
     :maverick? (:maverick api-card)
     :maverick-house (when (:maverick api-card)
                       (deck/normalize-house-name (:house api-card)))
     :anomaly? (:anomaly api-card)
     :anomaly-house (when (:anomaly api-card)
                      (deck/normalize-house-name (:house api-card)))}))

;; ============================================================================
;; Deck Search and Fetching
;; ============================================================================

(defn search-decks
  "Search for decks by name or other criteria"
  [query & {:keys [page page-size] :or {page 1 page-size 20}}]
  (log/info "Searching decks on Decks of Keyforge:" query)
  (let [url (str api-base-url "/decks")
        response (make-request url {:query-params {:search query
                                                   :page page
                                                   :page_size page-size}})]
    (log/debug "Found" (count (:data response)) "decks for query:" query)
    (:data response)))

(defn fetch-deck-by-keyforge-id
  "Fetch a deck from Decks of Keyforge API using Keyforge UUID"
  [keyforge-id]
  (log/info "Fetching deck from Decks of Keyforge by Keyforge ID:" keyforge-id)
  (let [url (str api-base-url "/decks/" keyforge-id)
        response (make-request url {})]
    (log/debug "Received deck response for Keyforge ID" keyforge-id)
    response))

(defn fetch-deck-by-dok-id
  "Fetch a deck from Decks of Keyforge API using their internal ID"
  [dok-id]
  (log/info "Fetching deck from Decks of Keyforge by DoK ID:" dok-id)
  (let [url (str api-base-url "/decks/" dok-id)
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
        cards (map transform-dok-card (:cards deck-data))
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

(s/defn fetch-deck :- deck/Deck
  "Fetch a deck from Decks of Keyforge API by Keyforge UUID or DoK ID"
  [deck-id :- s/Str]
  (let [response (if (re-matches #"^[a-fA-F0-9-]{36}$" deck-id)
                   (fetch-deck-by-keyforge-id deck-id)
                   (fetch-deck-by-dok-id deck-id))]
    (transform-dok-deck response)))

(defn search-and-fetch-first
  "Search for decks by name and return the first match"
  [deck-name]
  (log/info "Searching for deck:" deck-name)
  (let [search-results (search-decks deck-name)
        first-result (first search-results)]
    (if first-result
      (do
        (log/info "Found deck, fetching details:" (:name first-result))
        (fetch-deck (str (:id first-result))))
      (throw (ex-info "No decks found" {:query deck-name})))))