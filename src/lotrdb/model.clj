(ns lotrdb.model
  (:require 
    [clojure.java.io :as io]
    [clojure.data.json :as json]
		[lotrdb.database :as db]))

(def alert (atom nil))

(def cgdb-pack-name {
  "HfG"  "thfg"
  "JtR"  "ajtr"
  "HoEM" "thoem"
  "WitW" "twitw"
  "BoG"  "tbog"
  "Starter" "core"
  "TiT" "trouble-in-tharbad"
})

(def sku-code { ; id (MEC)sku
  1  1                                ; Core
  2  2  3  3  4  4  5  5  6  6  7  7   ; Shadows of Mirkwood
  8  8                                ; Khazad-dum
  9  9  10 10 11 11 12 12 13 13 14 14   ; Dwarrowdelf
  15 17                               ; Heirs of Numenor
  16 18 17 19 18 20 19 21 20 22 21 23   ; Against the Shadow
  22 25                               ; Voice of Isengard/
  23 26 24 27 25 28 26 29 27 30 28 31   ; Ring-maker
  29 38                               ; The Lost Realm
  30 39 31 40 32 41 33 42 34 43 35 44   ; Angmar Awakened
  36 47                               ; The Grey Havens
  37 48 40 34 41 45 42 46 43 48 44 49   ; Dream-chaser
  38 16 39 24                         ; Saga - The Hobbit 
  45 50 46 51 47 54 48 52 49 53 55 62   ; Saga - The Lord of the Rings
  53 55                               ; The Sands of Harad
  50 56 51 57 52 58 54 59 56 60 57 61   ; Haradrim
  58 65                               ; The Wilds of Rhovanion
  59 66 60 67 62 68 63 69 65 70         ; Ered Mithrin
  61 73                                ; 2 player Ltd Collectors Edition Starter
  64 99 ; PoD
})
    
(defn get-cycles [] 
  (-> "private/lotrdb_data_cycles.json"
      io/resource
      slurp
      (json/read-str :key-fn keyword)))
			
(defn get-packs [] 
  (-> "private/ringsdb-api-public-packs.json"
      io/resource
      slurp
      (json/read-str :key-fn keyword)))
			
(defn get-packs-with-sku []
  (->> (get-packs)
       (map (fn [p]
              (assoc p :sku (str "MEC" (format "%02d" (get sku-code (:id p)))))))))
							
(defn get-cards [] 
  (-> "private/lotrdb_data_cards.json"
      io/resource
      slurp
      (json/read-str :key-fn keyword)))
      
(defn get-cards-with-cycle []
  (let [positions (reduce merge (map #(hash-map (:code %) (:cycle_position %)) (get-packs)))
				cycles (get-cycles)]
    (map (fn [c] 
					(let [pos (get positions (:pack_code c))
								cycle (->> cycles (filter #(= (:cycle_position %) pos)) first)]
						(assoc c :cycle_position pos
										 :cycle_name (:name cycle)
										 )))
      (get-cards))))
			
(defn get-scenarios []
  (-> "private/ringsdb-api-public-scenarios.json"
      io/resource
      slurp
      (json/read-str :key-fn keyword)))
	
(defn api-data [ id ]
	(case id
		"cards" (get-cards-with-cycle)
		"packs" (get-packs-with-sku)
		"cycles" (get-cycles)
		"scenarios" (get-scenarios)
		{:status "Not Found"}))
		
(defn- normalize-name [ name ]
  (-> name
    clojure.string/lower-case
    (clojure.string/replace #"\s" "-")
    (clojure.string/replace #"\'|\!" "")
    (clojure.string/replace "\u00e1" "a")
    (clojure.string/replace "\u00e2" "a")
    (clojure.string/replace "\u00e4" "a")
    (clojure.string/replace "\u00e9" "e")
    (clojure.string/replace "\u00ed" "i")
    (clojure.string/replace "\u00f3" "o")
    (clojure.string/replace "\u00fa" "u")))
    
      
(defn- cgdb-card-name [ card ]
  (let [pack (->> (get-packs-with-sku) (filter #(= (:code %) (:pack_code card))) first)]
    (cond
      (some #(= (:id pack) %) (set (apply merge (range 1 23) [37 38 39 61])))
        (str 
          (normalize-name (:name card))
          "-"
          (cgdb-pack-name (:pack_code card) (clojure.string/lower-case (:pack_code card))))
      (= (:id pack) 23)
        (str 
          (normalize-name (:name card))
          "_"
          (normalize-name (:pack_name card))
          "_"
          (:position card)) 
      (< 23 (:id pack) 26)
        (str 
          (normalize-name (:name card))
          "-"
          (normalize-name (:pack_name card))
          "-"
          (:position card)) 
      (= (:id pack) 40)
        (str (:sku pack) "_" (format "%03d" (:position card)))
      :else ;(< 25 (:id pack))
        (str (:sku pack) "_" (:position card))
      )))
          
(defn get-card-image-url 
  ([ card size ]
      (str "http://www.cardgamedb.com/forums/uploads/lotr/"
          (if (= size :small) "tn_" "ffg_")
          (cgdb-card-name card)
          ".jpg"))
  ([ card ] 
    (get-card-image-url card :normal)))
          
      
;;;;;;;;;;;;
;; FILTER ;;
;;;;;;;;;;;;
(def find-regex #".*?(?=\s[a-z]:)|.+")
(def field-regex #"([a-z]):(.+)")
(def filter-synonyms {
  :type_code {"h" "hero" "a" "ally" "e" "event" "t" "attachment"}
  :sphere_code {"l" "leadership" "o" "lore" "s" "spirit" "t" "tactics" "n" "neutral" "f" "fellowship"}
})

(defn fmap [qry]
"returns a collection of maps including {:id 'field name' :val 'match')" 
  (map #(let [field-flt  (->> % (re-seq field-regex) first)
             field-name (case (get field-flt 1)
                            "e" :pack_code
														"n" :encounter_name
                            "y" :cycle_position
                            "t" :type_code
                            "s" :sphere_code
                            "r" :traits
                            "x" :text
                            :name)
             field-val  (get field-flt 2)]
          {
            :id field-name
            :val (or 
                  (case field-name 
                       (:cycle_position) (read-string field-val) 
                       field-val)
                  %)
          })
    (->> qry (re-seq find-regex) (remove clojure.string/blank?))
  )
)

(defn cardfilter [q]
  (sort-by :code
    (reduce
      (fn [data {:keys [id val]}]
        (case id
          (:name :text :traits) 
            (filter #(some? (re-find (re-pattern (str "(?i)" val)) (id % ""))) data) ; partial match
          (:pack_code :type_code :sphere_code) 
            (filter 
              (fn [x] 
                (some 
                  #(= (id x) (get-in filter-synonyms [id %] %)) 
                  (clojure.string/split val #"\|")))
              data)
          (filter #(= (id %) val) data)))
      (get-cards-with-cycle)
      (fmap q))))