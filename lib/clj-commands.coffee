fs = require 'fs'
process = require 'process'

module.exports = class CljCommands
  constructor: (@watches) ->

  prepare: ->
    code = @getFile("~/.atom/packages/clojure-plus/lib/clj/check_deps.clj")
    protoRepl.executeCode code, displayInRepl: false

  runRefresh: (all) ->
    before = atom.config.get('clojure-plus.beforeRefreshCmd')
    after = atom.config.get('clojure-plus.afterRefreshCmd')

    shouldRefreshAll = all || !@lastRefreshSucceeded
    refreshCmd = @getRefreshCmd(shouldRefreshAll)

    notify = atom.config.get('clojure-plus.notify')
    protoRepl.executeCode before, ns: "user", displayInRepl: false
    protoRepl.executeCode refreshCmd, ns: "user", displayInRepl: false, resultHandler: (result) =>
      console.log "Refreshed? ", result
      if result.value
        value = protoRepl.parseEdn(result.value)
        if !value.cause
          @lastRefreshSucceeded = true
          atom.notifications.addSuccess("Refresh successful.") if notify
          protoRepl.appendText("Refresh successful.")
          @assignWatches()
        else
          @lastRefreshSucceeded = false
          causes = value.via.map (e) -> e.message
          causes = "#{value.cause}\n#{causes.join("\n")}"
          atom.notifications.addError("Error refreshing.", detail: causes) if notify
          protoRepl.appendText("Error refreshing. CAUSE: #{value.cause}\n")
          protoRepl.appendText(result.value)
      else if !shouldRefreshAll
        @runRefresh(true)
      else
        atom.notifications.addError("Error refreshing.", detail: value.error) if notify
        protoRepl.appendText("Error refreshing. CAUSE: #{value.error}\n")
    protoRepl.executeCode after, ns: "user", displayInRepl: false

  getRefreshCmd: (all) ->
    key = if all then 'clojure-plus.refreshAllCmd' else 'clojure-plus.refreshCmd'
    @getFile(atom.config.get(key))

  assignWatches: ->
    console.log "Assigning watches"
    for id, mark of @watches
      if mark.isValid()
        ns = protoRepl.EditorUtils.findNsDeclaration(mark.editor)
        opts = { displayInRepl: false }
        opts.ns = ns if ns
        protoRepl.executeCode(mark.topLevelExpr, opts)
      else
        delete @watches[id]

  openFileContainingVar: ->
    if editor = atom.workspace.getActiveTextEditor()
      selected = editor.getWordUnderCursor(wordRegex: /[a-zA-Z0-9\-.$!?:\/><\+*]+/)
      tmpPath = '"' +
                atom.config.get('clojure-plus.tempDir').replace(/\\/g, "\\\\").replace(/"/g, "\\\"") +
                '"'
      console.log "TEMP PATH", tmpPath

      if selected
        text = "(--check-deps--/goto-var '#{selected} #{tmpPath})"

        protoRepl.executeCodeInNs text,
          displayInRepl: false
          resultHandler: (result)=>
            if result.value
              # @appendText("Opening #{result.value}")
              [file, line] = protoRepl.parseEdn(result.value)
              atom.workspace.open(file, {initialLine: line-1, searchAllPanes: true})
            else
              protoRepl.appendText("Error trying to open: #{result.error}")

  getFile: (file) ->
    home = process.env.HOME
    fileName = file.replace("~", home)
    fs.readFileSync(fileName).toString()
