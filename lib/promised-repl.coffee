# In the future, this should be a factory so we're able to abstract more than one
# repl. Right now, we're only abstracting "proto-repl".

module.exports = class PromisedRepl
  constructor: (@repl) ->

  runCodeInCurrentNS: (code) ->
    new Promise (resolve) =>
      @repl.executeCodeInNs code, displayInRepl: false, resultHandler: (result) =>
        resolve(result)

  runCodeInNS: (code, ns) ->
    new Promise (resolve) =>
      @repl.executeCode code, displayInRepl: false, ns: ns, resultHandler: (result) =>
        resolve(result)
