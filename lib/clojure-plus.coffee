{CompositeDisposable, TextEditor} = require 'atom'
SelectView = require './select-view'
EvryProvider = require './evry-provider'
CljCommands = require './clj-commands'
fs = require 'fs'

module.exports =
  config:
    notify:
      description: "Notify when refresh was done"
      type: "boolean"
      default: true
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
      description: "Path to a file with the refresh all namespaces' command"
      type: 'string'
      default: "~/.atom/packages/clojure-plus/lib/clj/refresh_all.clj"
    refreshCmd:
      description: "Path to a file with the refresh namespaces' command"
      type: 'string'
      default: "~/.atom/packages/clojure-plus/lib/clj/refresh.clj"
    clearRepl:
      description: "Clear REPL before running a command"
      type: 'boolean'
      default: false
    tempDir:
      description: "Temporary directory to unpack JAR files (used by goto-var)"
      type: "string"
      default: "/tmp/jar-path"

  currentWatches: {}
  lastClear: null

  everythingProvider: -> new EvryProvider()

  activate: (state) ->
    atom.commands.add 'atom-text-editor', 'clojure-plus:refresh-namespaces', =>
      @getCommands().runRefresh()
    atom.commands.add 'atom-text-editor', 'clojure-plus:goto-var-definition', =>
      @getCommands().openFileContainingVar()
    atom.commands.add 'atom-text-editor', 'clojure-plus:clear-and-refresh-namespaces', =>
      @getCommands().runRefresh(true)
    atom.commands.add 'atom-text-editor', 'clojure-plus:evaluate-top-block', =>
      @executeTopLevel()

    editorCode = ->
      editor = atom.workspace.getActiveTextEditor()
      editor.getTextInRange(editor.getSelectedBufferRange())

    atom.commands.add 'atom-text-editor', 'clojure-plus:execute-selection-and-copy-result', =>
      @executeAndCopy(editorCode())
    atom.commands.add 'atom-text-editor', 'clojure-plus:execute-selection-and-copy-pretty-printed-result', =>
      code = "(clojure.core/with-out-str (clojure.pprint/pprint #{editorCode()})))"
      @executeAndCopy(code, 'pretty-print')
    atom.commands.add 'atom-text-editor', 'clojure-plus:import-for-missing-symbol', =>
      @importForMissing()
    atom.commands.add 'atom-text-editor', 'clojure-plus:remove-unused-imports', =>
      @removeUnusedImport(atom.workspace.getActiveTextEditor())



    @addWatcher("watch", "(let [__sel__ ..SEL..]
                            (println \"Result at\ ..FILE_NAME.., line\"
                                     ..ROW..
                                     \"column\"
                                     ..COL..
                                     \" => \"
                                     __sel__)
                            (swap! user/__watches__ update-in [..ID..] (fn [x] (conj (or x []) __sel__)))
                            __sel__)")

    atom.commands.add 'atom-text-editor', 'clojure-plus:remove-all-watches', =>
      for id, watch of @currentWatches
        watch.destroy()
        delete @currentWatches[id]
      @getCommands().assignWatches()

    atom.workspace.observeTextEditors (editor) =>
      editor.onDidSave =>
        if atom.config.get('clojure-plus.refreshAfterSave') && editor.getGrammar().scopeName == 'source.clojure'
          @getCommands().runRefresh()

    atom.packages.onDidActivatePackage (pack) =>
      if pack.name == 'proto-repl'
        @commands = new CljCommands(@currentWatches, protoRepl)

        protoRepl.onDidConnect =>
          @getCommands().prepare()

          if atom.config.get('clojure-plus.refreshAfterConnect')
            @getCommands().runRefresh()

    atom.commands.add 'atom-text-editor', 'clojure-plus:display-full-symbol-name', =>
      editor = atom.workspace.getActiveTextEditor()
      [range, symbol] = @getRangeAndVar(editor)
      protoRepl.executeCodeInNs("`" + symbol, inlineOptions: {editor: editor, range: range})

  importForMissing: ->
      editor = atom.workspace.getActiveTextEditor()
      [varRange, varNameRaw] = @getRangeAndVar(editor)
      varName = varNameRaw?.replace(/"/g, '\\"')
      if !varName
        atom.notifications.addError("Position your cursor in a clojure var name")
        return

      @getCommands().nsForMissing(varName).then (results) =>
        command = (namespace, alias) ->
          atom.clipboard.write("[#{namespace} :as #{alias}]")
          editor.setTextInBufferRange(varRange, "#{alias}/#{varNameRaw}")
          atom.notifications.addSuccess("Import copied to clipboard!")

        if !results.value
          atom.notifications.addError("Error processing import request", detail: results.error)
          return

        result = protoRepl.parseEdn(results.value)
        if result && result.length > 0
          items = result.map (res) ->
            alias = if res[1] then res[1] else "[no alias]"
            text = "[#{res[0]} :as #{alias}]"
            label: text, run: =>
              if res[1]
                command(res[0], res[1])
              else
                te = new TextEditor(mini: true, placeholderText: "type your namespace alias")
                panel = atom.workspace.addModalPanel(item: te)
                atom.commands.add te.element, 'core:confirm': ->
                  command(res[0], te.getText())
                  panel.destroy()
                  atom.views.getView(atom.workspace).focus()
                , 'core:cancel': ->
                  panel.destroy()
                  atom.views.getView(atom.workspace).focus()
                setTimeout ->
                  te.getModel().scrollToCursorPosition()
          new SelectView(items)
        else
          atom.notifications.addError("Import with namespace alias not found")

  removeUnusedImport: (editor) ->
    project = atom.project.getPaths()
    path = editor.getPath()
    project = project.filter (p) -> path.indexOf(p) != -1
    path = path.replace(project + "/", "")

    @commands.unusedImports(path).then (result) =>
      namespaces = protoRepl.parseEdn(result.value)
      nsRange = @getNsRange(editor)
      nsTexts = editor.getTextInBufferRange(nsRange).split("\n")
      newNsText = nsTexts.filter (row) =>
        namespaces.some (ns) =>
          !row.match(new RegExp("[\\(\\[]\\s*#{@escapeRegex(ns)}[\\s\\]\\)]"))
      editor.setTextInBufferRange(nsRange, newNsText.join("\n"))

  getNsRange: (editor) ->
    ranges = protoRepl.EditorUtils.getTopLevelRanges(editor)
    ranges.find (r) => editor.getTextInBufferRange(r).match(/\(\s*ns\b/)

  escapeRegex: (str) ->
    str.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");

  getRangeAndVar: (editor) ->
    varRange = editor.getLastCursor().getCurrentWordBufferRange(wordRegex: /[a-zA-Z0-9\-.$!?\/><*]+/)
    varName = editor.getTextInBufferRange(varRange)
    [varRange, varName]

  checkDependents: ->
    cljCode = fs.readFileSync(__dirname + "/clj/check_deps.clj").toString()

  addWatcher: (type, expression) ->
    atom.commands.add 'atom-text-editor', "clojure-plus:add-#{type}-in-selection", =>
      @markCustomExpr
        type: type
        expression: expression

  markCustomExpr: ({expression, type, region}) ->
    editor = atom.workspace.getActiveTextEditor()
    return unless editor?
    region ?= editor.getSelectedBufferRange()
    {row, column} = region.getExtent()
    if row == 0 && column == 0 # If nothing is selected
      region = editor.getLastCursor()
                     .getCurrentWordBufferRange({wordRegex: /[a-zA-Z0-9\-.$!?:\/><*]+/})

    {row, column} = region.getExtent()
    return if row == 0 && column == 0 # if no word was found

    mark = editor.markBufferRange(region, invalidate: "touch") unless @removeMarkIfExists(editor, region)
    if mark?
      cljVar = editor.getTextInBufferRange(region)
      expression = expression.replace(/\.\.FILE_NAME\.\./g, editor.getPath())
      expression = expression.replace(/\.\.ROW\.\./g, region.start.row + 1)
      expression = expression.replace(/\.\.COL\.\./g, region.start.column + 1)
      expression = expression.replace(/\.\.SEL\.\./g, cljVar)
      expression = expression.replace(/\.\.ID\.\./g, mark.id)

      mark.expression = expression
      mark.editor = editor

      editor.decorateMarker(mark, type: "highlight", class: "clojure-watch-expr " + type)
      @currentWatches[mark.id] = mark

      topRanges = protoRepl.EditorUtils.getTopLevelRanges(editor)
      topRange = topRanges.find (range) => range.containsPoint(region.start)
      text = @updateWithMarkers(editor, topRange)
      mark.topLevelExpr = text

    @getCommands().assignWatches()

  removeMarkIfExists: (editor, region)->
    for _, mark of @currentWatches
      {start, end} = mark.getBufferRange()
      if start.column == region.start.column && start.row == region.start.row &&
         end.column == region.end.column && end.row == region.end.row
        mark.destroy()
        return true

    return false

  executeTopLevel: ->
    editor = atom.workspace.getActiveTextEditor()

    # Copy-paste from proto-repl... sorry...
    if editor = atom.workspace.getActiveTextEditor()
      if range = protoRepl.EditorUtils.getCursorInBlockRange(editor, topLevel: true)
        protoRepl.repl?.replTextEditor?.textEditor?.getBuffer()?.clearUndoStack()
        protoRepl.clearRepl() if atom.config.get('clojure-plus.clearRepl')
        oldText = editor.getTextInBufferRange(range).trim()
        text = @updateWithMarkers(editor, range)

        # Highlight the area that's being executed temporarily
        marker = editor.markBufferRange(range)
        decoration = editor.decorateMarker(marker,
            {type: 'highlight', class: "block-execution"})
        # Remove the highlight after a short period of time
        setTimeout(=>
          marker.destroy()
        , 350)

        options =
          displayCode: oldText
          resultHandler: (a,b) => @scheduleWatch(a, b)
          displayInRepl: false
          inlineOptions:
            editor: editor
            range: range

        # @getCommands().promisedRepl.clear()
        @getCommands().promisedRepl.syncRun("(do (in-ns 'user) (def __watches__ (atom {})))", 'user').then =>
          protoRepl.executeCodeInNs(text, options)

  executeAndCopy: (code, pprint) ->
    @getCommands().promisedRepl.syncRun(code).then (result) =>
      if result.value
        value = result.value
        value = protoRepl.parseEdn(value) if pprint
        atom.clipboard.write(value)
        atom.notifications.addSuccess("Copied result to clipboard")
      else
        atom.notifications.addError("There was an error with your code")

  scheduleWatch: (result, options) ->
    delete options.resultHandler
    protoRepl.repl.inlineResultHandler(result, options)
    protoRepl.executeCode '(map (fn [[k v]] (str k "#" (with-out-str (print-method v *out*)))) @user/__watches__)',
      displayInRepl: false, resultHandler: (res) => @handleWatches(res)

  handleWatches: (result, options) ->
    return unless result.value
    values = protoRepl.parseEdn(result.value)
    for row in values
      id = row.replace(/#.*/, "")
      data = row.replace(/\d+#/, "")
      watch = @currentWatches[id]
      if watch
        protoRepl.repl.displayInline(watch.editor, watch.getBufferRange(), protoRepl.ednToDisplayTree(data))

  updateWithMarkers: (editor, blockRange) ->
    marks = for _, m of @currentWatches then m
    marks = marks.filter (m) ->
      buffer = m.getBufferRange()
      buffer.start.row >= blockRange.start.row && buffer.end.row <= blockRange.end.row && m.editor == editor

    text = ""
    editor.transact =>
      for mark in marks
        mark.bufferMarker.invalidate = "never"
        editor.setTextInBufferRange(mark.getBufferRange(), mark.expression)

      # redo top range mark, 'cause we could have changed the limits
      topRanges = protoRepl.EditorUtils.getTopLevelRanges(editor)
      newBlockRange = topRanges.find (range) => range.containsPoint(blockRange.start)
      text = editor.getTextInBufferRange(newBlockRange).trim()
      editor.abortTransaction()

    for mark in marks
      mark.bufferMarker.invalidate = "touch"
    text

  getCommands: ->
    @commands ?= new CljCommands(@currentWatches, protoRepl)
