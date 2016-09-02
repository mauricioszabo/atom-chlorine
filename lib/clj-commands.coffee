fs = require 'fs'
process = require 'process'
PromisedRepl = require './promised-repl'

module.exports = class CljCommands
  constructor: (@watches, @repl) ->
    @promisedRepl = new PromisedRepl(@repl)

  prepare: ->
    code = @getFile("~/.atom/packages/clojure-plus/lib/clj/check_deps.clj")
    @promisedRepl.clear()
    @promisedRepl.syncRun(code, 'user')

  runRefresh: (all) ->
    before = atom.config.get('clojure-plus.beforeRefreshCmd')
    after = atom.config.get('clojure-plus.afterRefreshCmd')
    simple = atom.config.get('clojure-plus.simpleRefresh')

    @prepare()
    @promisedRepl.syncRun(before, "user") unless simple && all
    if simple
      @runSimpleRefresh(all)
    else
      @runFullRefresh(all)
    @promisedRepl.syncRun(after,"user")

  runSimpleRefresh: (all) ->
    return if all
    notify = atom.config.get('clojure-plus.notify')
    ns = @repl.EditorUtils.findNsDeclaration(atom.workspace.getActiveTextEditor())
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

  assignWatches: ->
    for id, mark of @watches
      if mark.isValid()
        ns = @repl.EditorUtils.findNsDeclaration(mark.editor)
        @promisedRepl.syncRun("(def __watches__ (atom {}))", 'user').then =>
          @promisedRepl.syncRun(mark.topLevelExpr, ns)
      else
        delete @watches[id]

  openFileContainingVar: (varName) ->
    tmpPath = '"' +
              atom.config.get('clojure-plus.tempDir').replace(/\\/g, "\\\\").replace(/"/g, "\\\"") +
              '"'

    if varName
      text = "(--check-deps--/goto-var '#{varName} #{tmpPath})"

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
    @promisedRepl.runCodeInCurrentNS("(--check-deps--/resolve-missing \"#{symbolName}\")")

  unusedImports: (filePath) ->
    filePath = filePath.replace(/"/g, '\\\\').replace(/\\/g, '\\\\')
    @promisedRepl.runCodeInCurrentNS("(--check-deps--/unused-namespaces \"#{filePath}\")")

  getSymbolsInEditor: (editor) ->
    @promisedRepl.runCodeInCurrentNS("(--check-deps--/symbols-from-ns-in-json *ns*)").then (result) =>
      return unless result.value
      JSON.parse(result.value)
