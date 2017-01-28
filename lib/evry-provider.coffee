code = "(map (fn [e]
               {:displayName (:fqname e)
                :additionalInfo (:file e)
                :line (:line e)
                :column (:column e)
                :fqpath (:fqpath e)
                :queryString (str (:fqname e) (:file e))})
             (clj.--check-deps--/symbols-in-project))"

module.exports = class EvryProvider
  name: "clj-symbols"
  defaultPrefix: "@"

  constructor: (@plus) ->

  onStart: (@evry) ->
    @promise = null

  onQuery: (query) ->
    return Promise.resolve([]) if query.length < 5

    @promise ?= @plus.getCommands().promisedRepl.syncRun(code).then (res) =>
      if res.value
        protoRepl.parseEdn(res.value)
      else
        []

    @promise.then (results) =>
      @evry.fuzzaldrin.filter(results, query, key: 'queryString').map (item) =>
        item.function = => @plus.getCommands().openFileContainingVar(item.displayName)
        item
