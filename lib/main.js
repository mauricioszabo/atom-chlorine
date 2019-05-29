const cmds = require('./cmds')

module.exports.activate = (s) => {
  // Register subscriptions
  cmds.aux.reload()
  const disposable = cmds.aux.get_disposable()
  // Register commands
  const commands = cmds.commands()
  for(let i in commands) {
    disposable.add(
      atom.commands.add(
        'atom-workspace',
        'chlorine:' + i,
        commands[i]
      )
    )
  }

  // Register some callbacks
  cmds.aux.deps()
  cmds.aux.observe_editor()
  cmds.aux.observe_config()
}

module.exports.deactivate = (s) => {
  cmds.aux.get_disposable().dispose()
}

module.exports.config = cmds.config
module.exports.repl = cmds.repl
module.exports.everything_provider = cmds.everything_provider
module.exports.autocomplete_provider = cmds.autocomplete_provider
module.exports.status_bar_consumer = cmds.status_bar_consumer
module.exports.ink_consumer = cmds.ink_consumer
