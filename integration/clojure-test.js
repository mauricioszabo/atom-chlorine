const assert = require('assert')
const Application = require('spectron').Application
const app = new Application({
  path: '/usr/bin/atom',
  args: ['--dev', '--socket-path=/tmp/atom-dev-socket-1.sock', '/tmp/test.clj', '/tmp/test2.cljs']
})

const sendJS = (cmd) =>
  app.client.execute(cmd)

const sendCommand = (cmd) =>
  sendJS(`atom.commands.dispatch(document.activeElement, "${cmd}")`)

const haveSelector = (sel) => app.client.waitForText(sel)

const haveText = (text) =>
  haveSelector(`//div[contains(., '${text}')]`)

const evalCommand = async (cmd) => {
  await sendCommand("vim-mode-plus:activate-insert-mode")
  await app.client.keys(`\n\n${cmd}`)
  await sendCommand("chlorine:evaluate-block")
}

const gotoTab = async (fileName) => {
  let i
  for(i=0; i < 10; i++) {
    await sendCommand("pane:show-next-item")
    const title = await app.browserWindow.getTitle()
    if(title.match(fileName)) return true
  }
  return false
}

const time = (time) => new Promise(res => {
  setTimeout(() => res(true), time)
})

describe('Atom should open and evaluate code', function () {
  after(() => {
    app.stop()
  })

  this.timeout(30000)

  it('opens an atom window', async () => {
    await app.start()
    assert.ok(await app.browserWindow.isVisible())
  })

  it('connects to editor', async () => {
    await sendCommand("chlorine:connect-clojure-socket-repl")
    assert.ok(await haveSelector('div*=Connect to Socket REPL'))
    await app.client.keys("Tab")
    await app.client.keys("3333")
    await app.client.keys("Enter")
    assert.ok(await haveSelector("div*=Console"))
    await sendCommand("window:focus-next-pane")
    assert.ok(await gotoTab('test.clj'))
  })

  describe('when connecting to Clojure', () => {
    it('evaluates code', async () => {
      await time(1000)
      await sendCommand("vim-mode-plus:activate-insert-mode")
      // await time(1000)
      assert.ok(await gotoTab('test.clj'))
      await app.client.keys("(ns user.test1)")
      await sendCommand("chlorine:evaluate-block")
      assert.ok(await haveSelector('div*=nil'))

      await app.client.keys("\n\n(str (+ 90 120))")
      await sendCommand("chlorine:evaluate-block")
      assert.ok(await haveSelector(`//div[contains(., '"210"')]`))
    })

    it('shows function doc', async () => {
      await sendCommand('inline-results:clear-all')
      await app.client.keys("\n\nstr")
      await sendCommand("chlorine:doc-for-var")
      assert.ok(await haveText("With no args, returns the empty string. With one arg x, returns\n"))
    })

    it('captures exceptions', async () => {
      await evalCommand(`(throw (ex-info "Error Number 1", {}))`)
      assert.ok(await haveSelector(`div.error`))
      assert.ok(await haveSelector(`div*=Error Number 1`))

      await evalCommand(`(ex-info "Error Number 2", {})`)
      assert.ok(await haveText(`#error {:cause "Error Number 2"`))
    })

    it('allows big strings to be represented', async () => {
      await sendCommand('inline-results:clear-all')
      await evalCommand("(str (range 200))")
      assert.ok(await haveText("29..."))
      await app.client.click("a*=...")
      assert.ok(await haveText("52 53 54"))
      await sendCommand('inline-results:clear-all')
    })
  })

  describe('when connecting to ClojureScript inside Clojure', () => {
    it('connects to embedded ClojureScript', async () => {
      await sendCommand('chlorine:clear-console')
      assert.ok(await gotoTab('test2.cljs'))
      await sendCommand('chlorine:connect-embeded-clojurescript-repl')
      assert.ok(await haveText("ClojureScript REPL connected"))
    })

    it('evaluates code', async () => {
      await evalCommand("(ns user.test2)")
      assert.ok(await haveText("nil"))

      await evalCommand("(+ 5 2)")
      assert.ok(await haveText(7))

      await evalCommand("(str (+ 90 120))")
      assert.ok(await haveText(`"210"`))

      await evalCommand("(/ 10 0)")
      assert.ok(await haveText("##Inf"))
    })

    it('shows function doc', async () => {
      await sendCommand('inline-results:clear-all')
      await app.client.keys("\n\nstr")
      await sendCommand("chlorine:doc-for-var")
      assert.ok(await haveText("With no args, returns the empty string"))
    })

    it('captures exceptions', async () => {
      await evalCommand(`(throw (ex-info "Error Number 2", {}))`)
      assert.ok(await haveSelector(`div.error`))
      assert.ok(await haveText(`Error Number 2`))

      await evalCommand(`(throw "Error Number 3")`)
      assert.ok(await haveSelector(`div.error`))
      assert.ok(await haveText(`Error Number 3`))

      await evalCommand(`(ex-info "Error Number 4", {})`)
      assert.ok(await haveSelector(`div.result`))
      assert.ok(await haveText(`#error {:message "Error Number 4"`))

      await evalCommand(`(somestrangefn 10)`)
      assert.ok(await haveSelector(`div.error`))
      assert.ok(await haveText(`Error: "Cannot read property`))
    })
  })
})
