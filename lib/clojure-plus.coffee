{CompositeDisposable} = require 'atom'
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
    tempDir:
      description: "Temporary directory to unpack JAR files (used by goto-var)"
      type: "string"
      default: "/tmp/jar-path"

  currentWatches: {}
  lastClear: null

  everythingProvider: -> new EvryProvider()

  activate: (state) ->
    atom.commands.add 'atom-text-editor', 'clojure-plus:refresh-namespaces', =>
      @commands.runRefresh()
    atom.commands.add 'atom-text-editor', 'clojure-plus:goto-var-definition', =>
      @commands.openFileContainingVar()
    atom.commands.add 'atom-text-editor', 'clojure-plus:clear-and-refresh-namespaces', =>
      @commands.runRefresh(true)

    atom.commands.add 'atom-text-editor', 'clojure-plus:test-item', =>
      new SelectView([{label: "FOO"}, {label: "FAR"}])

    atom.commands.add 'atom-text-editor', 'clojure-plus:watch-expression', =>
      @markCustomExpr
        type: "watch"
        expression: "(do
          (println 'swapping!)
          (swap! user/__watches__ update-in [..ID..] #(conj (or % []) ..SEL..)) ..SEL..)"
        #expression: "(do (println ..SEL.. ) ..SEL..)"

    atom.workspace.observeTextEditors (editor) =>
      editor.onDidSave =>
        if atom.config.get('clojure-plus.refreshAfterSave') && editor.getGrammar().scopeName == 'source.clojure'
          @commands.runRefresh()

    atom.packages.onDidActivatePackage (pack) =>
      if pack.name == 'proto-repl'
        @commands = new CljCommands(@currentWatches, protoRepl)

        protoRepl.onDidConnect =>
          @commands.prepare()

          if atom.config.get('clojure-plus.refreshAfterConnect')
            @commands.runRefresh()

    atom.commands.add 'atom-text-editor', 'clojure-plus:evaluate-top-block', =>
      @executeTopLevel()

    atom.commands.add 'atom-text-editor', 'clojure-plus:import-for-missing-symbol', =>
      @importForMissing()

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

      nreplCode = "
	(let [edn-str (refactor-nrepl.ns.resolve-missing/resolve-missing {:symbol \"#{varName}\"})
	      name (-> edn-str read-string first :name)]
	  (when name
	    (let [aliases (refactor-nrepl.ns.libspecs/namespace-aliases)]
	      (filter #(-> % second (= name))
		      (mapcat (fn [[k vs]] (map #(vector k %) vs))
			      (apply merge (vals aliases)))))))"
      protoRepl.executeCodeInNs nreplCode, displayInRepl: true, resultHandler: (res) ->
      	result = protoRepl.parseEdn(res.value)
      	if result && result.length > 0
          items = result.map (res) ->
            text = "[#{res[1]} :as #{res[0]}]"
            label: text, run: =>
              atom.clipboard.write(text)
              editor.setTextInBufferRange(varRange, "#{res[0]}/#{varNameRaw}")
              atom.notifications.addSuccess("Import copied to clipboard!")
          new SelectView(items)
      	else
      	  atom.notifications.addError("Import with namespace alias not found")

  getRangeAndVar: (editor) ->
    varRange = editor.getLastCursor().getCurrentWordBufferRange(wordRegex: /[a-zA-Z0-9\-.$!?\/><*]+/)
    varName = editor.getTextInBufferRange(varRange)
    [varRange, varName]

  checkDependents: ->
    cljCode = fs.readFileSync(__dirname + "/clj/check_deps.clj").toString()

  markCustomExpr: ({expression, type, region}) ->
    editor = atom.workspace.getActiveTextEditor()
    return unless editor?
    region ?= editor.getLastCursor()
                    .getCurrentWordBufferRange({wordRegex: /[a-zA-Z0-9\-.$!?:\/><*]+/})
    return if @removeMarkIfExists(editor, region)

    mark = editor.markBufferRange(region, invalidate: "touch")
    return unless mark?

    cljVar = editor.getTextInBufferRange(region)
    expression = expression.replace(/\.\.SEL\.\./g, cljVar)
    expression = expression.replace(/\.\.ID\.\./g, mark.id)

    mark.expression = expression
    mark.editor = editor

    editor.decorateMarker(mark, type: "highlight", class: "clojure-watch-expr " + type)
    @currentWatches[mark.id] = mark

    topRanges = protoRepl.EditorUtils.getTopLevelRanges(editor)
    topRange = topRanges.find (range) => range.containsPoint(region.start)
    text = editor.getTextInBufferRange(topRange).trim()
    text = @updateWithMarkers(editor, text, topRange)
    mark.topLevelExpr = text

    @commands.assignWatches()

  removeMarkIfExists: (editor, region)->
    for _, mark of @currentWatches
      {start, end} = mark.getBufferRange()
      if start.column == region.start.column && start.row == region.start.row &&
         end.column == region.end.column && end.row == region.end.row
        mark.destroy()

        delete @currentWatches[mark.id]
        return true

    return false

  executeTopLevel: ->
    editor = atom.workspace.getActiveTextEditor()

    # Copy-paste from proto-repl... sorry...
    if editor = atom.workspace.getActiveTextEditor()
      if range = protoRepl.EditorUtils.getCursorInBlockRange(editor, topLevel: true)
        oldText = editor.getTextInBufferRange(range).trim()
        text = @updateWithMarkers(editor, oldText, range)

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

        protoRepl.executeCodeInNs "(do (in-ns 'user) (def __watches__ (atom {})))", ns: "user", displayInRepl: false
        protoRepl.executeCodeInNs(text, options)

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

  updateWithMarkers: (editor, text, blockRange) ->
    lines = text.split("\n")

    marks = for _, m of @currentWatches then m
    marks = marks.filter (m) ->
      buffer = m.getBufferRange()
      buffer.start.row >= blockRange.start.row && buffer.end.row <= blockRange.end.row && m.editor == editor

    marks = marks.sort (f, s) -> f.compare(s)

    lastRow = null
    for mark in marks
      range = mark.getBufferRange()
      lastCol = 0 if range.start.row != lastRow
      lastRow = range.start.row

      row = range.start.row - blockRange.start.row
      line = lines[row]

      scol = range.start.column + lastCol
      ecol = range.end.column + lastCol
      line = line.substring(0, scol) + mark.expression + line.substring(ecol)
      lines[row] = line

      lastCol = ecol - scol + lastCol

    lines.join("\n")
