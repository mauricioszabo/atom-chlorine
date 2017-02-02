fs = require 'fs'
process = require 'process'
PromisedRepl = require './promised-repl'
MarkerCollection = require './marker-collection'

module.exports = class CljCommands
  cljs: false

  constructor: (@watches, @repl) ->
    @promisedRepl = new PromisedRepl(@repl)
    @markers = new MarkerCollection(@watches)

  prepareCljs: ->
    if !@cljs
      code = atom.config.get('clojure-plus.cljsCommand')
      @promisedRepl.clear()
      @promisedRepl.syncRun(code, 'user', session: 'cljs').then (e) =>
        @promisedRepl.syncRun("(ns clj.--check-deps--)", 'clj.--check-deps--', session: 'cljs')
        @promisedRepl.syncRun("(def last-exception (atom nil))", 'clj.--check-deps--', session: 'cljs')
        @promisedRepl.syncRun("(def watches (atom {}))", 'clj.--check-deps--', session: 'cljs')
        if e.value
          if atom.config.get('clojure-plus.notify')
            atom.notifications.addSuccess("Loaded ClojureScript REPL")
        else
          atom.notifications.addError("Error loading ClojureScript", detail: e.error)
        @cljs = true

  prepare: ->
    code = @getFile("~/.atom/packages/clojure-plus/lib/clj/__check_deps__.clj")
    @cljs = false
    @promisedRepl.clear()
    @promisedRepl.syncRun(code, 'user')

  runRefresh: (all) ->
    simple = atom.config.get('clojure-plus.simpleRefresh')

    @prepare()
    @runBefore()
    if simple
      @runSimpleRefresh(all)
    else
      @runFullRefresh(all)
    @runAfter()

  runBefore: ->
    before = atom.config.get('clojure-plus.beforeRefreshCmd')
    @promisedRepl.syncRun(before, "user")

  runAfter: ->
    after = atom.config.get('clojure-plus.afterRefreshCmd')
    @promisedRepl.syncRun(after,"user")

  runSimpleRefresh: (all) ->
    return if all
    notify = atom.config.get('clojure-plus.notify')
    ns = @repl.EditorUtils.findNsDeclaration(atom.workspace.getActiveTextEditor())
    return unless ns
    refreshCmd = "(require '#{ns} :reload)"

    @promisedRepl.syncRun(refreshCmd).then (result) =>
      if result.value
        atom.notifications.addSuccess("Refresh successful.") if notify
        @repl.info("Refresh successful.")
        @assignWatches()
      else
        atom.notifications.addError("Error refreshing.", detail: result.error) if notify
        @repl.stderr("Error refreshing. CAUSE: #{result.error}\n")

  runFullRefresh: (all) ->
    shouldRefreshAll = all || !@lastRefreshSucceeded
    refreshCmd = @getRefreshCmd(shouldRefreshAll)

    notify = atom.config.get('clojure-plus.notify')
    @promisedRepl.syncRun(refreshCmd, "user").then (result) =>
      if result.value
        value = @repl.parseEdn(result.value)
        if !value.cause
          @lastRefreshSucceeded = true
          atom.notifications.addSuccess("Refresh successful.") if notify
          @repl.info("Refresh successful.")
          @assignWatches()
        else
          @lastRefreshSucceeded = false
          causes = value.via.map (e) -> e.message
          causes = "#{value.cause}\n#{causes.join("\n")}"
          atom.notifications.addError("Error refreshing.", detail: causes) if notify
          @repl.stderr("Error refreshing. CAUSE: #{value.cause}\n")
          @repl.stderr(result.value)
      else if !shouldRefreshAll
        @runRefresh(true)
      else
        atom.notifications.addError("Error refreshing.", detail: result.error) if notify
        @repl.stderr("Error refreshing. CAUSE: #{result.error}\n")

  getRefreshCmd: (all) ->
    key = if all then 'clojure-plus.refreshAllCmd' else 'clojure-plus.refreshCmd'
    @getFile(atom.config.get(key))

  # TODO: Move me to MarkerCollection
  assignWatches: ->
    @runBefore()
    @promisedRepl.syncRun("(reset! clj.--check-deps--/watches {})", 'user').then =>
      for id, mark of @watches
        delete @watches[id] unless mark.isValid()
        ns = @repl.EditorUtils.findNsDeclaration(mark.editor)
        range = @markers.getTopLevelForMark(mark)
        @promisedRepl.syncRun(@markers.updatedCodeInRange(mark.editor, range), ns)

    @runAfter()

  openFileContainingVar: (varName) ->
    tmpPath = '"' +
              atom.config.get('clojure-plus.tempDir').replace(/\\/g, "\\\\").replace(/"/g, "\\\"") +
              '"'

    if varName
      text = "(clj.--check-deps--/goto-var '#{varName} #{tmpPath})"

      @promisedRepl.runCodeInCurrentNS(text).then (result) =>
        if result.value
          [file, line] = @repl.parseEdn(result.value)
          pending = atom.config.get('clojure-plus.openPending')
          atom.workspace.open(file, initialLine: line-1, searchAllPanes: true, pending: pending)
        else
          @repl.stderr("Error trying to open: #{result.error}")

  getFile: (file) ->
    home = process.env.HOME
    fileName = file.replace("~", home)
    fs.readFileSync(fileName).toString()

  nsForMissing: (symbolName) ->
    @promisedRepl.runCodeInCurrentNS("(clj.--check-deps--/resolve-missing \"#{symbolName}\")")

  unusedImports: (filePath) ->
    filePath = filePath.replace(/"/g, '\\\\').replace(/\\/g, '\\\\')
    @promisedRepl.runCodeInCurrentNS("(clj.--check-deps--/unused-namespaces \"#{filePath}\")")

  getSymbolsInEditor: (editor) ->
    @promisedRepl.runCodeInCurrentNS("(clj.--check-deps--/symbols-from-ns-in-json *ns*)").then (result) =>
      return unless result.value
      JSON.parse(result.value)
