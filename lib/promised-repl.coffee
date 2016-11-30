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

  runCodeInCurrentNS: (code, opts={}) ->
    new Promise (resolve) =>
      opts.displayInRepl = false
      opts.resultHandler = (result) => resolve(result)
      @repl.executeCodeInNs code, opts

  runCodeInNS: (code, ns, opts={}) ->
    new Promise (resolve) =>
      opts.displayInRepl = false
      opts.resultHandler = (result) => resolve(result)
      opts.ns = ns
      @repl.executeCode code, opts
