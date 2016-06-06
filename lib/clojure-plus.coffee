{CompositeDisposable} = require 'atom'

module.exports =
  activate: (state) ->
    atom.commands.add 'atom-text-editor', 'clojure-plus:watch-expression', =>
      @markCustomExpr
        type: "watch"
        expression: "(do (println ..SEL.. ) ..SEL..)"

    atom.commands.add 'atom-text-editor', 'clojure-plus:evaluate-top-block', =>
      @executeTopLevel()

    atom.commands.add 'atom-text-editor', 'clojure-plus:import-for-missing-symbol', ->
      editor = atom.workspace.getActiveTextEditor()
      varName = protoRepl.EditorUtils.getClojureVarUnderCursor(editor).replace(/"/g, '\\"')
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
      	  text = "[#{result[0][1]} :as #{result[0][0]}]"
      	  atom.clipboard.write(text)
      	  atom.notifications.addSuccess("Import copied to clipboard!")
      	else
      	  atom.notifications.addError("Import with namespace alias not found")

  markCustomExpr: ({expression, type, region}) ->
    editor = atom.workspace.getActiveTextEditor()
    return unless editor?
    region ?= editor.getLastCursor()
                    .getCurrentWordBufferRange({wordRegex: /[a-zA-Z0-9\-.$!?\/><*]+/})
    return if @removeMarkIfExists(editor, region)

    mark = editor.markBufferRange(region, invalidate: "touch")
    return unless mark?

    cljVar = editor.getTextInBufferRange(region)
    expression = expression.replace(/\.\.SEL\.\./g, cljVar)
    mark.expression = expression
    editor.decorateMarker(mark, type: "highlight", class: "clojure-watch-expr " + type)

  removeMarkIfExists: (editor, region)->
    for mark in editor.getMarkers() when mark.expression
      {start, end} = mark.getBufferRange()
      if start.column == region.start.column && start.row == region.start.row &&
         end.column == region.end.column && end.row == region.end.row
        mark.destroy()
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
        options = {displayCode: text}

        # Highlight the area that's being executed temporarily
        marker = editor.markBufferRange(range)
        decoration = editor.decorateMarker(marker,
            {type: 'highlight', class: "block-execution"})
        # Remove the highlight after a short period of time
        setTimeout(=>
          marker.destroy()
        , 350)

        options.inlineOptions =
          editor: editor
          range: range

        protoRepl.executeCodeInNs(text, options)

  updateWithMarkers: (editor, text, blockRange) ->
    lines = text.split("\n")

    marks = editor.getMarkers().filter (m) ->
      buffer = m.getBufferRange()
      buffer.start.row >= blockRange.start.row && buffer.end.row <= blockRange.end.row

    marks = marks.sort (f, s) -> f.compare(s)
      # b1 = f.getBufferRange()
      # b2 = s.getBufferRange()
      # row = b1.start.row - b2.start.row
      # if row == 0
      #   b1.start.column - b2.start.column
      # else
      #   row

    lastRow = null
    for mark in marks when mark.expression
      range = mark.getBufferRange()
      lastCol = 0 if range.start.row != lastRow
      lastRow = range.start.row

      # console.log("RANGE:", range)
      row = range.start.row - blockRange.start.row
      line = lines[row]
      # console.log("LINE:", line)

      scol = range.start.column + lastCol
      ecol = range.end.column + lastCol
      line = line.substring(0, scol) + mark.expression + line.substring(ecol)
      lines[row] = line

      lastCol = ecol - scol + lastCol

    lines.join("\n")
