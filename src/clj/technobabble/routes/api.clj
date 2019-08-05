(ns technobabble.routes.api
  (:require [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth.backends.token :refer [token-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [compojure.api.meta :refer [restructure-param]]
            [compojure.api.sweet :refer [defapi context PATCH POST GET PUT DELETE]]
            [technobabble.middleware :refer [token-auth-mw]]
            [technobabble.routes.api.auth :as auth]
            [technobabble.routes.api.message :as message]
            [ring.util.http-response :refer :all]
            [schema.core :as s]))

;;;; Access handlers and wrappers

(defn access-error [_ _]
  (unauthorized {:error "unauthorized"}))

(defn wrap-restricted [handler rule]
  (restrict handler {:handler  rule
                     :on-error access-error}))

(defmethod restructure-param :auth-rules
  [_ rule acc]
  (update-in acc [:middleware] conj [`wrap-restricted rule]))

(defmethod restructure-param :auth-data
  [_ binding acc]
  (update-in acc [:letks] into [binding `(:identity ~'+compojure-api-request+)]))

;;;; Schemas

(s/defschema Reminder
  {:id                        s/Uuid
   :type-id                   s/Str
   :message-id                s/Uuid
   :created                   s/Inst
   :next-date                 (s/maybe s/Inst)
   :properties                s/Any
   (s/optional-key :username) s/Str
   (s/optional-key :message)  s/Str                         ; Returned when querying for pending reminders
   })

(s/defschema Message
  {:id                         s/Uuid
   :username                   s/Str
   :message                    s/Str
   :created                    s/Inst
   :archived?                  s/Bool
   (s/optional-key :root-id)   (s/maybe s/Uuid)
   (s/optional-key :follow-id) (s/maybe s/Uuid)
   (s/optional-key :status)    s/Keyword
   (s/optional-key :reminders) [Reminder]})

(s/defschema MessageCluster
  {:id       s/Uuid
   :username s/Str
   :created  s/Inst})

(s/defschema MessageSearchResult
  {:total        s/Int
   :pages        s/Int
   :current-page s/Int
   :results      [Message]})

(s/defschema ThreadResult
  {:id      s/Uuid
   :results [Message]})

;;;; Services

(defapi service-routes
  {:swagger {:ui   "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version     "1.0.0"
                           :title       "technobabble API"
                           :description "Signup and data access"}}}}

  (context "/api/auth" []
    :tags ["AUTH"]

    (POST "/login" []
      :return s/Str
      :body-params [username :- s/Str
                    password :- s/Str]
      :summary "Attempts to validate a username and password, and returns a token"
      (auth/login username password))

    (GET "/validate" []
      :return s/Str
      :header-params [authorization :- String]
      :middleware [token-auth-mw]
      :auth-rules authenticated?
      :auth-data auth-data
      :summary "Attempts to validate a token, and echoes it if valid"
      ;; You'll notice I don't actually do any validation here. This is
      ;; because the validation and the authentication verification are
      ;; the same. If we got this far, the token is valid.
      (ok (:token auth-data)))

    (POST "/signup" []
      :return s/Str
      :body-params [username :- s/Str
                    password :- s/Str
                    {password2 :- s/Str ""}]
      :summary "Creates a new user"
      ;; Returns an authentication token
      (auth/signup! username password)))

  (context "/api" []
    :tags ["THOUGHTS"]

    ;; You'll need to be authenticated for these
    :middleware [token-auth-mw]
    :auth-rules authenticated?
    :header-params [authorization :- s/Str]

    (GET "/messages/:id" []
      :summary "Gets a message"
      :path-params [id :- s/Uuid]
      :return Message
      :auth-data auth-data
      (message/get-message (:username auth-data) id))

    (POST "/messages" []
      :summary "Creates a new message"
      :return Message
      :body-params [message :- s/Str
                    {follow-id :- (s/maybe s/Uuid) nil}]
      :auth-data auth-data
      (message/save-message (:username auth-data) message follow-id))))