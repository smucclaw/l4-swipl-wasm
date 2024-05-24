(ns l4-lp.web-editor.core
  (:require ["@mui/icons-material/Send$default" :as SendIcon]
            ["@mui/icons-material/ExpandMore$default" :as ExpandMoreIcon]
            ["@mui/material/Accordion$default" :as Accordion]
            ["@mui/material/AccordionDetails$default" :as AccordionDetails]
            ["@mui/material/AccordionSummary$default" :as AccordionSummary]
            ["@mui/material/Box$default" :as Box]
            ["@mui/material/Button$default" :as Button]
            ["@mui/material/Unstable_Grid2$default" :as Grid]
            ["@mui/material/Link$default" :as Link]
            ["@mui/material/Typography$default" :as Typography]
            ["react-dom" :as react-dom]
            [applied-science.js-interop :as jsi]
            [l4-lp.web-editor.codemirror-editor :as cm-editor]
            [l4-lp.web-editor.guifier :as guifier]
            [l4-lp.web-editor.utils :refer [fetch-text-from-url-and-then!]]
            [tupelo.core :refer [it->]]
            [uix.core :as uix]
            [uix.dom :as uix-dom]))

(def ^:private web-editor-preamble-url
 "web_editor_preamble.edn")

(def ^:private web-editor-instrs-url
  "web_editor_instructions.txt")

(def ^:private web-editor-app-id
  "web-editor-app")

(uix/defui cm-editor-grid-item
  [{:keys [grid-props cm-editor-ref]}]

  (let [[web-editor-instrs set-web-editor-instrs!] (uix/use-state nil)]
    (uix/use-effect
     #(fetch-text-from-url-and-then! web-editor-instrs-url
                                     set-web-editor-instrs!))
    (uix/$
     Grid grid-props
     (uix/$
      Box {:m 2}
      (uix/$ Accordion
             (uix/$ AccordionSummary
                    {:expand-icon (uix/$ ExpandMoreIcon)
                     :aria-controls :web-editor-instrs-control
                     :id :web-editor-instrs}
                    (uix/$ Typography {:variant :h6}
                           "Usage instructions"))
             (uix/$ AccordionDetails
                    (uix/$ Typography {:m 1 ;; :mt 1 :mb 1
                                       :max-width :md
                                       :white-space :pre-line
                                       :variant :body1}
                           web-editor-instrs))))

     (uix/$ cm-editor/cm-editor
            {:ref cm-editor-ref
             :max-height :90vh
             :font-size :14pt
             :editor-preamble-url web-editor-preamble-url}))))

(uix/defui guifier-grid-item
  [{:keys [grid-props cm-editor-ref guifier-ref]}]

  (let [cm-editor->query-trace-and-update-guifier!
        #(let [cm-editor @cm-editor-ref
               guifier @guifier-ref]
           (it-> cm-editor
                 (jsi/get-in it [:view :state :doc])
                 (str it)
                 (guifier/query-trace-and-update-guifier! guifier it)))]
    (uix/$
     Grid grid-props
     (uix/$ Typography {:variant :h4}
            "Query Results")

     (uix/$ Box {:m 3}
            (uix/$ Button {:variant :contained
                           :size :large
                           :end-icon (uix/$ SendIcon)
                           :on-click cm-editor->query-trace-and-update-guifier!}
                   "Run Queries"))

     (uix/$ guifier/guifier
            {:ref guifier-ref
             :max-height :100vh}))))

(uix/defui web-editor-app []
  (uix/$
   Box
   (uix/$ :title "L4 web editor")

   (react-dom/preconnect "https://fonts.googleapis.com")
   (react-dom/preconnect "https://fonts.gstatic.com"
                         #js {:crossOrigin "anonymous"})

   (react-dom/preload "https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500;700&display=swap"
                      #js {:as "style"})

   (uix/$ Typography {:variant :h3 :gutter-bottom true}
          "L4 web editor")

   (uix/$ Link {:href "https://github.com/smucclaw/l4-lp"
                :underline :hover
                :variant :h6}
          "Click here to visit the project on GitHub!")

   (let [cm-editor-ref (uix/use-ref)
         guifier-ref (uix/use-ref)]
     (uix/$ Grid {:container true}
            (uix/$ cm-editor-grid-item
                   {:grid-props {:m 2}
                    :cm-editor-ref cm-editor-ref})
            (uix/$ guifier-grid-item
                   {:grid-props {:m 2}
                    :cm-editor-ref cm-editor-ref
                    :guifier-ref guifier-ref})))))

(defn- render-web-editor-app! []
  (let [app-root
        (-> js/document
            (jsi/call :getElementById web-editor-app-id)
            uix-dom/create-root)]
    (uix-dom/render-root (uix/$ web-editor-app) app-root)))

(defn start! []
  (println "Starting..."))

(defn stop! []
  (println "Stopping..."))

(defn init! []
  (render-web-editor-app!)
  (start!))