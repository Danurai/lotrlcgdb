(ns lotrdb.pages
  (:require 
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [hiccup.page :as h]
    [cheshire.core :as cheshire]
    [cemerick.friend :as friend]
    [lotrdb.database :as db]
    [lotrdb.model :as model]))

(load "pages/common")

(def player_type_icons [
  ["Hero" "hero" [:i {:class "fas fa-user"}]]
  ["Ally" "ally" [:i {:class "fas fa-user-friends"}]]
  ["Attachment" "attachment" [:i {:class "fas fa-user-plus"}]]
  ["Event" "event" [:i {:class "fas fa-bolt"}]]
  ])
  
(def sphere_icons
  (map 
    #(let [code (clojure.string/lower-case %)]
      (vector % code [:img.icon-xs {:src (str "/img/icons/sphere_" code ".png")}]))
    ["Leadership","Lore","Spirit","Tactics","Baggins","Fellowship"]))

    
(defn- btngroup [ id array ]
  [:div.btn-group.btn-group-toggle.btn-group-small {:id id :data-toggle "buttons"}
    (for [[name code class] array]
      [:label.btn.btn-outline-secondary {:title name}
        [:input {:type "checkbox" :value code}]
        class])])

(load "pages/admin")

(defn- deck-card [deck]
  [:a.list-group-item.list-group-item-action {:href (str "/deck/edit/" (:uid deck))} 
    [:div.d-flex.justify-content-between
      [:span (:name deck)]
      [:form {:action "/deck/delete" :method "post"}
        [:input#deletedeckuid {:name "deletedeckuid" :hidden true :data-lpignore "true" :value (:uid deck)}]
        [:button.btn.btn-danger.btn-sm {:type "submit" :title "Delete Deck"} "x"]]]])
      
(defn home [ req ]
  (let [user-decks (db/get-user-decks (-> req get-authentications :uid))]
    (h/html5
      pretty-head
      [:body
        (navbar req)
        [:div.container.my-2
          [:div.row.my-1
            [:a.btn.btn-secondary.mr-1 {:href "/deck/new"} "New Deck"]]
          [:div.row.my-1
            [:div.col-md-6
              [:div.row.mb-2 (str "Saved decks (" (count user-decks) ")")]
              [:div.list-group
                (map #(deck-card %) user-decks)]]]]])))
          
(defn packs-page [ req ]
	(let [cards (model/get-cards-with-cycle)]
		(h/html5 
			pretty-head
			[:body  
				(navbar req)
				[:div.container.my-3
					[:ul.list-group
						(for [cycle (sort-by :cycle_position (model/get-cycles))]
							(let [packs (->> (model/get-packs) (filter #(= (:cycle_position %) (:cycle_position cycle))))]
								[:li.list-group-item.list-group-item-action 
									[:div.mb-2.d-flex.justify-content-between 
										[:a {:href (str "/cycle/" (:cycle_position cycle))} (:name cycle)]
										[:span
											[:span.mr-1 
												(str (->> cards (filter #(= (:cycle_position %) (:cycle_position cycle))) (map :quantity) (reduce +))
												" cards")]]]
									(if (< 1 (count packs))
										[:ul.list-group
											(for [pack packs]
												[:li.list-group-item
													[:div.d-flex.justify-content-between
														[:a {:href (str "/pack/" (:code pack))} (:name pack)]
														[:span (str
															(->> cards (filter #(= (:pack_code %) (:code pack))) (map :quantity) (reduce +))
															" cards")]]])])]))]]])))
                  
(defn search-page [ q ]
  (h/html5
    pretty-head
    [:body
      (navbar nil)
      [:div.container.my-3
        [:div.row.mb-2
          [:form.form-inline {:action "/search" :method "GET"}
            [:input.form-control.mr-2 {:type "text" :name "q" :value q}]
            [:button.btn.btn-primary {:role "submit"} "Search"]]]
        [:div.row
          [:table#tblresults.table.table-sm.table-hover
            [:thead [:tr [:th.sortable "Code"][:th.sortable "Name"][:th.sortable "Type"][:th.sortable "Sphere"][:th.sortable "Set"][:th.sortable "qty"]]]
            [:tbody#bodyresults
              (for [card (model/cardfilter (or q ""))]
                [:tr
									[:td (:code card)]
                  [:td [:a.card-link {:data-code (:code card) :href (str "/card/" (:code card))} (:name card)]]
                  [:td (:type_name card)]
                  [:td (:sphere_name card)]
                  [:td (str (:pack_name card) " #" (:position card))]
									[:td (:quantity card)]
									])]]]]]))
                  
(defn- card-icon [ card ]
  [:img.icon-sm.float-right {
    :src (str "/img/icons/"
              (if (some? (:sphere_name card)) 
                  (str "sphere_" (:sphere_code card))
                  (str "pack_" (:pack_code card)))
              ".png")}])
                
(defn card-page [ id ]
  (let [card (->> (model/get-cards-with-cycle) (filter #(= (:code %) id)) first)]
    (h/html5
      pretty-head
      [:body
        (navbar nil)
        [:div.container.my-3
          [:div.row
            [:div.col-sm-6
              [:div.card 
                [:div.card-header
                  [:span.h3.card-title (:name card)]
                  (card-icon card)]
                [:div.card-body
                  [:div.text-center [:b (:traits card)]]
                  [:div {:style "white-space: pre-wrap;"} (:text card)]
                  [:div.mt-1	 [:em {:style "white-space: pre-wrap;"} (:flavor card)]]
                  [:div [:small.text-muted (str (:pack_name card) " #" (:position card))]]]]]
            [:div.col-sm-6
              [:img {:src (or (:cgdbimgurl card) (model/get-card-image-url card))}]]]
          [:div.row
            [:div {:style "white-space: pre-wrap"} (cheshire/generate-string card {:pretty true})]]]])))
            
(defn scenarios-page [req]
	(let [cards (model/get-cards-with-cycle)]
		(h/html5
			pretty-head
			[:body  
				(navbar req)
				[:body
					[:div.container.my-3
						[:ul.list-group
							(for [s (model/get-scenarios)]
								(let [quests (->> cards (filter #(= (:type_code %) "quest")) (filter #(= (:encounter_name %) (:name s))))] 
									[:li.list-group-item 
										[:div.row.justify-content-between
											[:span.h4 (:name s)]
											[:span
												[:span.mr-2.align-middle (-> quests first :pack_name)]
												(card-icon (first quests))]]
										[:div.row
											[:div.col-sm-6
												[:h5 "Quests"]
												(for [q quests]
													[:div [:a.card-link {:href (str "/card/" (:code q)) :data-code (:code q)} (:name q)]])]
											[:div.col-sm-6 
												[:h5 "Encounter Sets"]
												; assumed Encounter set always includes encounter pack with a matching name
												[:div [:a {:href (str "/search?q=n:" (clojure.string/replace (:name s) " " "+"))} (:name s)]]
												(for [e (sort-by :id (:encounters s))]
													(if (not= (:name s) (:name e))
														[:div [:a {:href (str "/search?q=n:" (clojure.string/replace (:name e) " " "+"))} (:name e)]]))]]]))]]]])))
           
(defn get-deck-data [req]
	(let [deck (db/get-user-deck (-> req :params :id))]
		(if (some? deck)
				deck
				(if (some? (-> req :params :deck))
					(let [deck (json/read-str (-> req :params :deck) :key-fn keyword)]
						(assoc deck :data (-> deck :data json/write-str)))
					{}))))
					
(defn deckbuilder [ req ]
  (let [deck (get-deck-data req)]
		(h/html5
			pretty-head
			(h/include-js "/js/externs/typeahead.js")
			[:body  
				(navbar req)
        [:div.container.my-2
          [:div.row.my-1
            [:div.col-sm-6
              [:div.row.my-3
                [:form#save_form.form.needs-validation {:method "post" :action "/deck/save" :role "form" :novalidate true}
                  [:div.form-row.align-items-center
                    [:div.col-auto
                      [:label.sr-only {:for "#deck-name"} "Fellowship Name"]
                      [:input#fellowshipname.form-control {:type "text" :name "fellowshipname" :placeholder "New Fellowship" :required true :value (:name deck) :data-lpignore "true"}]
                      [:div.invalid-feedback "You must name your fellowship"]]
                    [:div.col-auto
                      [:button.btn.btn-warning.mr-2 {:role "submit"} "Save"]
                      [:a.btn.btn-light.mr-2 {:href "/"} "Cancel Edits"]]]
                  [:input#deck-id      {:type "text" :name "deck-id"      :value (:uid deck) :readonly true :hidden true}]
                  [:input#deck-content {:type "text" :name "deck-content" :value (:data deck)  :readonly true :hidden true}]
                  [:input#deck-tags    {:type "text" :name "deck-tags"    :value (:tags deck) :readonly true :hidden true}]
                  [:input#deck-notes   {:type "text"  :name "deck-notes"  :value (:notes deck) :readonly true :hidden true}]]]
              [:div#decklist.row]
            ]
            [:div.col-sm-6
              [:div.row.mb-2.justify-content-between
                (btngroup "type_code" player_type_icons)
                (btngroup "sphere_code" sphere_icons)]
              [:div.row 
                [:div.col-md-12
                  [:div.row 
                    [:input#filtertext.form-control {:type "text"}]]]]
              [:div#info.row]
              [:div.row
                [:table.table.table-sm.table-hover
                  [:thead
                    [:tr 
                      [:th "#"]
                      [:th.sortable "Name"]
                      [:th.sortable.text-center "Type"]
                      [:th.sortable.text-center "Sphere"]
                      [:th.sortable.text-center {:title "Cost/Threat"} "C."]
                      [:th.sortable.text-center {:title "Attack"} "A."]
                      [:th.sortable.text-center {:title "Defense"} "D."]
                      [:th.sortable.text-center {:title "Willpower"} "W."]
                      [:th.sortable.text-center "Set"]]]
                  [:tbody#cardtbl]]]
            ]]]
        [:div#cardmodal.modal {:tab-index -1 :role "dialog"}
          [:div.modal-dialog.modal-sm {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h5.modal-title]
                [:span.buttons]
                [:button.close {:data-dismiss "modal"} "x"]]
              [:div.modal-body]]]]
    (h/include-js "/js/lotrdb_deckbuilder.js?v=1")])))

(defn test-page [req]
  (h/html5
    pretty-head
    [:body 
      (navbar req)
      [:div.container
        [:div.row
          [:table.table.table-sm
            [:thead [:tr [:th "id"][:th "name"][:th "SKU"]]]
            [:tbody 
              (for [pack (sort-by :id (model/get-packs-with-sku))]
                [:tr [:td (:id pack)][:td (:name pack)][:td (:sku pack)]])]]]
        [:div.row
          ; One image per pack
          (for [pack (sort-by :id (model/get-packs))]
            (let [card (->> (model/get-cards) (filter #(= (:pack_code %) (:code pack))) first)]
              (if (some? card)
                [:img.img-fluid {
                  :src (model/get-card-image-url card :small)
                  :alt (str (:id pack) ": " (:pack_name card))
                  :title (str (:id pack) ": " (:pack_name card) " - " (:name card))}])))]
      ]]))
      
(defn allcards [req]
  (h/html5 
    pretty-head
    [:body
      (navbar req)
      [:div.container.my-3
        [:div.row-fluid.mb-3
          [:label "TAFFY Filter"]
          [:input#filter.form-control {:type "text"}]]
        [:div.row-fluid
          [:small#resultsummary.row-fluid.float-right.text-muted ]
          [:table.table.table-sm.table-hover
            [:thead [:tr [:th "Code"][:th "Name"][:th "Type"][:th "Set"][:th "Number"][:th "info"]]]
            [:tbody#cardlist]]]]
      (h/include-js "/js/cardbrowser.js?v=1")
    ]))