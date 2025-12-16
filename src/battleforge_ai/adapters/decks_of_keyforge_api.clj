(ns battleforge-ai.adapters.decks-of-keyforge-api
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [schema.core :as s]
            [battleforge-ai.models.deck :as deck]
            [java-time.api :as time]))

(defn- parse-keywords
  "Extract keywords from card text"
  [card-text]
  (if (nil? card-text)
    []
    (let [valid-keywords #{:elusive :skirmish :taunt :deploy :alpha :omega
                           :hazardous :assault :poison :splash-attack}
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
  "Transform Decks of Keyforge API card to internal card format"
  [api-card house sas-data]
  (let [card-id (-> (:cardTitle api-card)
                    str/lower-case
                    (str/replace #"[?.!,]" "")
                    (str/replace #"[\s']" "-"))
        enhanced? (:enhanced api-card)]
    {:id card-id,
     :name (:cardTitle api-card),
     :house (deck/normalize-house-name house),
     :card-type (when (:cardType api-card)
                  (deck/normalize-card-type (:cardType api-card))),
     :amber (or (:amber api-card) (:bonusAember api-card) 0),
     :power (:power api-card),
     :armor (:armor api-card),
     :rarity (when (:rarity api-card)
               (deck/normalize-rarity (:rarity api-card))),
     :card-text (:cardText api-card),
     :traits (or (:traits api-card) []),
     :keywords (parse-keywords (:cardText api-card)),
     :expansion (:expansion api-card),
     :number (:cardNumber api-card),
     :image (or (:frontImage api-card) (:cardTitleUrl api-card)),
     :count (or (:copies sas-data) 1),
     :enhanced? enhanced?,
     :maverick? (:maverick api-card),
     :maverick-house (when (:maverick api-card)
                       (deck/normalize-house-name house)),
     :anomaly? (:anomaly api-card),
     :anomaly-house (when (:anomaly api-card)
                      (deck/normalize-house-name house)),
     :enhancements
       (when enhanced?
         (cond-> []
           (> (or (:bonusAember api-card) 0) 0)
             (conj {:type :amber, :value (:bonusAember api-card)})
           (> (or (:bonusCapture api-card) 0) 0)
             (conj {:type :capture, :value (:bonusCapture api-card)})
           (> (or (:bonusDamage api-card) 0) 0)
             (conj {:type :damage, :value (:bonusDamage api-card)})
           (> (or (:bonusDraw api-card) 0) 0)
             (conj {:type :draw, :value (:bonusDraw api-card)})
           (> (or (:bonusDiscard api-card) 0) 0)
             (conj {:type :discard, :value (:bonusDiscard api-card)}))),
     :aerc-score (:aercScore sas-data),
     :expected-amber (:expectedAmber sas-data),
     :amber-control (:amberControl sas-data),
     :creature-control (:creatureControl sas-data),
     :artifact-control (:artifactControl sas-data),
     :efficiency (:efficiency sas-data),
     :recursion (:recursion sas-data),
     :effective-power (:effectivePower sas-data),
     :creature-protection (:creatureProtection sas-data),
     :disruption (:disruption sas-data),
     :other (:other sas-data),
     :net-synergy (:netSynergy sas-data),
     :synergies (:synergies sas-data),
     :copies (:copies sas-data)}))

(s/defn legacy-response->deck
  :-
  deck/Deck
  "Transform legacy Decks of Keyforge API response to internal deck format"
  [api-response]
  (let [deck-data api-response
        cards (map (fn [card] (api-card->card card (:house card) {}))
                (:cards deck-data))
        houses (mapv deck/normalize-house-name (:houses deck-data))]
    {:id (str (:id deck-data)),
     :name (:name deck-data),
     :uuid (:keyforge_id deck-data),
     :identity (-> (:name deck-data)
                   str/lower-case
                   (str/replace #"[?.!,]" "")
                   (str/replace #"[\s']" "-")),
     :houses houses,
     :cards cards,
     :expansion (:expansion deck-data),
     :source :decks-of-keyforge,
     :power-level (:power_level deck-data),
     :sas-rating (when-let [sas-data (:sas_rating deck-data)]
                   {:sas-rating (:sas_rating sas-data),
                    :amber (:amber sas-data),
                    :expected-amber (:expected_amber sas-data),
                    :artifact-control (:artifact_control sas-data),
                    :creature-control (:creature_control sas-data),
                    :efficiency (:efficiency sas-data),
                    :recursion (:recursion sas-data),
                    :creature-protection (:creature_protection sas-data),
                    :disruption (:disruption sas-data),
                    :other (:other sas-data),
                    :effective-power (:effective_power sas-data),
                    :raw-amber (:raw_amber sas-data),
                    :synergy-rating (:synergy_rating sas-data),
                    :antisynergy-rating (:antisynergy_rating sas-data)}),
     :chains (:chains deck-data),
     :wins (:wins deck-data),
     :losses (:losses deck-data),
     :win-rate
       (when (and (:wins deck-data) (:losses deck-data))
         (let [total-games (+ (:wins deck-data) (:losses deck-data))]
           (if (> total-games 0) (/ (:wins deck-data) total-games) 0.0))),
     :usage-count nil,
     :verified? nil,
     :is-alliance? false,
     :last-updated (when (:last_update deck-data)
                     (time/instant (:last_update deck-data))),
     :fetched-at (time/instant),
     :total-power (deck/calculate-deck-power {:cards cards}),
     :total-amber (deck/calculate-total-amber {:cards cards}),
     :creature-count (deck/count-by-type {:cards cards} :creature),
     :action-count (deck/count-by-type {:cards cards} :action),
     :artifact-count (deck/count-by-type {:cards cards} :artifact),
     :upgrade-count (deck/count-by-type {:cards cards} :upgrade)}))

(s/defn v3-response->deck
  :-
  deck/Deck
  "Transform v3 API response to internal deck format"
  [api-response deck-uuid :- s/Str]
  (log/debug "DoK v3 API response keys:" (keys api-response))
  (let [deck-data (:deck api-response)
        sas-version (:sasVersion api-response)
        deck-name (or (:name deck-data) (:deck_name deck-data) "Unknown Deck")
        houses-and-cards (:housesAndCards deck-data)
        synergy-details (:synergyDetails deck-data)
        sas-lookup (reduce (fn [acc sas-entry]
                             (let [card-name (:cardName sas-entry)]
                               (assoc acc card-name sas-entry)))
                     {}
                     synergy-details)
        cards (mapcat (fn [{:keys [house cards]}]
                        (map (fn [card]
                               (let [card-title (:cardTitle card)
                                     enhanced? (:enhanced card)
                                     sas-key (if enhanced?
                                               (str card-title " Enhanced")
                                               card-title)
                                     sas-data (or (get sas-lookup sas-key)
                                                  (get sas-lookup card-title)
                                                  {})]
                                 (api-card->card card house sas-data)))
                          cards))
                houses-and-cards)
        houses (mapv (comp deck/normalize-house-name :house) houses-and-cards)]
    (log/debug "Deck data keys:" (keys deck-data))
    (log/debug "SAS version:" sas-version)
    (log/debug "Houses found:" houses)
    (log/debug "Total cards found:" (count cards))
    (log/debug "SAS lookup keys:" (take 10 (keys sas-lookup)))
    {:id (str (or (:id deck-data) deck-uuid)),
     :name deck-name,
     :uuid deck-uuid,
     :identity (-> deck-name
                   str/lower-case
                   (str/replace #"[?.!,]" "")
                   (str/replace #"[\s']" "-")),
     :houses houses,
     :cards cards,
     :expansion (:expansion deck-data),
     :source :decks-of-keyforge,
     :power-level (:totalPower deck-data),
     :sas-rating (when (:sasRating deck-data)
                   {:sas-rating (:sasRating deck-data),
                    :sas-version sas-version,
                    :amber (:amberControl deck-data),
                    :expected-amber (:expectedAmber deck-data),
                    :artifact-control (:artifactControl deck-data),
                    :creature-control (:creatureControl deck-data),
                    :efficiency (:efficiency deck-data),
                    :recursion (:recursion deck-data),
                    :creature-protection (:creatureProtection deck-data),
                    :disruption (:disruption deck-data),
                    :other (:other deck-data),
                    :effective-power (:effectivePower deck-data),
                    :raw-amber (:rawAmber deck-data),
                    :synergy-rating (:synergyRating deck-data),
                    :antisynergy-rating (:antisynergyRating deck-data),
                    :aerc-score (:aercScore deck-data)}),
     :chains (:chains deck-data),
     :wins (:wins deck-data),
     :losses (:losses deck-data),
     :win-rate
       (when (and (:wins deck-data) (:losses deck-data))
         (let [total-games (+ (:wins deck-data) (:losses deck-data))]
           (if (> total-games 0) (/ (:wins deck-data) total-games) 0.0))),
     :usage-count nil,
     :verified? nil,
     :is-alliance? false,
     :last-updated (when (:lastSasUpdate deck-data)
                     (try (time/local-date (:lastSasUpdate deck-data))
                          (catch Exception _ nil))),
     :fetched-at (time/instant),
     :total-power (deck/calculate-deck-power {:cards cards}),
     :total-amber (deck/calculate-total-amber {:cards cards}),
     :creature-count (deck/count-by-type {:cards cards} :creature),
     :action-count (deck/count-by-type {:cards cards} :action),
     :artifact-count (deck/count-by-type {:cards cards} :artifact),
     :upgrade-count (deck/count-by-type {:cards cards} :upgrade)}))
