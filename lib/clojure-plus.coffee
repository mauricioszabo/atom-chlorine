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
