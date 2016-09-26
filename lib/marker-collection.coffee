module.exports = class MarkerCollection
  constructor: (@markers) ->

  getTopLevelForMark: (mark) ->
    editor = mark.editor
    topRanges = protoRepl.EditorUtils.getTopLevelRanges(editor)
    regionStart = mark.getBufferRange().start
    topRanges.find (range) => range.containsPoint(regionStart)

  updatedCodeInRange: (editor, blockRange) ->
    # Get meaningful marks for this block range
    marks = for _, m of @markers then m

    marks = marks.filter (m) ->
      buffer = m.getBufferRange()
      blockRange.containsRange(buffer) && m.editor == editor

    # Sort marks by occurence in text
    marks = marks.sort (a, b) ->
      a = a.getBufferRange().start
      b = b.getBufferRange().start
      (a.row * 1000 + a.column) - (b.row * 1000 + b.column)

    text = ""
    editor.transact =>
      for mark in marks
        # mark.bufferMarker.invalidate = "never"
        editor.setTextInBufferRange(mark.getBufferRange(), @updateMarkerText(mark))

      # redo top range mark, 'cause we could have changed the limits
      topRanges = protoRepl.EditorUtils.getTopLevelRanges(editor)
      newBlockRange = topRanges.find (range) => range.containsPoint(blockRange.start)
      text = editor.getTextInBufferRange(newBlockRange).trim()
      editor.abortTransaction()

    # for mark in marks
    #   mark.bufferMarker.invalidate = "touch"
    text

  updateMarkerText: (marker) ->
    region = marker.getBufferRange()
    editor = marker.editor
    expression = marker.expression

    cljText = editor.getTextInBufferRange(region)
    expression = expression.replace(/\.\.FILE_NAME\.\./g, editor.getPath())
    expression = expression.replace(/\.\.ROW\.\./g, region.start.row + 1)
    expression = expression.replace(/\.\.COL\.\./g, region.start.column + 1)
    expression = expression.replace(/\.\.SEL\.\./g, cljText)
    expression = expression.replace(/\.\.ID\.\./g, marker.id)
    expression
