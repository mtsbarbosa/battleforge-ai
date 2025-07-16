(ns battleforge-ai.models.deck
  (:require [schema.core :as s]
            [clojure.string :as str]))

;; ============================================================================
;; Deck Models and Schemas
;; ============================================================================

(s/defschema House
  "Keyforge house representation"
  (s/enum :brobnar :dis :logos :mars :sanctum :shadows :untamed
          :star-alliance :saurian :unfathomable :ekwidon :geistoid))

(s/defschema CardType
  "Card type enumeration"
  (s/enum :action :artifact :creature :upgrade))

(s/defschema Rarity
  "Card rarity enumeration"  
  (s/enum :common :uncommon :rare :special :fixed :variant))

(s/defschema Enhancement
  "Card enhancement representation"
  {:type (s/enum :amber :capture :damage :draw :discard)
   :value s/Int})

(s/defschema Card
  "Individual card representation"
  {:id                s/Str
   :name              s/Str
   :house             House
   :card-type         CardType
   :amber             s/Int
   :power             (s/maybe s/Int)
   :armor             (s/maybe s/Int)
   :rarity            Rarity
   :card-text         (s/maybe s/Str)
   :traits            [s/Str]
   :keywords          [s/Str]
   :expansion         s/Int
   :number           (s/maybe s/Str)
   :image             (s/maybe s/Str)
   (s/optional-key :count) s/Int
   (s/optional-key :enhanced?) s/Bool
   (s/optional-key :enhancements) [Enhancement]
   (s/optional-key :maverick?) s/Bool
   (s/optional-key :maverick-house) House  
   (s/optional-key :anomaly?) s/Bool
   (s/optional-key :anomaly-house) House
   (s/optional-key :uuid) s/Str})

(s/defschema DeckSource
  "Source where deck was fetched from"
  (s/enum :keyforge-api :decks-of-keyforge :local-file :manual))

(s/defschema Deck
  "Complete deck representation"
  {:id            s/Str
   :name          s/Str  
   :uuid          (s/maybe s/Str)    ; Keyforge UUID
   :identity      (s/maybe s/Str)    ; URL-safe name
   :houses        [House]
   :cards         [Card]
   :expansion     s/Int
   :source        DeckSource
   :power-level   (s/maybe s/Int)
   :chains        (s/maybe s/Int)
   :wins          (s/maybe s/Int)
   :losses        (s/maybe s/Int)
   :win-rate      (s/maybe s/Num)
   :usage-count   (s/maybe s/Int)
   :verified?     (s/maybe s/Bool)
   :is-alliance?  (s/maybe s/Bool)
   :last-updated  (s/maybe java.time.Instant)
   :fetched-at    java.time.Instant
   ;; Calculated fields
   :total-power   (s/maybe s/Int)
   :total-amber   (s/maybe s/Int)
   :creature-count (s/maybe s/Int)
   :action-count   (s/maybe s/Int)
   :artifact-count (s/maybe s/Int)
   :upgrade-count  (s/maybe s/Int)})

;; ============================================================================
;; API Response Schemas
;; ============================================================================

(s/defschema KeyforgeApiCard
  "Card structure from Keyforge API"
  {:id s/Str
   :card_title s/Str
   :card_text (s/maybe s/Str)
   :card_type s/Str
   :house s/Str
   :power (s/maybe s/Int)
   :armor (s/maybe s/Int)
   :aember s/Int
   :rarity s/Str
   :expansion s/Int
   :card_number (s/maybe s/Str)
   :front_image (s/maybe s/Str)
   :is_maverick s/Bool
   :is_anomaly s/Bool
   :is_enhanced s/Bool
   (s/optional-key :is_non_deck) s/Bool})

(s/defschema KeyforgeApiDeck
  "Deck structure from Keyforge API"
  {:data {:id s/Str
          :name s/Str
          :expansion s/Int
          :_links {:houses [s/Str]
                   :cards [s/Str]}}
   :_linked {:cards [KeyforgeApiCard]}})

(s/defschema DecksOfKeyforgeCard
  "Card structure from Decks of Keyforge API"
  {:cardTitle s/Str
   :cardText (s/maybe s/Str)
   :cardType s/Str
   :house s/Str
   :power (s/maybe s/Int)
   :armor (s/maybe s/Int)
   :amber s/Int
   :rarity s/Str
   :expansion s/Int
   :cardNumber (s/maybe s/Str)
   :frontImage (s/maybe s/Str)
   :traits (s/maybe [s/Str])
   :maverick s/Bool
   :anomaly s/Bool
   :enhanced s/Bool})

;; ============================================================================
;; Deck Analysis Models
;; ============================================================================

(s/defschema DeckStats
  "Statistical analysis of a deck"
  {:deck-id           s/Str
   :average-amber     s/Num
   :creature-quality  s/Num
   :action-density    s/Num
   :control-rating    s/Num
   :tempo-rating      s/Num
   :synergy-score     s/Num
   :consistency-score s/Num})

(s/defschema HouseAnalysis
  "Analysis of individual house performance in a deck"
  {:house             House
   :card-count        s/Int
   :total-amber       s/Int
   :creature-count    s/Int
   :action-count      s/Int
   :artifact-count    s/Int
   :upgrade-count     s/Int
   :synergy-rating    s/Num})

;; ============================================================================
;; Utility Functions (Pure - No Implementation Yet)
;; ============================================================================

(defn deck->houses
  "Extract houses from a deck"
  [deck]
  (:houses deck))

(defn count-by-type
  "Count cards by type in a deck"
  [deck card-type]
  (->> (:cards deck)
       (filter #(= (:card-type %) card-type))
       (count)))

(defn calculate-deck-power
  "Calculate total power rating for a deck"
  [deck]
  (->> (:cards deck)
       (map :power)
       (filter some?)
       (reduce +)))

(defn calculate-total-amber
  "Calculate total amber in a deck"
  [deck]
  (->> (:cards deck)
       (map :amber)
       (reduce +)))

(defn validate-deck
  "Validate that a deck follows Keyforge rules"
  [deck]
  (let [cards (:cards deck)
        houses (:houses deck)]
    (and
      ;; Exactly 36 cards
      (= 36 (count cards))
      ;; Exactly 3 houses
      (= 3 (count houses))
      ;; 12 cards per house (approximately, accounting for mavericks/anomalies)
      (every? #(<= 10 % 14) 
        (vals (frequencies (map :house cards)))))))

(defn normalize-house-name
  "Normalize house name from API format to our format"
  [house-str]
  (-> house-str
      str/lower-case
      (str/replace #"\s+" "")
      (str/replace #"[^a-z]" "")
      keyword))

(defn normalize-card-type
  "Normalize card type from API format to our format"
  [type-str]
  (-> type-str
      str/lower-case
      keyword))

(defn normalize-rarity
  "Normalize rarity from API format to our format"
  [rarity-str]
  (-> rarity-str
      str/lower-case
      keyword)) 