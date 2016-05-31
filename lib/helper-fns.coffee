{TextEditor} = require 'atom'
{$, TextEditorView} = require 'atom-space-pen-views'
https = require 'https'
shell = require 'shell'
path = require 'path'
child = require 'child_process'

module.exports =
  treatErrors: (obj) ->
    msgs = obj.stdout.toString()
    errors = obj.stderr.toString()

    if obj.status == 0
      msgs += errors
      atom.notifications.addSuccess("Success", detail: msgs)
    else
      errors += msgs
      atom.notifications.addError("Error running command", detail: errors)

  runAsyncGitCommand: (params...) -> new Promise (resolve) =>
    obj = child.spawn('git', params, cwd: path.dirname(@getFilename()))
    msg = ''
    obj.stdout.on 'data', (d) -> msg += d
    obj.stderr.on 'data', (d) -> msg += d
    obj.on 'close', (status) ->
      if status == 0
        atom.notifications.addSuccess("Success", detail: msg)
      else
        atom.notifications.addError("Error running command", detail: msg)
      resolve(status, msg)

  findGithubRepository: ->
    fileName = atom.workspace.getActiveTextEditor().getPath()
    out = @runGitCommand('remote', '-v')
    repo = out.stdout.toString().split("\n").filter((row) -> row.match(/origin.*push/))[0]
    "https://" + repo.split(/@|\s/)[2].replace(':', '/').replace(".git", "")

  currentBranch: ->
    @runGitCommand('rev-parse', '--abbrev-ref', 'HEAD').stdout.toString().trim()

  getFilename: -> atom.workspace.getActiveTextEditor().getPath()

  runGitCommand: (params...) ->
    child.spawnSync('git', params, cwd: path.dirname(@getFilename()))

  separateDiffs: (diffContents) ->
    files = {}
    fileName = null
    diffContents.split("\n").forEach (row) ->
      if row.match(/^diff/)
        fileName = row.replace(/diff.*?a/, 'a')
        files[fileName] ?= ''
      else
        files[fileName] += "#{row}\n"
    files

  promptEditor: (placehold, fn) ->
    te = new TextEditorView(mini: true, placeholderText: placehold)
    div = $('<div>')
    div.append(te)
    editor = new TextEditor()
    div.append(atom.views.getView(editor))
    panel = atom.workspace.addModalPanel(item: div)

    atom.commands.add te.element, 'core:confirm': ->
      fn(te.getText())
      panel.destroy()
      atom.views.getView(atom.workspace).focus()
    , 'core:cancel': ->
      panel.destroy()
      atom.views.getView(atom.workspace).focus()
    setTimeout ->
      te.focus()
      te.getModel().scrollToCursorPosition()

    workspaceSize = $(atom.views.getView(atom.workspace)).height() * 0.8
    editorView = $(atom.views.getView(editor))
    editorView.height(workspaceSize)
    editor

  prompt: (placehold, fn) ->
    te = new TextEditorView(mini: true, placeholderText: placehold)
    panel = atom.workspace.addModalPanel(item: te)
    atom.commands.add te.element, 'core:confirm': ->
      fn(te.getText())
      panel.destroy()
      atom.views.getView(atom.workspace).focus()
    , 'core:cancel': ->
      panel.destroy()
      atom.views.getView(atom.workspace).focus()
    setTimeout ->
      te.focus()
      te.getModel().scrollToCursorPosition()
