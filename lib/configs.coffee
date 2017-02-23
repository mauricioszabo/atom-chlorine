module.exports =
  highlightSexp:
    description: "Highlight current SEXP under cursor"
    type: "boolean"
    default: false
  notify:
    description: "Notify when refresh was done"
    type: "boolean"
    default: true
  simpleRefresh:
    description: "Refresh with a very simple code (that requires the current namespace)"
    type: "boolean"
    default: false
  refreshAfterConnect:
    description: "Refresh after REPL is connected"
    type: "boolean"
    default: true
  refreshAfterSave:
    description: "Refresh after saving a file"
    type: "boolean"
    default: true
  afterRefreshCmd:
    description: "Command to run after each refresh (success or failure)"
    type: 'string'
    default: "(alter-var-root #'clojure.test/*load-tests* (constantly true))"
  beforeRefreshCmd:
    description: "Command to run before each refresh (success or failure)"
    type: 'string'
    default: "(alter-var-root #'clojure.test/*load-tests* (constantly false))"
  refreshAllCmd:
    description: "Clear and refresh all namespaces command"
    type: 'string'
    default: "(do (require '[clojure.tools.namespace.repl :as repl]) (repl/clear) (repl/refresh-all))"
  refreshCmd:
    description: "Refresh namespaces command"
    type: 'string'
    default: "(do (require '[clojure.tools.namespace.repl :as repl]) (repl/refresh))"
  clearRepl:
    description: "Clear REPL before running a command"
    type: 'boolean'
    default: false
  openPending:
    description: "When opening a file with Goto Var Definition, keep tab as pending"
    type: 'boolean'
    default: true
  cljsCommand:
    description: "Clojure command to open ClojureScript's REPL"
    type: 'string'
    default: "(use 'figwheel-sidecar.repl-api) (start-figwheel!) (figwheel-sidecar.repl-api/cljs-repl)"
  tempDir:
    description: "Temporary directory to unpack JAR files (used by goto-var)"
    type: "string"
    default: "/tmp/jar-path"
  invertStack:
    description: "Inverts stacktrace showing on console"
    type: 'boolean'
    default: true
