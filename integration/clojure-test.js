const assert = require('assert')
const Application = require('spectron').Application
const app = new Application({
  path: '/usr/bin/atom-beta',
  args: ['--dev', '--socket-path=/tmp/atom-dev-socket-1.sock', '/tmp/test.clj']
})

const sendJS = (cmd) =>
  app.client.execute(cmd)

const sendCommand = (cmd) =>
  sendJS(`atom.commands.dispatch(document.activeElement, "${cmd}")`)

const openAndConnect = async () => {
  app.start()

  sendCommand("pane:show-next-item")
  app.client.keys("(ns user.test1)")
  sendCommand("chlorine:connect-clojure-socket-repl")
  app.client.keys("Tab")
  app.client.keys("46739")
  app.client.keys("Enter")
  // sendJS('atom.workspace.getActiveTextEditor().getFileName()').then(console.log)
}

const haveText = (text) => app.client.waitForText(text)

describe('Atom should open and evaluate code', function () {
  this.timeout(30000)

  it('opens an atom window', async () => {
    await app.start()
    assert.ok(await app.browserWindow.isVisible())
  })

  it('connects to editor', async () => {
    await sendCommand("pane:show-next-item")
    const title = await app.browserWindow.getTitle()
    assert.ok(title.match(/test\.clj/))
    await sendCommand("chlorine:connect-clojure-socket-repl")
    assert.ok(await haveText('div*=Connect to Socket REPL'))
    app.client.keys("Tab")
    app.client.keys("46739")
    app.client.keys("Enter")
    assert.ok(await haveText("div*=Console"))
    await sendCommand("window:focus-next-pane")
  })

  it('evaluates Clojure code', async () => {
    await sendCommand("vim-mode-plus:activate-insert-mode")
    await app.client.keys("(ns user.test1)")
    await sendCommand("chlorine:evaluate-block")
    assert.ok(await haveText('div*=nil'))

    await app.client.keys("\n\n(str (+ 90 120))")
    await sendCommand("chlorine:evaluate-block")
    assert.ok(await haveText(`//div[contains(., '"210"')]`))
  })
})
