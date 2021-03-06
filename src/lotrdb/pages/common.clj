(in-ns 'lotrdb.pages)

(defn get-authentications [req]
  (#(-> (friend/identity %) :authentications (get (:current (friend/identity %)))) req))

(def pretty-head
  [:head
  ;; Meta Tags
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
  ;; icon
    [:link {:rel "icon" :href "/img/lotrdb.ico"}]
  ;; jquery
    [:script {
      :src "https://code.jquery.com/jquery-3.3.1.min.js" 
      :integrity "sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8=" 
      :crossorigin "anonymous"}]
  ;; popper tooltip.js
    [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/tooltip.js/1.3.1/umd/tooltip.min.js" :integrity "sha256-5hYn1dYaPW5VRitzMTQ8UsMvqSPqCiqwtQbT77tyEso=" :crossorigin="anonymous"}]
    [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.6/umd/popper.min.js" :integrity "sha256-WHwIASWxNdKakx7TceUP/BqWQYMcEIfeLNdFMoFfRWA=" :crossorigin "anonymous"}]
  ;; Bootstrap  
    [:link {:rel "stylesheet" :href "https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css" :integrity "sha384-MCw98/SFnGE8fJT3GXwEOngsV7Zt27NXFoaoApmYm81iuXoPkFOJwJ8ERdknLPMO" :crossorigin "anonymous"}]
    [:script {:src "https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/js/bootstrap.min.js" :integrity "sha384-ChfqqxuZUCnJSK3+MXmPNIyE6ZbWh2IMqE241rYiqJxyMiZ6OW/JmZQ5stwEULTy" :crossorigin "anonymous"}]
  ;; Bootstrap Select
  ;  [:link {:rel "stylesheet" :href "https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.8/css/bootstrap-select.css" :integrity "sha256-OejstTtgpWqwtX/gwUbICEQz8wbdVWpVrCwqZ29apg4=" :crossorigin "anonymous"}]
  ;  [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.8/js/bootstrap-select.js" :integrity "sha256-/X1l5JQfBqlJ1nW6V8EhZJsnDycL6ocQDWd531nF2EI=" :crossorigin "anonymous"}]
  ;; Font Awesome
    [:script {:defer true :src "https://use.fontawesome.com/releases/v5.0.13/js/all.js"}]
  ;; TAFFY JQuery database
    [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/taffydb/2.7.2/taffy-min.js"}]
  ;; font 
    [:link {:href "https://fonts.googleapis.com/css?family=Eczar" :rel "stylesheet"}]
  ;; Site Specific
    (h/include-css "/css/lotrdb_style.css?v=1")
    (h/include-js "/js/popover.js?v=1")
    (h/include-js "/js/tablesort.js?v=1")
    ])
    
(defn navbar [req]
  [:nav.navbar.navbar-expand-lg.navbar-dark.bg-dark
    [:div.container
    ;; Home Brand with Icon
      [:a.navbar-brand.mb-0.h1 {:href "/"}
        [:img.icon-sm.mr-1 {:src "/img/icons/sphere_fellowship.png"}] "LotR DB"]
    ;; Collapse Button for smaller viewports
      [:button.navbar-toggler {:type "button" :data-toggle "collapse" :data-target "#navbarSupportedContent" 
                            :aria-controls "navbarSupportedContent" :aria-label "Toggle Navigation" :aria-expanded "false"}
        [:span.navbar-toggler-icon]]
    ;; Collapsable Content
      [:div#navbarSupportedContent.collapse.navbar-collapse
    ;; List of Links
        [:ul.navbar-nav.mr-auto
          [:li.nav-item [:a.nav-link {:href "/packs"} "Packs"]]
          [:li.nav-item [:a.nav-link {:href "/scenarios"} "Scenarios"]]
          [:li.nav-item [:a.nav-link {:href "/search"} "Search"]]
          ]
    ;; Login Icon
          [:span.nav-item.dropdown
            [:a#userDropdown.nav-link.dropdown-toggle.text-white {:href="#" :role "button" :data-toggle "dropdown" :aria-haspopup "true" :aria-expanded "false"}
              [:i.fas.fa-user]]
              (if-let [identity (friend/identity req)]
                [:div.dropdown-menu {:aria-labelledby "userDropdown"}
                  (if (friend/authorized? #{::db/admin} (friend/identity req))
                    [:a.dropdown-item {:href "/admin"} "Admin Console"])
                  [:a.dropdown-item {:href "/logout"} "Logout"]]
                [:div.dropdown-menu {:aria-labelledby "userDropdown"}
                  [:a.dropdown-item {:href "/login"} "Login"]])]]]])
                  
(defn show-alert []
  (let [type (-> @model/alert :type)
        msg  (-> @model/alert :message)]
    (when (some? type)
      (reset! model/alert {})
      [:div.alert.alert-dismissible.fade.show {:class type :role "alert"} msg
        [:button.close {:type"button" :data-dismiss "alert" :aria-label "Close"} [:span {:aria-hidden "true"} "&#10799;"]]])))