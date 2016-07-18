# In the future, this should be a factory so we're able to abstract more than one
# repl. Right now, we're only abstracting "proto-repl".

module.exports = class PromisedRepl
  constructor: (@repl) ->
    @lastCmd = Promise.resolve()

  syncRun: (code, ns) =>
    lastPromise = @lastCmd
    @lastCmd = new Promise (resolve) =>
      lastPromise.then =>
        @lastCmd = if(ns)
          @runCodeInNS(code, ns).then (v) => resolve(v)
        else
          @runCodeInCurrentNS(code).then (v) => resolve(v)

  runCodeInCurrentNS: (code) ->
    new Promise (resolve) =>
      @repl.executeCodeInNs code, displayInRepl: false, resultHandler: (result) =>
        resolve(result)

  runCodeInNS: (code, ns) ->
    new Promise (resolve) =>
      @repl.executeCode code, displayInRepl: false, ns: ns, resultHandler: (result) =>
        resolve(result)
