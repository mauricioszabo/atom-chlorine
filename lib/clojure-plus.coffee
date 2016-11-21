{CompositeDisposable, TextEditor} = require 'atom'
fs = require 'fs'

SelectView = require './select-view'
EvryProvider = require './evry-provider'
CljCommands = require './clj-commands'
highlight = require './sexp-highlight'
MarkerCollection = require './marker-collection'

module.exports =
  config: require('./configs')

  currentWatches: {}
  lastClear: null

  everythingProvider: ->
    new EvryProvider(this)

  activate: (state) ->
    atom.commands.add 'atom-text-editor', 'clojure-plus:refresh-namespaces', =>
      @getCommands().runRefresh()
    atom.commands.add 'atom-text-editor', 'clojure-plus:toggle-simple-refresh', =>
      atom.config.set('clojure-plus.simpleRefresh', !atom.config.get('clojure-plus.simpleRefresh'))
    atom.commands.add 'atom-text-editor', 'clojure-plus:goto-var-definition', =>
      if editor = atom.workspace.getActiveTextEditor()
        varName = editor.getWordUnderCursor(wordRegex: /[a-zA-Z0-9\-.$!?:\/><\+*]+/)
        @getCommands().openFileContainingVar(varName)
    atom.commands.add 'atom-text-editor', 'clojure-plus:clear-and-refresh-namespaces', =>
      @getCommands().runRefresh(true)
    atom.commands.add 'atom-text-editor', 'clojure-plus:evaluate-top-block', =>
      @executeTopLevel()
    atom.commands.add 'atom-text-editor', 'clojure-plus:evaluate-full-file', =>
      editor = atom.workspace.getActiveTextEditor()
      return unless editor?
      ranges = protoRepl.EditorUtils.getTopLevelRanges(editor)
      ranges.forEach (r) =>
        @runCodeInRange(editor, r)

    atom.config.observe("clojure-plus.highlightSexp", highlight)
    highlight(atom.config.get('clojure-plus.highlightSexp'))
    atom.config.observe "clojure-plus.simpleRefresh", (refresh) => @checkRefreshMode(refresh)

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



    @addWatcher "watch", "(let [__sel__ ..SEL..]
                            (println \"Result at\ ..FILE_NAME.., line\"
                                     ..ROW..
                                     \"column\"
                                     ..COL..
                                     \" => \"
                                     __sel__)
                            (swap! user/__watches__ update-in [..ID..] (fn [x] (conj (or x []) __sel__)))
                            __sel__)"

    @addWatcher "def-local-symbols", "(do (doseq [__s__ (refactor-nrepl.find.find-locals/find-used-locals {:file ..FILE_NAME..
                                                                                                           :line ..ROW..
                                                                                                           :column ..COL..})]
                                            (do (println \"Adding eval to\" __s__) (eval `(def ~__s__ ~'__s__))))
                                       ..SEL..)"

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
          atom.notifications.addSuccess("REPL connected") if atom.config.get('clojure-plus.notify')
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
                # TODO - VER ISSO!
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
      if namespaces.length == 0
        atom.notifications.addInfo("No unused namespaces on file")
        return

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

    mark = editor.markBufferRange(region) unless @removeMarkIfExists(editor, region)
    if mark?
      mark.editor = editor
      mark.expression = expression
      editor.decorateMarker(mark, type: "highlight", class: "clojure-watch-expr " + type)
      @currentWatches[mark.id] = mark

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

    if editor = atom.workspace.getActiveTextEditor()
      if range = protoRepl.EditorUtils.getCursorInBlockRange(editor, topLevel: true)
        @runCodeInRange(editor, range)

  runCodeInRange: (editor, range) ->
    # Copy-paste from proto-repl... sorry...
    protoRepl.clearRepl() if atom.config.get('clojure-plus.clearRepl')
    oldText = editor.getTextInBufferRange(range).trim()
    text = @getCommands().markers.updatedCodeInRange(editor, range)
    text = "(try #{text}\n(catch Exception e
      (do
        (reset! --check-deps--/last-exception
          {:cause (str e)
           :trace (map --check-deps--/prettify-stack (.getStackTrace e))}))
        (throw e)))"

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
      displayInRepl: true
      resultHandler: (_) => null
      inlineOptions:
        editor: editor
        range: range

    @getCommands().promisedRepl.syncRun("(do (in-ns 'user)
                                             (def __watches__ (atom {}))
                                             (reset! --check-deps--/last-exception nil))", 'user').then =>
      @getCommands().promisedRepl.syncRun(text, options).then (result) =>
        if result.value
          options.displayInRepl = true
          options.resultHandler = protoRepl.repl.inlineResultHandler
          protoRepl.repl.inlineResultHandler(result, options)
        else
          @getCommands().promisedRepl.runCodeInCurrentNS('@--check-deps--/last-exception', session: 'exceptions').then (result) =>
            value = protoRepl.parseEdn(result.value) if result.value
            @makeErrorInline(value, editor, range) if value

        @handleWatches(options)

  makeErrorInline: ({cause, trace}, editor, range) ->
    result = new protoRepl.ink.Result(editor, [range.start.row, range.end.row], type: "block")
    causeHtml = document.createElement('strong')
    causeHtml.classList.add('error-description')
    causeHtml.innerText = cause

    strTrace = cause + "\n"

    traceHtmls = trace.map (row) =>
      strTrace += "\n\tin #{row.fn}\n\tat #{row.file}:#{row.line}\n"

      div = document.createElement('div')
      div.classList.add('trace-entry')

      span = document.createElement('span')
      span.classList.add('fade')
      span.innerText = ' in '
      div.appendChild span

      div.appendChild new Text(row.fn)

      span = document.createElement('span')
      span.classList.add('fade')
      span.innerText = ' at '
      div.appendChild span

      a = document.createElement('a')
      div.appendChild(a)
      a.href = '#'
      a.appendChild new Text("#{row.file}:#{row.line}")
      if row.link
        a.onclick = =>
          if row.link.match(/\.jar!/)
            tmp = atom.config.get('clojure-plus.tempDir').replace(/\\/g, "\\\\").replace(/"/g, "\\\"") +
            saneLink = row.link.replace(/\\/g, "\\\\").replace(/"/g, '\\"')
            code = "(let [[_ & jar-data] (--check-deps--/extract-jar-data \"#{saneLink}\")]
                      (--check-deps--/decompress-all \"#{tmp}\" jar-data))"
            @getCommands().promisedRepl.syncRun(code).then (result) =>
              return unless result.value
              atom.workspace.open(protoRepl.parseEdn(result.value), searchAllPanes: true, initialLine: row.line-1)
          else
            atom.workspace.open(row.link, searchAllPanes: true, initialLine: row.line-1)
      div

    treeHtml = protoRepl.ink.tree.treeView(causeHtml, traceHtmls, {})
    result.setContent(treeHtml)
    result.view.classList.add('error')
    protoRepl.stderr(strTrace)

  executeAndCopy: (code, pprint) ->
    @getCommands().promisedRepl.syncRun(code).then (result) =>
      if result.value
        value = result.value
        value = protoRepl.parseEdn(value) if pprint
        atom.clipboard.write(value)
        atom.notifications.addSuccess("Copied result to clipboard")
      else
        atom.notifications.addError("There was an error with your code")

  handleWatches: (options) ->
    @getCommands().promisedRepl
      .syncRun('(map (fn [[k v]] (str k "#" (with-out-str (print-method v *out*)))) @user/__watches__)')
      .then (result) =>
        return unless result.value
        values = protoRepl.parseEdn(result.value)
        for row in values
          id = row.replace(/#.*/, "")
          data = row.replace(/\d+#/, "")
          watch = @currentWatches[id]
          if watch && !watch.destroyed
            protoRepl.repl.displayInline(watch.editor, watch.getBufferRange(), protoRepl.ednToDisplayTree(data))

  getCommands: ->
    @commands ?= new CljCommands(@currentWatches, protoRepl)

  statusBarConsumer: (statusBar) ->
    div = document.createElement('div')
    div.classList.add('inline-block', 'clojure-plus')
    @statusBarTile = statusBar.addRightTile(item: div, priority: 101)
    @checkRefreshMode(atom.config.get("clojure-plus.simpleRefresh"))

  checkRefreshMode: (simple) ->
    return unless @statusBarTile
    text = "Clojure, refreshing"
    if simple
      text += " (simple)"
    else
      text += " (full)"

    if atom.config.get('clojure-plus.refreshAfterSave')
      text += " after saving"
    @statusBarTile.item.innerText = text

  deactivate: ->
    @statusBarTile?.destroy()
    @statusBarTile = null
