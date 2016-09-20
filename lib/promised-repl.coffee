# In the future, this should be a factory so we're able to abstract more than one
# repl. Right now, we're only abstracting "proto-repl".

module.exports = class PromisedRepl
  constructor: (@repl) -> @clear()

  clear: ->
    @lastCmd = Promise.resolve()

  syncRun: (code, ns={}, options={}) ->
    lastPromise = @lastCmd
    @lastCmd = new Promise (resolve) =>
      options = ns unless typeof ns == 'string'
      options = Object.create(options)
      options.displayInRepl ?= false
      options.resultHandler = (result) =>
        resolve(result)

      f = =>
        if(typeof ns == 'string')
          options.ns = ns
          @repl.executeCode code, options
        else
          @repl.executeCodeInNs code, options
      lastPromise.then(f)
      lastPromise.catch(f)

  runCodeInCurrentNS: (code) ->
    new Promise (resolve) =>
      @repl.executeCodeInNs code, displayInRepl: false, resultHandler: (result) =>
        resolve(result)

  runCodeInNS: (code, ns) ->
    new Promise (resolve) =>
      @repl.executeCode code, displayInRepl: false, ns: ns, resultHandler: (result) =>
        resolve(result)
