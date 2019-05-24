const cmds = require('./cmds')

module.exports.activate = (s) => {
  let i
  for(i in cmds.commands) {
    atom.commands.add(
      'atom-workspace',
      'chlorine:' + i,
      cmds.commands[i]
    )
  }
}

module.exports.config = cmds.config
module.exports.repl = cmds.repl
module.exports.everything_provider = cmds.everything_provider
module.exports.autocomplete_provider = cmds.autocomplete_provider
module.exports.status_bar_consumer = cmds.status_bar_consumer
module.exports.ink_consumer = cmds.ink_consumer
