(ns technobabble.core
  (:require [bidi.bidi :as bidi]
            [chord.client :refer [ws-ch]]
            [clojure.string :refer [trim split]]
            [cljsjs.react-bootstrap]
            [cljs.core.async :as async :include-macros true]
            [jayq.core :refer [$]]
            [markdown.core :refer [md->html]]
            [technobabble.handlers.auth :refer [clear-token-on-unauth]]
            [technobabble.handlers.cache]
            [technobabble.handlers.cluster]
            [technobabble.handlers.memory]
            [technobabble.handlers.routing :as r]
            [technobabble.handlers.ui-state]
            [technobabble.handlers.thread]
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

;;;;------------------------------
;;;; Data and helpers
;;;;------------------------------

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

(defn find-dom-elem
  "Find a dom element by its id. Expects a keyword."
  [id]
  (first ($ id)))

(def top-div-target (find-dom-elem :#header))

;;;;------------------------------
;;;; Queries
;;;;------------------------------

(defn general-query
  [db [sid element-id]]
  (get-in db [sid element-id]))

(reg-sub :note general-query)
(reg-sub :cache general-query)
(reg-sub :ui-state general-query)
(reg-sub :search-state general-query)
(reg-sub :credentials general-query)

;;;;------------------------------
;;;; Handlers
;;;;------------------------------

(reg-event-db
 :initialize
 (fn [app-state _]
   (merge app-state {:ui-state {:is-busy?        false
                                :wip-login?      false
                                :show-thread?    false
                                :section         :chat
                                :current-query   ""
                                :query-all?      false
                                :results-page    0
                                :memories        {:pages 0}
                                :show-reminders? false
                                :is-searching?   false}
                     :cache    {}                          ; Will be used for caching threads and reminders
                     :note     {:edit-memory nil}})))

;;;;------------------------------
;;;; Components
;;;;------------------------------


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
       [navbar-item "Sign up" :signup]]
      [Nav
       [navbar-item "Chat" :chat]
       [NavItem {:href "/" :class "nav-link" :on-click #(cookies/clear!)} "Logout"]])]])

(defn alert []
  (let [msg (<sub [:ui-state :last-message])]
    (when (not-empty (:text msg))
      [:div {:class (str "alert " (:class msg))}
       [:button {:type :button :class "close" :on-click #(dispatch [:state-message ""])} "x"]
       (:text msg)])))

(defn thought-edit-box [note-id]
  [:div {:class "form-group"}
   [focused-thought]
   [:div {:class "col-sm-12"}
    [initial-focus-wrapper
     [:textarea {:class       "form-control"
                 :id          "thought-area"
                 :placeholder "I was thinking..."
                 :rows        12
                 :style       {:font-size "18px"}
                 :on-change   #(dispatch-sync [:state-note note-id (-> % .-target .-value)])
                 :value       (<sub [:note note-id])}]]]])

(defn panel [title msg class]
  [:div {:class (str "panel " class)}
   [:div {:class "panel-heading"}
    [:h3 {:class "panel-title"} title]]
   [:div {:class "panel-body"} msg]])

(defn dispatch-on-press-enter [e d]
  (when (= 13 (.-which e))
    (dispatch d)))

(defn memory-query []
  (let [archived? (<sub [:ui-state :query-all?])
        tooltip   (reagent/as-element [Tooltip {:id :archived?} "Include archived thoughts"])]
    [:div {:class "form-horizontal"}
     [:div {:class "form-group"}
      [:label {:for "input-search" :class "col-md-1 control-label"} "Search:"]
      [:div {:class "col-md-9"}
       [initial-focus-wrapper
        [:input {:type      "text"
                 :class     "form-control"
                 :id        "input-search"
                 :value     (<sub [:ui-state :current-query])
                 :on-change #(dispatch-sync [:state-current-query (-> % .-target .-value)])}]]]
      [:div {:class "col-md-2"}
       [OverlayTrigger
        {:placement :left
         :overlay   tooltip}
        [:div {:class "checkbox"}
         [:label
          [:input {:type     "checkbox"
                   :checked  archived?
                   :on-click #(dispatch-sync [:state-query-all? (not archived?)])}]
          [:i {:class "fa icon-margin-both fa-archive fa-lg fa-6x"}]]]]]]]))

(defn memory-load-trigger []
  (let [total-pages (reaction (:pages (<sub [:search-state :last-result])))]
    (when (< (<sub [:search-state :page-index])
             (dec @total-pages))
      [:div {:style {:text-align "center"}}
       (if (<sub [:ui-state :is-searching?])
         [:i {:class "fa fa-spinner fa-spin"}]
         [:i {:class "fa fa-ellipsis-h" :id "load-trigger"}])])))

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
        [:h4 {:class "modal-title"} "Login"]]
       [:div {:class "modal-body"}
        (when message
          [:div {:class (str "col-lg-12 alert " (:type message))}
           [:p (:text message)]])
        [:div {:class (str "form-group" @u-class)}
         [:label {:for "inputLogin" :class "col-sm-2 control-label"} "Username"]
         [:div {:class "col-sm-10"}
          [:input {:type         "text"
                   :class        "formControl col-sm-8"
                   :id           "inputLogin"
                   :placeholder  ""
                   :on-change    #(dispatch-sync [:state-credentials :username (-> % .-target .-value)])
                   :on-key-press #(dispatch-on-press-enter % [:auth-request @signup?])
                   :value        username}]]]
        [:div {:class (str "form-group" @pw-class)}
         [:label {:for "inputPassword" :class "col-sm-2 control-label"} "Password"]
         [:div {:class "col-sm-10"}
          [:input {:type         "password"
                   :class        "formControl col-sm-8"
                   :id           "inputPassword"
                   :on-change    #(dispatch-sync [:state-credentials :password (-> % .-target .-value)])
                   :on-key-press #(dispatch-on-press-enter % [:auth-request @signup?])
                   :value        password}]]]
        (if @signup?
          [:div {:class (str "form-group" @pw2-class)}
           [:label {:for "inputPassword2" :class "col-sm-2 col-lg-2 control-label"} "Confirm:"]
           [:div {:class "col-sm-10 col-lg-10"}
            [:input {:type         "password"
                     :class        "formControl col-sm-8 col-lg-8"
                     :id           "inputPassword2"
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
    ;; Only renders the section, any dispatch should have happened on the :state-ui-section handler
    (case @section
      :chat [ws-chat/app-container]
      :remember [memory-list]
      [login-form])))

(defn header []
  (let [state (subscribe [:ui-state :section])
        label (case @state
                :chat "Chat room"
                :remember "Remember"
                "")]
    (if (not-empty label)
      [:h1 {:id "forms"} label])))

;;; -------------------------
;;; Initialize app

(defn mount-components []
  (reagent/render-component [navbar] (.getElementById js/document "navbar"))
  (reagent/render-component [content-section] (.getElementById js/document "content-section"))
  (reagent/render-component [header] (.getElementById js/document "header")))

(defn init! []
  (timbre/set-level! :debug)
  (pushy/start! r/history)
  (dispatch-sync [:initialize])
  (dispatch-sync [:auth-set-token (cookies/get :token nil)])
  (dispatch-sync [:auth-validate])
  (mount-components))