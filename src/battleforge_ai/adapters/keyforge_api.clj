(ns battleforge-ai.adapters.keyforge-api
  (:require [clojure.string :as str]
            [schema.core :as s]
            [battleforge-ai.models.deck :as deck]
            [java-time.api :as time]))

(defn- parse-keywords
  "Extract keywords from card text"
  [card-text]
  (if (nil? card-text)
    []
    (let [valid-keywords #{:elusive :skirmish :taunt :deploy :alpha :omega
                           :hazardous :assault :poison :splash-attack
                           :treachery :versatile}
          lines (str/split card-text #"[\r\n]")
          potential-keywords (mapcat #(str/split % #"\.") lines)
          normalized-keywords (map #(-> %
                                        str/lower-case
                                        str/trim
                                        (str/replace #"\s+" "-")
                                        keyword)
                                   potential-keywords)]
      (->> normalized-keywords
           (filter valid-keywords)
           (into [])))))

(defn- api-card->card
  "Transform Keyforge API card to internal card format"
  [api-card]
  (let [card-id (-> (:card_title api-card)
                    str/lower-case
                    (str/replace #"[?.!,]" "")
                    (str/replace #"[\s']" "-"))]
    {:id card-id
     :name (:card_title api-card)
     :house (deck/normalize-house-name (:house api-card))
     :card-type (deck/normalize-card-type (:card_type api-card))
     :amber (or (when-let [aember (:aember api-card)]
                  (if (string? aember)
                    (Integer/parseInt aember)
                    aember)) 0)
     :power (when-let [power (:power api-card)]
              (if (string? power)
                (Integer/parseInt power)
                power))
     :armor (when-let [armor (:armor api-card)]
              (if (string? armor)
                (Integer/parseInt armor)
                armor))
     :rarity (deck/normalize-rarity (:rarity api-card))
     :card-text (:card_text api-card)
     :traits (or (:traits api-card) [])
     :keywords (parse-keywords (:card_text api-card))
     :expansion (:expansion api-card)
     :number (:card_number api-card)
     :image (:front_image api-card)
     :count 1
     :enhanced? (:is_enhanced api-card)
     :maverick? (:is_maverick api-card)
     :maverick-house (when (:is_maverick api-card)
                       (deck/normalize-house-name (:house api-card)))
     :anomaly? (:is_anomaly api-card)
     :anomaly-house (when (:is_anomaly api-card)
                      (deck/normalize-house-name (:house api-card)))
     :uuid (:id api-card)}))

(s/defn api-response->deck :- deck/Deck
  "Transform Keyforge API response to internal deck format"
  [api-response]
  (let [deck-data (:data api-response)
        linked-cards (:cards (:_linked api-response))
        cards (map api-card->card linked-cards)
        houses (mapv deck/normalize-house-name
                     (get-in deck-data [:_links :houses]))]
    {:id (:id deck-data)
     :name (:name deck-data)
     :uuid (:id deck-data)
     :identity (-> (:name deck-data)
                   str/lower-case
                   (str/replace #"[?.!,]" "")
                   (str/replace #"[\s']" "-"))
     :houses houses
     :cards cards
     :expansion (:expansion deck-data)
     :source :keyforge-api
     :power-level nil
     :sas-rating nil
     :chains nil
     :wins nil
     :losses nil
     :win-rate nil
     :usage-count nil
     :verified? nil
     :is-alliance? false
     :last-updated nil
     :fetched-at (time/instant)
     :total-power (deck/calculate-deck-power {:cards cards})
     :total-amber (deck/calculate-total-amber {:cards cards})
     :creature-count (deck/count-by-type {:cards cards} :creature)
     :action-count (deck/count-by-type {:cards cards} :action)
     :artifact-count (deck/count-by-type {:cards cards} :artifact)
     :upgrade-count (deck/count-by-type {:cards cards} :upgrade)}))

(defn validate-deck-uuid
  "Validate that a string looks like a valid Keyforge deck UUID"
  [uuid-str]
  (and (string? uuid-str)
       (re-matches #"^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$" uuid-str)))

(defn extract-uuid-from-url
  "Extract deck UUID from Keyforge deck URL"
  [url]
  (when-let [match (re-find #"decks/([a-fA-F0-9-]{36})" url)]
    (second match)))
