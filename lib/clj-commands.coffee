fs = require 'fs'
process = require 'process'

module.exports = class CljCommands
  runRefresh: (all) ->
    before = atom.config.get('clojure-plus.beforeRefreshCmd')
    after = atom.config.get('clojure-plus.afterRefreshCmd')

    shouldRefreshAll = all || !@lastRefreshSucceeded
    refreshCmd = @getRefreshCmd(shouldRefreshAll)

    notify = atom.config.get('clojure-plus.notify')
    protoRepl.executeCode before, ns: "user", displayInRepl: false
    protoRepl.executeCode refreshCmd, ns: "user", displayInRepl: false, resultHandler: (result) =>
      console.log "Refreshed? ", result
      if result.value
        value = protoRepl.parseEdn(result.value)
        if !value.cause
          @lastRefreshSucceeded = true
          atom.notifications.addSuccess("Refresh successful.") if notify
          protoRepl.appendText("Refresh successful.")
        else
          @lastRefreshSucceeded = false
          causes = value.via.map (e) -> e.message
          causes = "#{value.cause}\n#{causes.join("\n")}"
          atom.notifications.addError("Error refreshing.", detail: causes) if notify
          protoRepl.appendText("Error refreshing. CAUSE: #{value.cause}\n")
          protoRepl.appendText(result.value)
      else if !shouldRefreshAll
        @runRefresh(true)
      else
        atom.notifications.addError("Error refreshing.", detail: value.error) if notify
        protoRepl.appendText("Error refreshing. CAUSE: #{value.error}\n")
    protoRepl.executeCode after, ns: "user", displayInRepl: false

  getRefreshCmd: (all) ->
    key = if all then 'clojure-plus.refreshAllCmd' else 'clojure-plus.refreshCmd'
    file = @translateFile(atom.config.get(key))
    fs.readFileSync(file).toString()

  translateFile: (file) ->
    home = process.env.HOME
    file.replace("~", home)
