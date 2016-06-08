{CompositeDisposable} = require 'atom'
SelectView = require './select-view'
fs = require 'fs'

module.exports =
  currentWatches: {}
  lastClear: null

  activate: (state) ->
    atom.commands.add 'atom-text-editor', 'clojure-plus:watch-expression', =>
      @markCustomExpr
        type: "watch"
        expression: "(do
          (println 'swapping!)
          (swap! user/__watches__ update-in [..ID..] #(conj (or % []) ..SEL..)) ..SEL..)"
        #expression: "(do (println ..SEL.. ) ..SEL..)"

    setTimeout ->
      protoRepl.onDidConnect ->
        protoRepl.executeCode("(def __watches__ (atom {}))", ns: "user", displayInRepl: false)
    , 5000


    atom.commands.add 'atom-text-editor', 'clojure-plus:evaluate-top-block', =>
      @executeTopLevel()

    atom.commands.add 'atom-text-editor', 'clojure-plus:import-for-missing-symbol', =>
      @importForMissing()

  importForMissing: ->
      editor = atom.workspace.getActiveTextEditor()
      varRange = editor.getLastCursor().getCurrentWordBufferRange(wordRegex: /[a-zA-Z0-9\-.$!?\/><*]+/)
      varNameRaw = editor.getTextInBufferRange(varRange)
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
    # Repl.EditorUtils.getCursorInBlockRange(editor, {topLevel: true})

    # Copy-paste from proto-repl... sorry...
    if editor = atom.workspace.getActiveTextEditor()
      if range = protoRepl.EditorUtils.getCursorInBlockRange(editor, topLevel: true)
        text = editor.getTextInBufferRange(range).trim()
        text = @updateWithMarkers(editor, text, range)

        # Highlight the area that's being executed temporarily
        marker = editor.markBufferRange(range)
        decoration = editor.decorateMarker(marker,
            {type: 'highlight', class: "block-execution"})
        # Remove the highlight after a short period of time
        setTimeout(=>
          marker.destroy()
        , 350)

        options =
          displayCode: text
          resultHandler: (a,b) => @scheduleWatch(a, b)
          displayInRepl: false
          inlineOptions:
            editor: editor
            range: range

        protoRepl.executeCodeInNs "(do (in-ns 'user) (def __watches__ (atom {})))", ns: "user", displayInRepl: true
        protoRepl.executeCodeInNs(text, options)

  scheduleWatch: (result, options) ->
    delete options.resultHandler
    protoRepl.repl.inlineResultHandler(result, options)
    # protoRepl.executeCode("@user/__watches__",
    protoRepl.executeCode '(map (fn [[k v]] (str k "#" (with-out-str (print-method v *out*)))) @user/__watches__)',
      displayInRepl: false, resultHandler: (res) => @handleWatches(res)

  handleWatches: (result, options) ->
    return unless result.value
    values = protoRepl.parseEdn(result.value)
    console.log(result.values, values)
    for row in values
      id = row.replace(/#.*/, "")
      data = row.replace(/\d+#/, "")
      console.log(row, @currentWatches, id, data)
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
