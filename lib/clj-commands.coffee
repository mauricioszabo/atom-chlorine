fs = require 'fs'
process = require 'process'
PromisedRepl = require './promised-repl'

module.exports = class CljCommands
  constructor: (@watches, @repl) ->
    @promisedRepl = new PromisedRepl(@repl)
    window.commands = this

  prepare: ->
    code = @getFile("~/.atom/packages/clojure-plus/lib/clj/check_deps.clj")
    @repl.executeCode code, displayInRepl: false

  runRefresh: (all) ->
    before = atom.config.get('clojure-plus.beforeRefreshCmd')
    after = atom.config.get('clojure-plus.afterRefreshCmd')

    @repl.executeCode before, ns: "user", displayInRepl: false unless simple && all
    if simple
      @runSimpleRefresh(all)
    else
      @runFullRefresh(all)
    @repl.executeCode after, ns: "user", displayInRepl: false

  runSimpleRefresh: (all) ->
    return if all
    notify = atom.config.get('clojure-plus.notify')
    refreshCmd = "(require (.name *ns*) :reload)"
    @repl.executeCodeInNs refreshCmd, displayInRepl: false, resultHandler: (result) =>
      if result.value
        atom.notifications.addSuccess("Refresh successful.") if notify
        @repl.appendText("Refresh successful.")
        @assignWatches()
      else
        atom.notifications.addError("Error refreshing.", detail: result.error) if notify
        @repl.appendText("Error refreshing. CAUSE: #{result.error}\n")

  runFullRefresh: (all) ->
    shouldRefreshAll = all || !@lastRefreshSucceeded
    refreshCmd = @getRefreshCmd(shouldRefreshAll)

    notify = atom.config.get('clojure-plus.notify')
    @repl.executeCode refreshCmd, ns: "user", displayInRepl: false, resultHandler: (result) =>
      if result.value
        value = @repl.parseEdn(result.value)
        if !value.cause
          @lastRefreshSucceeded = true
          atom.notifications.addSuccess("Refresh successful.") if notify
          @repl.appendText("Refresh successful.")
          @assignWatches()
        else
          @lastRefreshSucceeded = false
          causes = value.via.map (e) -> e.message
          causes = "#{value.cause}\n#{causes.join("\n")}"
          atom.notifications.addError("Error refreshing.", detail: causes) if notify
          @repl.appendText("Error refreshing. CAUSE: #{value.cause}\n")
          @repl.appendText(result.value)
      else if !shouldRefreshAll
        @runRefresh(true)
      else
        atom.notifications.addError("Error refreshing.", detail: result.error) if notify
        @repl.appendText("Error refreshing. CAUSE: #{result.error}\n")

  getRefreshCmd: (all) ->
    key = if all then 'clojure-plus.refreshAllCmd' else 'clojure-plus.refreshCmd'
    @getFile(atom.config.get(key))

  assignWatches: ->
    console.log "Assigning watches"
    for id, mark of @watches
      if mark.isValid()
        ns = @repl.EditorUtils.findNsDeclaration(mark.editor)
        opts = { displayInRepl: false }
        opts.ns = ns if ns
        @repl.executeCode(mark.topLevelExpr, opts)
      else
        delete @watches[id]

  openFileContainingVar: ->
    if editor = atom.workspace.getActiveTextEditor()
      selected = editor.getWordUnderCursor(wordRegex: /[a-zA-Z0-9\-.$!?:\/><\+*]+/)
      tmpPath = '"' +
                atom.config.get('clojure-plus.tempDir').replace(/\\/g, "\\\\").replace(/"/g, "\\\"") +
                '"'

      if selected
        text = "(--check-deps--/goto-var '#{selected} #{tmpPath})"

        @repl.executeCodeInNs text,
          displayInRepl: false
          resultHandler: (result)=>
            if result.value
              # @appendText("Opening #{result.value}")
              [file, line] = @repl.parseEdn(result.value)
              atom.workspace.open(file, {initialLine: line-1, searchAllPanes: true})
            else
              @repl.appendText("Error trying to open: #{result.error}")

  getFile: (file) ->
    home = process.env.HOME
    fileName = file.replace("~", home)
    fs.readFileSync(fileName).toString()

  getSymbolsInEditor: (editor) ->
    @promisedRepl.runCodeInCurrentNS("(--check-deps--/symbols-from-ns-in-json *ns*)").then (result) =>
      return unless result.value
      JSON.parse(result.value)
