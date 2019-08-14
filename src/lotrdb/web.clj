(ns lotrdb.web
   (:require 
    [clojure.java.io :as io]
    [clojure.data.json :as json]
    [compojure.core :refer [context defroutes GET ANY POST]]
    [compojure.route :refer [resources]]
    [ring.util.response :refer [response content-type redirect]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.session :refer [wrap-session]]
    [cemerick.friend :as friend]
    (cemerick.friend [workflows :as workflows]
                   [credentials :as creds])
    [hiccup.page :as h]
    [lotrdb.database :as db]
    [lotrdb.pages :as pages]
    [lotrdb.model :as model]
    ))
  
(defn- alert [ type message ]
  (reset! model/alert {:type type :message message}))

(defn- save-deck-handler [ id name content tags notes req ]
  (db/save-deck id name content tags notes (-> req pages/get-authentications :uid))
  (alert "alert-info" (str "Deck " name " saved"))
  (redirect "/"))
  
(defn- delete-deck-handler [ deletedeckuid ]
  (db/delete-deck deletedeckuid)
  (alert "alert-warning" "Deck deleted.")
  (redirect "/"))

(defroutes deck-routes
  (GET "/new" []
    pages/deckbuilder)
  (GET "/edit" []
    pages/deckbuilder)
  (GET "/edit/:id" []
    pages/deckbuilder)
  (POST "/save" [deck-id deck-name deck-content deck-tags deck-notes]  
    (friend/wrap-authorize 
      #(save-deck-handler deck-id deck-name deck-content deck-tags deck-notes %)
      #{::db/user}))
  (POST "/delete" [deletedeckuid]
    (friend/wrap-authorize 
      (delete-deck-handler deletedeckuid)
      #{::db/user})))
      
(defroutes admin-routes
  (GET "/" []
    pages/useradmin)
  (POST "/updatepassword" [uid password]
    (db/updateuserpassword uid password)
    (alert "alert-info" "Password updated")
    (redirect "/admin"))
  (POST "/updateadmin" [uid admin]
    (db/updateuseradmin uid (some? admin))
    (alert "alert-info" (str "Admin status " (if (some? admin) "added" "removed")))
    (redirect "/admin"))
  (POST "/adduser" [username password admin]
    (db/adduser username password (= admin "on"))
    (alert "alert-info" (str "User Account created for " username))
    (redirect "/admin"))
  (POST "/deleteuser" [uid]
    (alert "alert-warning" "User Account Deleted")
    (db/dropuser uid)
    (redirect "/admin")))
    
(defroutes app-routes
  (GET "/" [] 
    ;(friend/wrap-authorize pages/home #{::db/user}))
		pages/home)
  (GET "/packs" []
    pages/packs-page)
  (GET "/scenarios" []
    pages/scenarios-page)
  (GET "/search" [ q ]
    (pages/search-page q))
  (GET "/cycle/:id" [ id ]
    (pages/search-page (str "y:" id)))
  (GET "/pack/:id" [ id ]
    (pages/search-page (str "e:" id)))
  (GET "/card/:id" [ id ]
    (pages/card-page id))
;; TODO
  (context "/deck" []
   ; (friend/wrap-authorize deck-routes #{::db/user}))
		deck-routes)
  (GET "/login" []
    pages/login)
  (context "/admin" []
    (friend/wrap-authorize admin-routes #{::db/admin}))
  (POST "/register" [username password]
    (db/adduser username password false)
    (redirect "/"))
  (POST "/checkusername" [username] 
    (response (str (some #(= (clojure.string/lower-case username) (clojure.string/lower-case %)) (map :username (db/get-users))))))
  (friend/logout
    (ANY "/logout" [] (redirect "/")))
	(GET "/api/data/:id" [id] 
		(-> (model/api-data id)
				json/write-str
				response
				(content-type "application/json")))
  (GET "/test" []
    pages/test-page)
  (resources "/"))
  
(def app
  (-> app-routes
     (friend/authenticate
      {:allow-anon? true
       :login-uri "/login"
       :default-landing-uri "/"
       :unauthorised-handler (h/html5 [:body [:div.h5 "Access Denied " [:a {:href "/"} "Home"]]])
       :credential-fn #(creds/bcrypt-credential-fn (db/users) %)
       :workflows [(workflows/interactive-form)]})
    (wrap-keyword-params)
    (wrap-params)
    (wrap-session)))