(ns battleforge-ai.diplomat.http-out.decks-of-keyforge-api
  (:require [clj-http.client :as http]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [schema.core :as s]
            [battleforge-ai.adapters.decks-of-keyforge-api :as dok-adapter]
            [battleforge-ai.models.deck :as deck]))

(def ^:private api-base-url "https://decksofkeyforge.com/public-api")
(def ^:private api-version "v3")
(def ^:private legacy-api-url "https://decksofkeyforge.com/api/decks")
(def ^:private request-timeout 30000)
(def ^:private rate-limit-delay 1000)

(def ^:private ^:dynamic *api-key* nil)

(defn set-api-key!
  "Set the DoK API key"
  [key]
  (alter-var-root #'*api-key* (constantly key))
  (log/info "DoK API key configured"))

(defn get-api-key "Get current API key" [] *api-key*)

(defn load-config-api-key!
  "Load API key from environment variable or config.edn file"
  ([] (load-config-api-key! "config.edn"))
  ([config-path]
   (try (if-let [env-key (System/getenv "DOK_API_KEY")]
          (do (set-api-key! env-key)
              (log/info
                "Loaded DoK API key from DOK_API_KEY environment variable"))
          (when (.exists (io/file config-path))
            (let [config (edn/read-string (slurp config-path))
                  api-key (get-in config [:decks-of-keyforge :api-key])]
              (when api-key
                (set-api-key! api-key)
                (log/info "Loaded DoK API key from configuration file")))))
        (catch Exception e
          (log/warn "Could not load configuration from" config-path
                    ":" (.getMessage e))))))

;; Initialize API key from config on namespace load
(load-config-api-key!)

(defn- make-request!
  "Make HTTP request with error handling and rate limiting"
  [url options]
  (Thread/sleep rate-limit-delay)
  (try (let [api-key (get-api-key)
             headers (cond-> {:accept "application/json"}
                       api-key (assoc "Api-Key" api-key))]
         (log/debug "Making DoK API request to:" url)
         (if api-key
           (log/debug "Using API key:" (str (subs api-key 0 8) "..."))
           (log/debug "No API key configured"))
         (let [response (http/get url
                                  (merge {:timeout request-timeout,
                                          :headers headers,
                                          :as :json}
                                         options))]
           (:body response)))
       (catch Exception e
         (log/error e "Failed to make request to" url)
         (throw (ex-info "API request failed"
                         {:url url, :error (.getMessage e)})))))

(defn fetch-deck-v3!
  "Fetch a deck using the v3 API with SAS ratings"
  [deck-uuid]
  (log/info "Fetching deck from DoK v3 API:" deck-uuid)
  (let [url (str api-base-url "/" api-version "/decks/" deck-uuid)]
    (if (get-api-key)
      (do (log/debug "Using authenticated DoK v3 API") (make-request! url {}))
      (do (log/warn
            "No DoK API key configured - v3 API requires authentication")
          (throw (ex-info "DoK API key required for v3 API"
                          {:deck-uuid deck-uuid}))))))

(defn search-decks!
  "Search for decks by name or other criteria (DEPRECATED - uses old API)"
  [query & {:keys [page page-size], :or {page 1, page-size 20}}]
  (log/warn "Using deprecated search API - will likely fail")
  (log/info "Searching decks on Decks of Keyforge:" query)
  (let [response (make-request! legacy-api-url
                                {:query-params {:search query,
                                                :page page,
                                                :page_size page-size}})]
    (log/debug "Found" (count (:data response)) "decks for query:" query)
    (:data response)))

(defn fetch-deck-by-keyforge-id!
  "Fetch a deck from Decks of Keyforge API using Keyforge UUID (DEPRECATED - uses old API)"
  [keyforge-id]
  (log/warn
    "Using deprecated DoK API - will likely fail. Consider using v3 API with API key.")
  (log/info "Fetching deck from Decks of Keyforge by Keyforge ID:" keyforge-id)
  (let [url (str legacy-api-url "/" keyforge-id)
        response (make-request! url {})]
    (log/debug "Received deck response for Keyforge ID" keyforge-id)
    response))

(defn fetch-deck-by-dok-id!
  "Fetch a deck from Decks of Keyforge API using their internal ID (DEPRECATED - uses old API)"
  [dok-id]
  (log/warn
    "Using deprecated DoK API - will likely fail. Consider using v3 API with API key.")
  (log/info "Fetching deck from Decks of Keyforge by DoK ID:" dok-id)
  (let [url (str legacy-api-url "/" dok-id)
        response (make-request! url {})]
    (log/debug "Received deck response for DoK ID" dok-id)
    response))

(s/defn fetch-deck!
  :-
  deck/Deck
  "Fetch a deck from Decks of Keyforge API - uses v3 API if API key available, falls back to legacy"
  [deck-id :- s/Str]
  (if (and (get-api-key) (re-matches #"^[a-fA-F0-9-]{36}$" deck-id))
    (let [response (fetch-deck-v3! deck-id)]
      (dok-adapter/v3-response->deck response deck-id))
    (let [response (if (re-matches #"^[a-fA-F0-9-]{36}$" deck-id)
                     (fetch-deck-by-keyforge-id! deck-id)
                     (fetch-deck-by-dok-id! deck-id))]
      (dok-adapter/legacy-response->deck response))))

(defn test-api-key!
  "Test if the DoK API key is working"
  []
  (if (get-api-key)
    (try (log/info "Testing DoK API key...")
         (let [test-uuid "938ded7f-4ea0-4698-b47c-2cdabb44e76c"
               url (str api-base-url "/" api-version "/decks/" test-uuid)]
           (make-request! url {})
           (log/info "DoK API key is working!")
           true)
         (catch Exception e
           (log/error "DoK API key test failed:" (.getMessage e))
           false))
    (do (log/warn "No DoK API key configured") false)))

(defn search-and-fetch-first!
  "Search for decks by name and return the first match (DEPRECATED - uses old API)"
  [deck-name]
  (log/warn "search-and-fetch-first uses deprecated API - will likely fail")
  (log/info "Searching for deck:" deck-name)
  (let [search-results (search-decks! deck-name)
        first-result (first search-results)]
    (if first-result
      (do (log/info "Found deck, fetching details:" (:name first-result))
          (fetch-deck! (str (:id first-result))))
      (throw (ex-info "No decks found" {:query deck-name})))))

(defn test-v3-transformation!
  "Test function to verify v3 API transformation with the provided deck UUID"
  []
  (let [test-deck-uuid "938ded7f-4ea0-4698-b47c-2cdabb44e76c"]
    (log/info "Testing v3 API transformation with deck:" test-deck-uuid)
    (try (let [result (fetch-deck! test-deck-uuid)]
           (log/info "Transformation successful!")
           (log/info "Deck name:" (:name result))
           (log/info "Houses:" (:houses result))
           (log/info "Total cards:" (count (:cards result)))
           (log/info "Cards with SAS data:"
                     (count (filter #(some? (:aerc-score %)) (:cards result))))
           (log/info "Sample card with SAS data:"
                     (-> (filter #(some? (:aerc-score %)) (:cards result))
                         first
                         (select-keys [:name :house :aerc-score
                                       :creature-control :amber-control])))
           result)
         (catch Exception e
           (log/error "Transformation failed:" (.getMessage e))
           (throw e)))))

