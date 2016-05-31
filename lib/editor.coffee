{$} = require 'atom-space-pen-views'

module.exports = class
  constructor: (editor) ->
    @id = "id" + Math.random().toString().replace('.', '')
    @editor = editor
    @view = atom.views.getView(editor)
    @view.classList.add('diff-view')
    @view.id = @id
    @lineMaps = {}

    observer = new MutationObserver (mutations) => mutations.forEach (m) =>
      for element in m.addedNodes
        @updateElement(element)

    @element = $("##{@id}::shadow .gutter-container")
    observer.observe(@element[0], childList: true, subtree: true)
    @editor.onWillInsertText (evt) -> evt.cancel()
    @editor.isModified = -> false

  updateElement: (element) ->
    element = $(element)
    line = element.data('buffer-row')
    if line?
      html = element.html().replace(/\d+/, '&nbsp;')
      newLine = @lineMaps[line]
      if newLine
        element.html(html.replace(/(&nbsp;)+/, newLine))
      else
        element.html(html)

  setDiff: (file, contents, firstLine=1) ->
    lastLine = firstLine - 1
    @lineMaps = {}
    # contents = contents.replace(/(.*?\n)*?@@.*?\n/, '')
    contents = contents.replace(/^index.*\n/gm, '').replace(/^\-\-\-.*\n/gm, '')
      .replace(/^\+\+\+.*\n/gm, '')
    # @editor.setText(contents.)
    # contents.replace(/^@.*/g, "  (...)")

    # We can replace anything without changing line numbers.
    editorContents = contents
      .replace(/^@.*/gm, "## (...)") #Remove @@ -10,19 +10,20, for example
      .replace(/diff.*?a\/(.+)\sb\/.*/gm, "## Diff for file $1") #Remove diff --git...
      .replace(/^./gm, '') #Remove the first char of each line
    @editor.setText(editorContents)

    text = contents.split("\n").forEach (row, index) =>
      if row[0] == "+" || row[0] == "-"
        marker = @editor.markBufferRange([[index, 0], [index, row.length]])
        className = if(row[0] == "+") then "plus" else "minus"
        @editor.decorateMarker(marker, type: 'line-number', class: className)
        marker = @editor.markBufferRange([[index, 0], [index, row.length]])
        @editor.decorateMarker(marker, type: 'line', class: className)
      else if row[0] == '@'
        lastLine = parseInt(row.match(/@@.*\+(\d+),/)[1]) - 1

      if row[0] == '+' || row[0] == ' '
        lastLine += 1
        @lineMaps[index] = lastLine

    @editor.setGrammar(atom.grammars.selectGrammar(file, contents))
    @refresh()

  refresh: ->
    # Re-update
    for element in @element.find('div.line-number')
      @updateElement(element)
