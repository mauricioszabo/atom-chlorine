{CompositeDisposable} = require 'atom'
h = require './helper-fns'
DiffEditor = require './editor'

module.exports =
  activate: (state) ->
    atom.commands.add 'atom-workspace', 'git-repository:update-master', ->
      h.runAsyncGitCommand('checkout', 'master').then (code) ->
        if code == 0
          h.runAsyncGitCommand('pull', 'origin')

    atom.commands.add 'atom-workspace', 'git:quick-commit-current-file', =>
      @commitWithDiff(['diff', 'HEAD', h.getFilename()], h.getFilename())

    atom.commands.add 'atom-workspace', 'git:commit', =>
      @commitWithDiff(['diff', '--staged'])

    atom.commands.add 'atom-workspace', 'git:push-current-branch', ->
      h.runAsyncGitCommand('push', '--set-upstream', 'origin', h.currentBranch())

    atom.commands.add 'atom-workspace', 'git:add-current-file', ->
      h.treatErrors h.runGitCommand('add', h.getFilename())

    atom.commands.add 'atom-workspace', 'git:revert-current-file', ->
      h.treatErrors h.runGitCommand('checkout', h.getFilename())

    atom.commands.add 'atom-workspace', 'git:show-diff-for-current-file', ->
      path = atom.workspace.getActiveTextEditor().getPath()
      out = h.runGitCommand('diff', '-U999999', path)
        .stdout.toString()
      diff.newDiffView(path, out)

    # atom.commands.add 'atom-workspace', 'git:diff-layer', ->
    #   path = atom.workspace.getActiveTextEditor().getPath()
    #   out = h.runGitCommand('diff', '-U999999', path)
    #     .stdout.toString()
    #   diff.newDiffView(path, out)

    atom.commands.add 'atom-workspace', 'git:toggle-blame', => @toggleBlame()

  commitWithDiff: (gitParams, filename) ->
    cont = h.runGitCommand(gitParams...).stdout.toString()

    if cont
      editor = h.promptEditor "Type your commit message", (commit) ->
        if filename
          h.treatErrors h.runGitCommand('commit', filename, '-m', commit)
        else
          h.treatErrors h.runGitCommand('commit', '-m', commit)

      diffEditor = new DiffEditor(editor)
      startLine = cont.match(/@@.*?(\d+)/)[1]
      cont = cont.replace(/(.*?\n)*?@@.*?\n/, '')
      diffEditor.setDiff(h.getFilename(), cont, parseInt(startLine))
      diffEditor.view.classList.add('commit')
    else
      atom.notifications.addError("Failed to commit", detail: "Nothing to commit...
      Did you forgot to add files, or the current file have any changes?")

  toggleBlame: ->
    editor = atom.workspace.getActiveTextEditor()
    if !editor.blameDecorations
      editor.blameDecorations = []
      editor.onDidSave =>
        @toggleBlame()
        @toggleBlame()

    if editor.blameDecorations.length == 0
      blames = @getBlames(editor.getPath())

      for line, {author, commit, time} of blames
        div = document.createElement('div')
        div.textContent = "#{author} made these changes on commit #{commit} at #{time}"
        div.classList.add('blame')
        div.classList.add('decoration')
        marker = editor.markScreenPosition([parseInt(line), 0])
        editor.blameDecorations.push(marker)
        editor.decorateMarker(marker, type: 'block', position: 'before', item: div)

    else
      editor.blameDecorations.forEach (m) -> m.destroy()
      editor.blameDecorations = []

  getBlames: (path) ->
    formatted = {}
    blames = h.runGitCommand('blame', '-M', '-w', '-c', path).stdout.toString().split("\n")
    lastLine = {}

    blames.forEach (row, number) =>
      [commit, author, timestamp] = row.split("\t")
      data = if author && commit != '00000000'
        {author: author.substring(1).trim(), commit: commit, time: timestamp}
      else
        {author: "YOU", commit: '<none>', time: '<none>'}
      formatted[number] = data if !@sameLines(data, lastLine)
      lastLine = data

    formatted

  sameLines: (d1, d2) ->
    {author, commit, time} = d1
    [a1, c1, t1] = [author, commit, time]
    {author, commit, time} = d2
    [a2, c2, t2] = [author, commit, time]

    a1 == a2 && c1 == c2 && t1 == t2
#
#   deactivate() {
#   },
#
#   serialize() {
#   },
# };
