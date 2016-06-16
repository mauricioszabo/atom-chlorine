{SelectListView} = require 'atom-space-pen-views'

module.exports = class SelectView extends SelectListView
  initialize: (items) ->
    super
    @storeFocusedElement()
    @addClass('overlay from-top')
    @setItems(items)
    @panel ?= atom.workspace.addModalPanel(item: this)
    @panel.show()

    @focusFilterEditor()

  viewForItem: (item) ->
    "<li>#{item.label}</li>"

  getFilterKey: ->
    "label"

  confirmed: (item) ->
    item.run()
    @cancel()

  cancelled: ->
    @panel.destroy()
