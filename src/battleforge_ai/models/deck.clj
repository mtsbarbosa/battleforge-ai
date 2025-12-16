(ns battleforge-ai.models.deck
  (:require [schema.core :as s]
            [clojure.string :as str]))

(s/defschema House
  (s/enum :brobnar :dis :logos :mars :sanctum :shadows :untamed
          :star-alliance :saurian :unfathomable :ekwidon :geistoid))

(s/defschema CardType
  (s/enum :action :artifact :creature :upgrade))

(s/defschema Rarity
  (s/enum :common :uncommon :rare :special :fixed :variant))

(s/defschema Enhancement
  {:type (s/enum :amber :capture :damage :draw :discard)
   :value s/Int})

(s/defschema Card
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
   (s/optional-key :uuid) s/Str
   (s/optional-key :aerc-score) (s/maybe s/Num)
   (s/optional-key :expected-amber) (s/maybe s/Num)
   (s/optional-key :amber-control) (s/maybe s/Num)
   (s/optional-key :creature-control) (s/maybe s/Num)
   (s/optional-key :artifact-control) (s/maybe s/Num)
   (s/optional-key :efficiency) (s/maybe s/Num)
   (s/optional-key :recursion) (s/maybe s/Num)
   (s/optional-key :effective-power) (s/maybe s/Num)
   (s/optional-key :creature-protection) (s/maybe s/Num)
   (s/optional-key :disruption) (s/maybe s/Num)
   (s/optional-key :other) (s/maybe s/Num)
   (s/optional-key :net-synergy) (s/maybe s/Num)
   (s/optional-key :synergies) (s/maybe [s/Any])
   (s/optional-key :copies) (s/maybe s/Int)})

(s/defschema DeckSource
  (s/enum :keyforge-api :decks-of-keyforge :local-file :manual))

(s/defschema SASRating
  {(s/optional-key :sas-rating) (s/maybe s/Num)
   (s/optional-key :sas-version) (s/maybe s/Int)
   (s/optional-key :aerc-score) (s/maybe s/Num)
   (s/optional-key :amber) (s/maybe s/Num)
   (s/optional-key :expected-amber) (s/maybe s/Num)
   (s/optional-key :artifact-control) (s/maybe s/Num)
   (s/optional-key :creature-control) (s/maybe s/Num)
   (s/optional-key :efficiency) (s/maybe s/Num)
   (s/optional-key :recursion) (s/maybe s/Num)
   (s/optional-key :creature-protection) (s/maybe s/Num)
   (s/optional-key :disruption) (s/maybe s/Num)
   (s/optional-key :other) (s/maybe s/Num)
   (s/optional-key :effective-power) (s/maybe s/Num)
   (s/optional-key :raw-amber) (s/maybe s/Num)
   (s/optional-key :synergy-rating) (s/maybe s/Num)
   (s/optional-key :antisynergy-rating) (s/maybe s/Num)})

(s/defschema Deck
  {:id            s/Str
   :name          s/Str  
   :uuid          (s/maybe s/Str)
   :identity      (s/maybe s/Str)
   :houses        [House]
   :cards         [Card]
   :expansion     s/Int
   :source        DeckSource
   :power-level   (s/maybe s/Int)
   :sas-rating    (s/maybe SASRating)
   :chains        (s/maybe s/Int)
   :wins          (s/maybe s/Int)
   :losses        (s/maybe s/Int)
   :win-rate      (s/maybe s/Num)
   :usage-count   (s/maybe s/Int)
   :verified?     (s/maybe s/Bool)
   :is-alliance?  (s/maybe s/Bool)
   :last-updated  (s/maybe java.time.Instant)
   :fetched-at    java.time.Instant
   :total-power   (s/maybe s/Int)
   :total-amber   (s/maybe s/Int)
   :creature-count (s/maybe s/Int)
   :action-count   (s/maybe s/Int)
   :artifact-count (s/maybe s/Int)
   :upgrade-count  (s/maybe s/Int)})

(s/defschema KeyforgeApiCard
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
  {:data {:id s/Str
          :name s/Str
          :expansion s/Int
          :_links {:houses [s/Str]
                   :cards [s/Str]}}
   :_linked {:cards [KeyforgeApiCard]}})

(s/defschema DecksOfKeyforgeCard
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

(s/defschema DeckStats
  {:deck-id           s/Str
   :average-amber     s/Num
   :creature-quality  s/Num
   :action-density    s/Num
   :control-rating    s/Num
   :tempo-rating      s/Num
   :synergy-score     s/Num
   :consistency-score s/Num})

(s/defschema HouseAnalysis
  {:house             House
   :card-count        s/Int
   :total-amber       s/Int
   :creature-count    s/Int
   :action-count      s/Int
   :artifact-count    s/Int
   :upgrade-count     s/Int
   :synergy-rating    s/Num})

(defn deck->houses [deck]
  (:houses deck))

(defn count-by-type [deck card-type]
  (->> (:cards deck)
       (filter #(= (:card-type %) card-type))
       (count)))

(defn calculate-deck-power [deck]
  (->> (:cards deck)
       (map :power)
       (filter some?)
       (map #(if (number? %) % 0))
       (reduce + 0)))

(defn calculate-total-amber [deck]
  (->> (:cards deck)
       (map :amber)
       (map #(if (number? %) % 0))
       (reduce + 0)))

(defn validate-deck [deck]
  (let [cards (:cards deck)
        houses (:houses deck)]
    (and
      (= 36 (count cards))
      (= 3 (count houses))
      (every? #(<= 10 % 14) 
        (vals (frequencies (map :house cards)))))))

(defn normalize-house-name [house-str]
  (-> house-str
      str/lower-case
      (str/replace #"\s+" "")
      (str/replace #"[^a-z]" "")
      keyword))

(defn normalize-card-type [type-str]
  (-> type-str
      str/lower-case
      keyword))

(defn normalize-rarity [rarity-str]
  (-> rarity-str
      str/lower-case
      keyword)) 