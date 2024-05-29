(ns l4-lp.ide.query.worker 
  (:require [applied-science.js-interop :as jsi]
            [l4-lp.ide.query.utils :refer [post-data-as-js!]]
            [l4-lp.swipl.js.wasm-query :as swipl-wasm-query]
            [l4-lp.syntax.l4-to-prolog :as l4->prolog]
            [meander.epsilon :as m]
            [promesa.core :as prom]
            [tupelo.core :refer [it->]]))

(defn- transpile-and-query! [swipl-prelude-qlf-url l4-program]
  (let [transpiled-prolog (-> l4-program
                              l4->prolog/l4->prolog-program+queries)]
    (post-data-as-js! :tag "transpiled-prolog" :payload transpiled-prolog)

    (it-> transpiled-prolog
          (swipl-wasm-query/query-and-trace!
           it
           :swipl-prelude-qlf-url swipl-prelude-qlf-url
           :on-query-result
           #(post-data-as-js! :tag "query-result" :payload %))
          (prom/hcat #(post-data-as-js! :tag "done") it))))

(jsi/defn ^:private on-message! [^:js {:keys [data]}]
  (m/match data
    #js {:swipl-prelude-qlf-url (m/some ?swipl-prelude-qlf-url)
         :l4-program (m/some ?l4-program)}
    (transpile-and-query! ?swipl-prelude-qlf-url ?l4-program)))

(defn init! []
  ;; Ugly hack to get swipl wasm working in a web worker without access
  ;; to js/window.
  ;; The issue is that otherwise, it fails to load prolog and qlf file via
  ;; Prolog.consult with following error:
  ;; ERROR: JavaScript: ReferenceError: window is not defined
  ;; To solve this, we assign a global window object to an empty object just so
  ;; that it's defined.
  (jsi/assoc! js/globalThis :window #js {})

  ;; For some reason, (set! js/onmessage ...) yields the following error when
  ;; compiled with :optimizations :advanced
  ;;   constant onmessage assigned a value more than once.
  ;;   Original definition at externs.shadow.js:7
  ;; To workaround this, we add an event handler via addEventListener instead. 
  (jsi/call js/globalThis :addEventListener
            "message" on-message!))