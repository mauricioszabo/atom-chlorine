{CompositeDisposable} = require('atom')

disposables = new CompositeDisposable
marks = []

fn = (editor) =>
  mark = editor.markBufferRange([0,0], invalidate: 'never')
  marks.push(mark)

  editor.decorateMarker(mark, type: 'highlight', class: 'clojure-sexp')
  disposables.add editor.onDidChangeCursorPosition =>
    unless editor.getGrammar().scopeName.match(/clojure/)
      mark.setBufferRange([0,0])
      return

    blockRange = protoRepl.EditorUtils.getCursorInClojureBlockRange(editor)
    blockRange ?= editor.getSelectedBufferRange()
    mark.setBufferRange(blockRange)

module.exports = (willObserve) ->
  if willObserve
    disposables.add atom.workspace.observeTextEditors(fn)
    atom.workspace.getTextEditors().forEach(fn)
  else
    marks.forEach (m) -> m.destroy()
    marks = []
    disposables.dispose()
    disposables.clear()
