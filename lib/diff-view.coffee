{$} = require 'atom-space-pen-views'
path = require 'path'
child = require 'child_process'
Editor = require './editor'

module.exports =
  newDiffView: (filePath, contents) ->
    if contents.trim() == ''
      contents = atom.workspace.getActiveTextEditor().getText()
      contents = contents.replace(/^(.?)/gm, " $1")

    contents = contents.replace(/(.*?\n)*?@@.*?\n/, '')

    parts = filePath.split(/[\/\\]/)
    file = parts[parts.length-1]

    promise = atom.workspace.open("(diff) #{file}")
    promise.promiseDispatch =>
      atomEditor = promise.valueOf()
      editor = new Editor(atomEditor)
      editor.setDiff(filePath, contents)

      # LOGS
      div = $('<div>')
      $('::shadow div').find('.scroll-view:visible').parent().append(div)
      div.append(
        $('<p>').append(
          $('<a>').html("HEAD").on('click', => editor.setDiff(filePath, contents))
        )
      )

      out = child.spawnSync('git', ['log', '--date=short',
        '--format=format:%h##..##%ad##..##%an##..##%s', '--follow', filePath],
        cwd: path.dirname(filePath))

      out.stdout.toString().split("\n").forEach (row) =>
        [hash, date, author, message] = row.split("##..##")
        p = $('<p>')
        a = $('<a>').html(hash).on 'click', =>
          diff = child.spawnSync('git', ['diff', '-U999999',
            "#{hash}^..#{hash}", filePath], cwd: path.dirname(filePath)).stdout.toString()
          editor.setDiff(filePath, diff.replace(/(.*?\n)*?@@.*?\n/, ''))
        a.css('cursor', 'pointer')
        p.append(a).append(" #{date} #{message} (#{author})")
        div.append(p)

      div.css(width: '400px', height: '100%', float: 'right', 'background': 'white')
      div.css('margin-right': '15px', 'overflow': 'scroll')
