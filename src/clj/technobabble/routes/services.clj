(ns technobabble.routes.services
  (:require
   [technobabble.routes.services.auth :as auth]
   [ring.util.http-response :refer :all]
   [compojure.api.sweet :refer [defapi context PATCH POST GET PUT DELETE]]
   [schema.core :as s]))

(s/defschema UserRegistration
  {:username String
   :password String
   :password-confirm String})

(s/defschema Result
  {:result s/Keyword
   (s/optional-key :message) String})

(defapi service-routes
  {:swagger {:ui   "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version     "1.0.0"
                           :title       "Technobabble API"
                           :description "Signup and data access"}}}}

  (context "/api/auth" []
    :tags ["AUTH"]

    (POST "/register" req
      :return Result
      :body [user UserRegistration]
      :summary "Registers a new user"
      (auth/register! req user))))
