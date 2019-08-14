(ns lotrdb.tests
  (:require 
    [expectations :refer :all]
    [lotrdb.model :as model]))
  
(expect "http://www.cardgamedb.com/forums/uploads/lotr/ffg_aragorn-core.jpg"
  (model/get-card-image-url (first (model/get-cards))))
  
(expect [{:id :text :val "test"}]
  (model/fmap "x:test"))
  
(expect [{:id :name :val "Aragorn"}]
  (model/fmap "Aragorn"))

(expect "lore"
  (get-in model/filter-synonyms [:sphere_code "o"]))
  
(expect "hero"
  (get-in model/filter-synonyms [:type_code "h"]))
  
 
(expect #(< 1 %)
  (-> (model/cardfilter "s:o")
      count))
(expect #(< 1 %)
  (-> (model/cardfilter "x:ready")
      count))
  
(expect 7
  (-> (model/cardfilter "Aragorn")
      count))
     
(expect [{:id :cycle_position :val 1}]
  (model/fmap "y:1"))
      
(expect 73
  (-> (model/cardfilter "y:1")
      count))
            
(expect 1
  (-> (model/get-cards-with-cycle)
      first 
      :cycle_position))