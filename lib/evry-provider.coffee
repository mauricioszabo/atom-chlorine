module.exports = class EvryProvider
  name: "clj-symbols"
  defaultPrefix: "@"

  onStart: (@evry) ->
    code = "(map (fn [e]
                   {:displayName (:fqname e)
                    :additionalInfo (:file e)
                    :line (:line e)
                    :column (:column e)
                    :fqpath (:fqpath e)
                    :queryString (str (:fqname e) (:file e))})
                 (check-deps/symbols-in-project))"

    @promise = new Promise (resolve) =>
      if window.protoRepl
        protoRepl.executeCode code, displayInRepl: false, resultHandler: (res) =>
          if res.value
            resolve(protoRepl.parseEdn(res.value))
          else
            resolve([])
      else
        resolve([])

  onQuery: (query) -> @promise.then (results) =>
    return [] if query.length < 5

    @evry.fuzzaldrin.filter(results, query, key: 'queryString').map (item) =>
      item.function = =>
        filePath = item.fqpath.replace(/"/g, '\\"').replace(/\\/, "\\\\")
        code = "(check-deps/fq-path-to-relative \"#{filePath}\")"
        protoRepl.executeCode code, displayInRepl: false, resultHandler: (res) =>
          if(res.value)
            line = Number(item.line) - 1
            column = Number(item.column) - 1
            atom.workspace.open(protoRepl.parseEdn(res.value), initialLine: line, initialColumn: column)
          else
            atom.notifications.addError("Error opening file #{filePath}")
      item

window.P = EvryProvider
