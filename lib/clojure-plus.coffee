{CompositeDisposable, TextEditor} = require 'atom'
fs = require 'fs'

SelectView = require './select-view'
EvryProvider = require './evry-provider'
CljCommands = require './clj-commands'
highlight = require './sexp-highlight'
MarkerCollection = require './marker-collection'

disposable = new CompositeDisposable()
setTimeout ->
  window["clojure plus extensions"] = {disposable: disposable}
  require './js/main.js'
  delete window["clojure plus extensions"]

module.exports =
  config: require('./configs')

  currentWatches: {}
  lastClear: null

  everythingProvider: ->
    new EvryProvider(this)

  activate: (state) ->
    @evalModes = new Map()
    @subs = disposable
    @subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:refresh-namespaces', =>
      @getCommands().runRefresh()
    @subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:interrupt', =>
      @interrupt()
    @subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:toggle-simple-refresh', =>
      atom.config.set('clojure-plus.simpleRefresh', !atom.config.get('clojure-plus.simpleRefresh'))
    @subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:goto-var-definition', =>
      if editor = atom.workspace.getActiveTextEditor()
        varName = editor.getWordUnderCursor(wordRegex: /[a-zA-Z0-9\-.$!?:\/><\+*]+/)
        @getCommands().openFileContainingVar(varName)
    @subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:clear-and-refresh-namespaces', =>
      @getCommands().runRefresh(true)
    @subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:evaluate-top-block', =>
      @executeTopLevel()
    @subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:evaluate-block', =>
      @executeBlock()
    @subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:evaluate-selection', =>
      @executeSelection()
    @subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:evaluate-last-code', =>
      @runCode(@oldCode...) if @oldCode
    @subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:unregister-cljs-repl', =>
      @getCommands().cljs = false
      @updateStatusbar()

    @subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:evaluate-full-file', =>
      editor = atom.workspace.getActiveTextEditor()
      return unless editor?
      ranges = protoRepl.EditorUtils.getTopLevelRanges(editor)
      ranges.forEach (r) =>
        @runCodeInRange(editor, r)

    @subs.add atom.config.observe("clojure-plus.highlightSexp", highlight)
    highlight(atom.config.get('clojure-plus.highlightSexp'))
    @subs.add atom.config.observe "clojure-plus.simpleRefresh", (refresh) => @updateStatusbar(refresh)

    editorCode = ->
      editor = atom.workspace.getActiveTextEditor()
      editor.getTextInRange(editor.getSelectedBufferRange())
    @subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:execute-selection-and-copy-result', =>
      @executeAndCopy(editorCode())
    @subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:execute-selection-and-copy-pretty-printed-result', =>
      code = "(clojure.core/with-out-str (clojure.pprint/pprint #{editorCode()})))"
      @executeAndCopy(code, 'pretty-print')
    @subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:remove-unused-imports', =>
      @removeUnusedImport(atom.workspace.getActiveTextEditor())



    @addWatcher "watch", "(let [__sel__ ..SEL..]
                            (println \"Result at\ ..FILE_NAME.., line\"
                                     ..ROW..
                                     \"column\"
                                     ..COL..
                                     \" => \"
                                     __sel__)
                            (swap! clj.--check-deps--/watches update-in [..ID..] (fn [x] (conj (or x []) __sel__)))
                            __sel__)"

    @addWatcher "def-local-symbols", "(do (doseq [__s__ (refactor-nrepl.find.find-locals/find-used-locals {:file ..FILE_NAME..
                                                                                                           :line ..ROW..
                                                                                                           :column ..COL..})]
                                            (do (println \"Adding eval to\" __s__) (eval `(def ~__s__ ~'__s__))))
                                       ..SEL..)"

    @subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:remove-all-watches', =>
      for id, watch of @currentWatches
        watch.destroy()
        delete @currentWatches[id]
      @getCommands().assignWatches()

    @subs.add atom.workspace.observeActivePaneItem (item) =>
      if item instanceof TextEditor
        @updateStatusbar(atom.config.get("clojure-plus.simpleRefresh"), item)

    @subs.add atom.commands.add 'atom-text-editor', 'clojure-plus:toggle-clojure-and-clojurescript', =>
      editor = atom.workspace.getActiveTextEditor()
      mode = @evalModes.get(editor.getBuffer().id)
      if mode == 'cljs'
        @evalModes.set(editor.getBuffer().id, 'clojure')
      else
        @evalModes.set(editor.getBuffer().id, 'cljs')
      @updateStatusbar(atom.config.get("clojure-plus.simpleRefresh"), editor)

    grammarCode = (editor, {name}) =>
      if editor.getFileName()?.endsWith(".cljs")
        @evalModes.set(editor.getBuffer().id, 'cljs')
      else if name.match(/clojure/i)
        @evalModes.set(editor.getBuffer().id, 'clj')

    atom.workspace.observeTextEditors (editor) =>
      grammarCode(editor, editor.getGrammar())
      editor.onDidChangeGrammar (e) =>
        grammarCode(editor, e)
        @updateStatusbar(atom.config.get("clojure-plus.simpleRefresh"), editor)

      editor.onDidSave =>
        if atom.config.get('clojure-plus.refreshAfterSave') && editor.getGrammar().scopeName == 'source.clojure'
          @getCommands().runRefresh()

    atom.packages.onDidActivatePackage (pack) =>
      if pack.name == 'proto-repl'
        @commands = new CljCommands(@currentWatches, protoRepl)
        # require './js/main.js'

        protoRepl.onDidConnect =>
          atom.notifications.addSuccess("REPL connected") if atom.config.get('clojure-plus.notify')
          @getCommands().prepare()

          if atom.config.get('clojure-plus.refreshAfterConnect')
            @getCommands().runRefresh()

    atom.commands.add 'atom-text-editor', 'clojure-plus:display-full-symbol-name', =>
      editor = atom.workspace.getActiveTextEditor()
      [range, symbol] = @getRangeAndVar(editor)
      protoRepl.executeCodeInNs("`" + symbol, inlineOptions: {editor: editor, range: range})

  interrupt: ->
    protoRepl.interrupt()
    @getCommands().promisedRepl.clear()

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
    @subs.add atom.commands.add 'atom-text-editor', "clojure-plus:add-#{type}-in-selection", =>
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
    @executeCodeHelper false, (e) => protoRepl.EditorUtils.getCursorInBlockRange(e, topLevel: true)

  executeBlock: ->
    @executeCodeHelper true, (e) => protoRepl.EditorUtils.getCursorInBlockRange(e)

  executeSelection: ->
    @executeCodeHelper true, (e) => e.getSelectedBufferRange()

  executeCodeHelper: (ignoreWatches, fn) ->
    if editor = atom.workspace.getActiveTextEditor()
      if range = fn(editor)
        @runCodeInRange(editor, range, ignoreWatches)

  runCodeInRange: (editor, range, ignoreWatches) ->
    session = 'cljs' if @evalModes.get(editor.getBuffer().id) == 'cljs'

    # Copy-paste from proto-repl... sorry...
    protoRepl.clearRepl() if atom.config.get('clojure-plus.clearRepl')
    oldText = editor.getTextInBufferRange(range).trim()
    text = if ignoreWatches
      oldText
    else
      @getCommands().markers.updatedCodeInRange(editor, range)
    text = @wrapText(text, session)

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
      ns: protoRepl.EditorUtils.findNsDeclaration(editor)
      resultHandler: (_) => null
      inlineOptions:
        editor: editor
        range: range
    options.session = session if session?
    @getCommands().prepareCljs() if session == 'cljs'

    @getCommands().promisedRepl.syncRun("(reset! clj.--check-deps--/watches {})", 'user', session: session)
    @getCommands().promisedRepl.syncRun("(reset! clj.--check-deps--/last-exception nil)", 'user')
    @runCode(text, options, session)

  runCode: (text, options, session) ->
    editor = options.inlineOptions.editor
    range = options.inlineOptions.range
    @oldCode = [text, options, session] if editor
    @getCommands().promisedRepl.syncRun(text, options).then (result) =>
      if result.value
        options.displayInRepl = true
        options.resultHandler = protoRepl.repl.inlineResultHandler
        protoRepl.repl.inlineResultHandler(result, options)
      else if session != 'cljs'
        @getCommands().promisedRepl.runCodeInCurrentNS('@clj.--check-deps--/last-exception').then (res2) =>
          value = protoRepl.parseEdn(res2.value) if res2.value
          value = {cause: result.error, trace: []} if !value
          @makeErrorInline(value, editor, range)
      else
        value = {cause: result.error, trace: []}
        @makeErrorInline(value, editor, range)
      @handleWatches()
      @updateStatusbar()

  wrapText: (text, session) ->
    if session == 'cljs'
      text
    else
      "(try #{text}\n(catch Exception e
        (do
          (reset! clj.--check-deps--/last-exception
            {:cause (str e)
             :trace (map clj.--check-deps--/prettify-stack (.getStackTrace e))}))
          (throw e)))"

  makeErrorInline: ({cause, trace}, editor, range) ->
    result = new protoRepl.ink.Result(editor, [range.start.row, range.end.row], type: "block")
    causeHtml = document.createElement('strong')
    causeHtml.classList.add('error-description')
    causeHtml.innerText = cause
    invert = atom.config.get("clojure-plus.invertStack")

    strTrace = cause
    if invert
      strTrace = "\n" + strTrace
    else
      strTrace += "\n"

    traceHtmls = trace.map (row) =>
      if invert
        strTrace = "\n\tin #{row.fn}\n\tat #{row.file}:#{row.line}\n" + strTrace
      else
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
            code = "(let [[_ & jar-data] (clj.--check-deps--/extract-jar-data \"#{saneLink}\")]
                      (clj.--check-deps--/decompress-all \"#{tmp}\" jar-data))"
            @getCommands().promisedRepl.syncRun(code).then (result) =>
              return unless result.value
              atom.workspace.open(protoRepl.parseEdn(result.value), searchAllPanes: true, initialLine: row.line-1)
          else
            atom.workspace.open(row.link, searchAllPanes: true, initialLine: row.line-1)
      div

    treeHtml = protoRepl.ink.tree.treeView(causeHtml, traceHtmls, {})
    result.setContent(treeHtml, error: true)
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

  handleWatches: ->
    pr = @getCommands().promisedRepl
    pr.syncRun('(map (fn [[k v]] (str k "#" (with-out-str (print-method v *out*)))) @clj.--check-deps--/watches)',
      "user").then (result) => @updateInAtom(result)
    if @getCommands().cljs
      pr.syncRun('(map (fn [[k v]] (str k "#" v)) @clj.--check-deps--/watches)',
        "user", session: "cljs").then (result) => @updateInAtom(result)

  updateInAtom: (result) ->
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
    @updateStatusbar(atom.config.get("clojure-plus.simpleRefresh"))

  updateStatusbar: (simple, item) ->
    return unless @statusBarTile
    text = if item instanceof TextEditor
      if @evalModes.get(item.getBuffer().id) == 'cljs'
        "ClojureScript"
      else
        "Clojure"
    else
      "Clojure"

    text += ", CLJS REPL active" if @commands?.cljs
    text += ", refreshing "
    text += if simple then "(simple)" else "(full)"

    if atom.config.get('clojure-plus.refreshAfterSave')
      text += " after saving"
    @statusBarTile.item.innerText = text

  consumeInk: (ink) ->
    window.ink = ink

  deactivate: ->
    @evalModes.clear()
    @statusBarTile?.destroy()
    @statusBarTile = null
    @subs.dispose()
