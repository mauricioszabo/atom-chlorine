{CompositeDisposable} = require 'atom'
h = require './helper-fns'
DiffEditor = require './editor'

module.exports =
  activate: (state) ->
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
    mark = editor.markBufferRange(region, invalidate: "touch")
    return unless mark?
    mark.expression = expression
    editor.decorateMarker(mark, type: "highlight", class: "clojure-watch-expr " + type)

  executeTopLevel ->
    editor = atom.workspace.getActiveTextEditor()
    # Repl.EditorUtils.getCursorInBlockRange(editor, {topLevel: true})

    # Copy-paste from proto-repl... sorry...
    if editor = atom.workspace.getActiveTextEditor()
      if range = EditorUtils.getCursorInBlockRange(editor, options)
        text = editor.getTextInBufferRange(range).trim()
        text = @updateWithMarkers(editor, text)
        options.displayCode = text

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

        @executeCodeInNs(text, options)

  updateWithMarkers: (editor, text) ->
