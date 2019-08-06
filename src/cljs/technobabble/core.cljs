(ns technobabble.core
  (:require [bidi.bidi :as bidi]
            [chord.client :refer [ws-ch]]
            [cljsjs.react-bootstrap]
            [cljs.core.async :as async :include-macros true]
            [jayq.core :refer [$]]
            [technobabble.handlers.cache]
            [technobabble.handlers.message]
            [technobabble.handlers.routing :as r]
            [technobabble.handlers.ui-state]
            [technobabble.helpers :as helpers :refer [<sub]]
            [technobabble.ws-chat :as ws-chat]
            [pushy.core :as pushy]
            [reagent.cookies :as cookies]
            [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [dispatch reg-sub reg-event-db subscribe dispatch-sync]]
            [taoensso.timbre :as timbre
             :refer-macros [log trace debug info warn error fatal report
                            logf tracef debugf infof warnf errorf fatalf reportf
                            spy get-env]])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [technobabble.misc.cljs-macros :refer [adapt-bootstrap]]))

;;;; Data and helpers

(adapt-bootstrap Button)
(adapt-bootstrap ButtonGroup)
(adapt-bootstrap DropdownButton)
(adapt-bootstrap MenuItem)
(adapt-bootstrap OverlayTrigger)
(adapt-bootstrap Popover)
(adapt-bootstrap Tooltip)
(adapt-bootstrap Navbar)
(adapt-bootstrap Navbar.Header)
(adapt-bootstrap Navbar.Brand)
(adapt-bootstrap Navbar.Toggle)
(adapt-bootstrap Navbar.Collapse)
(adapt-bootstrap Nav)
(adapt-bootstrap NavItem)
(def Modal (reagent/adapt-react-class js/ReactBootstrap.Modal))
(def ModalBody (reagent/adapt-react-class js/ReactBootstrap.ModalBody))
(def ModalFooter (reagent/adapt-react-class js/ReactBootstrap.ModalFooter))

(defn clear-cookies-and-reload
  []
  (.reload js/window.location true)
  (cookies/clear!))

;;;; Queries

(defn general-query
  [db [sid element-id]]
  (get-in db [sid element-id]))

(reg-sub :cache general-query)
(reg-sub :ui-state general-query)
(reg-sub :credentials general-query)

;;;; Handlers

(reg-event-db
 :initialize
 (fn [app-state _]
   (merge app-state {:ui-state {:wip-login?      false
                                :section         :chat}
                     :cache    {}                          ; Will be used for caching messages
                     :note     {:edit-message nil}})))

;;;; Components

(def initial-focus-wrapper
  (with-meta identity
    {:component-did-mount #(.focus (reagent/dom-node %))}))

(defn navbar-item
  "Renders a navbar item. Having each navbar item have its own subscription will probably
  have a bit of overhead, but I don't imagine it'll be anything major since we won't have
  more than a couple of them.

  It will use the section id to get the route to link to."
  [label section]
  (let [current     (subscribe [:ui-state :section])
        is-current? (reaction (= section @current))
        class       (when @is-current? "active")]
    [NavItem {:class    class
              :eventKey section
              :href     (bidi/path-for r/routes section)}
     label
     (when @is-current?
       [:span {:class "sr-only"} "(current)"])]))

(defn navbar []
  [Navbar {:collapseOnSelect true
           :fixedTop         true}
   [Navbar.Header
    [:a {:href "/"}
     [Navbar.Brand "Technobabble"]]
    [Navbar.Toggle]]
   [Navbar.Collapse
    (if (nil? (<sub [:credentials :token]))
      [Nav
       [navbar-item "Login" :login]
       [navbar-item "Sign Up" :signup]]
      [Nav
       [navbar-item "Chat" :chat]
       [NavItem {:href "/" :class "nav-link" :on-click #(clear-cookies-and-reload)} "Logout"]])]])

(defn alert []
  (let [msg (<sub [:ui-state :last-message])]
    (when (not-empty (:text msg))
      [:div {:class (str "alert " (:class msg))}
       [:button {:type :button :class "close" :on-click #(dispatch [:state-message ""])} "x"]
       (:text msg)])))

(defn panel [title msg class]
  [:div {:class (str "panel " class)}
   [:div {:class "panel-heading"}
    [:h3 {:class "panel-title"} title]]
   [:div {:class "panel-body"} msg]])

(defn dispatch-on-press-enter [e d]
  (when (= 13 (.-which e))
    (dispatch d)))

(defn login-form []
  (let [username  (<sub [:credentials :username])
        password  (<sub [:credentials :password])
        confirm   (<sub [:credentials :password2])
        message   (<sub [:credentials :message])
        section   (<sub [:ui-state :section])
        signup?   (reaction (= :signup section))
        u-class   (reaction (if (and @signup? (empty? username)) " has-error"))
        pw-class  (reaction (if (and @signup? (> 5 (count password))) " has-error"))
        pw2-class (reaction (if (not= password confirm) " has-error"))]
    [:div {:class "modal"}
     [:div {:class "modal-dialog"}
      [:div {:class "modal-content"}
       [:div {:class "modal-header"}
        [:h4 {:class "modal-title"} (if (= :signup section) "Sign Up" "Login")]]
       [:div {:class "modal-body"}
        (when message
          [:div {:class (str "col-lg-12 alert " (:type message))}
           [:p (:text message)]])
        [:div {:class (str "form-group" @u-class)}
         [:div {:class "col-sm-10"}
          [:input {:type         "text"
                   :class        "form-control col-sm-8"
                   :id           "inputLogin"
                   :placeholder  "Username"
                   :on-change    #(dispatch-sync [:state-credentials :username (-> % .-target .-value)])
                   :on-key-press #(dispatch-on-press-enter % [:auth-request @signup?])
                   :value        username}]]]
        [:div {:class (str "form-group" @pw-class)}
         [:div {:class "col-sm-10"}
          [:input {:type         "password"
                   :class        "form-control col-sm-8"
                   :id           "inputPassword"
                   :placeholder  "Password"
                   :on-change    #(dispatch-sync [:state-credentials :password (-> % .-target .-value)])
                   :on-key-press #(dispatch-on-press-enter % [:auth-request @signup?])
                   :value        password}]]]
        (if @signup?
          [:div {:class (str "form-group" @pw2-class)}
           [:div {:class "col-sm-10 col-lg-10"}
            [:input {:type         "password"
                     :class        "form-control col-sm-8 col-lg-8"
                     :id           "inputPassword2"
                     :placeholder  "Confirm password"
                     :on-change    #(dispatch-sync [:state-credentials :password2 (-> % .-target .-value)])
                     :on-key-press #(dispatch-on-press-enter % [:auth-request @signup?])
                     :value        confirm}]]])]
       [:div {:class "modal-footer"}
        [:button {:type "button" :class "btn btn-primary" :disabled (<sub [:ui-state :wip-login?]) :on-click #(dispatch [:auth-request @signup?])} "Submit"]]]]]))

(defn content-section []
  (let [section (subscribe [:ui-state :section])
        token   (subscribe [:credentials :token])]
    (when (and (nil? @token)
               (not= :login @section)
               (not= :signup @section))
      (dispatch-sync [:state-ui-section :login]))
    (case @section
      :chat [ws-chat/app-container]
      [login-form])))

;;; Initialize app

(defn mount-components []
  (reagent/render-component [navbar] (.getElementById js/document "navbar"))
  (reagent/render-component [content-section] (.getElementById js/document "content-section")))

(defn init! []
  (timbre/set-level! :debug)
  (pushy/start! r/history)
  (dispatch-sync [:initialize])
  (dispatch-sync [:auth-set-token (cookies/get :token nil)])
  (dispatch-sync [:auth-validate])
  (mount-components))