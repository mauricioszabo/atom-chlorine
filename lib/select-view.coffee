{SelectListView} = require 'atom-space-pen-views'

module.exports = class SelectView extends SelectListView
  initialize: (items) ->
    super
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
    e = atom.workspace.getActiveTextEditor()
    atom.views.getView(e).focus()

  cancelled: ->
    @panel.destroy()
