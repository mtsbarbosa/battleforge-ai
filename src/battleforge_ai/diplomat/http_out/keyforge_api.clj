(ns battleforge-ai.diplomat.http-out.keyforge-api
  (:require [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [schema.core :as s]
            [battleforge-ai.adapters.keyforge-api :as keyforge-adapter]
            [battleforge-ai.models.deck :as deck]))

(def ^:private api-base-url "https://www.keyforgegame.com/api")
(def ^:private request-timeout 30000)
(def ^:private rate-limit-delay 1000)

(defn- make-request!
  "Make HTTP request with error handling and rate limiting"
  [url options]
  (Thread/sleep rate-limit-delay)
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

(s/defn fetch-deck! :- deck/Deck
  "Fetch a deck from the Keyforge API by UUID and transform to internal format"
  [deck-uuid :- s/Str]
  (when-not (keyforge-adapter/validate-deck-uuid deck-uuid)
    (throw (ex-info "Invalid Keyforge UUID format"
                    {:uuid deck-uuid :type :validation-error})))
  (log/info "Fetching deck from Keyforge API:" deck-uuid)
  (let [url (str api-base-url "/decks/" deck-uuid "/?links=cards")
        response (make-request! url {})]
    (log/debug "Received deck response for" deck-uuid)
    (keyforge-adapter/api-response->deck response)))

