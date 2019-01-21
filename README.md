# Chlorine
[![Build Status](https://travis-ci.org/mauricioszabo/atom-chlorine.svg?branch=master)](https://travis-ci.org/mauricioszabo/atom-chlorine)

Cl + Atom = Chlorine

Socket-REPL integration with Clojure and ClojureScript with Atom.

This package requires `Ink` to work. Install it on Atom package manager

## Usage:
Fire up a clojure REPL with Socket REPL support. With `shadow-cljs`, when you `watch` some build ID it'll give you a port for nREPL and Socket REPL. With `lein`, invoke it in a folder where you have `project.clj` and you can use `JVM_OPTS` environment variable like:

```bash
JVM_OPTS='-Dclojure.server.myrepl={:port,5555,:accept,clojure.core.server/repl}' lein trampoline repl
```

You can use `lein trampoline repl` or `lein repl`: both work (but I found that using `trampoline` uses less memory. Notice that `trampoline` **will not work** with nREPL).

With `clj`, you can run the following from any folder:

```bash
clj -J'-Dclojure.server.repl={:port,5555,:accept,clojure.core.server/repl}'
```

Or have it in `:aliases` in `deps.edn`. (For an example with port 50505 see https://github.com/seancorfield/dot-clojure/blob/master/deps.edn, then you can run `clj -A:socket`.)


Then, you connect Chlorine with the port using the command _Connect Clojure Socket REPL_. This package works with lumo too, but you'll need to run _Connect ClojureScript Socket REPL_.

When connected, it'll try to load `compliment` and `org.clojure/tools.namespace` (for autocomplete and refresh). Then you can evaluate code on it, and it'll render on a block decoration below the line.

## Keybindings:
This package does not register any keybinding for you. You can define whatever you want. Some suggestions could be:

**If you use vim-mode-plus:**
```cson
'atom-text-editor.vim-mode-plus.normal-mode[data-grammar="source clojure"]':
  'g f':          'chlorine:go-to-var-definition'
  'ctrl-d':       'chlorine:doc-for-var'
  'space c':      'chlorine:connect-clojure-socket-repl'
  'space l':      'chlorine:clear-console'
  'shift-enter':  'chlorine:evaluate-block'
  'ctrl-enter':   'chlorine:evaluate-top-block'
  'space space':  'inline-results:clear-all'
  'space x':      'chlorine:run-tests-in-ns'
  'space t':      'chlorine:run-test-for-var'

'atom-text-editor.vim-mode-plus.insert-mode[data-grammar="source clojure"]':
  'shift-enter': 'chlorine:evaluate-block'
  'ctrl-enter': 'chlorine:evaluate-top-block'
```

**If you don't use vim bindings:**
```cson
'atom-text-editor.vim-mode-plus.normal-mode[data-grammar="source clojure"]':
  'ctrl-, y':       'chlorine:connect-clojure-socket-repl'
  'ctrl-, e':       'chlorine:disconnect'
  'ctrl-, k':       'chlorine:clear-console'
  'ctrl-, f':       'chlorine:load-file'
  'ctrl-, b':       'chlorine:evaluate-block'
  'ctrl-, B':       'chlorine:evaluate-top-block'
  'ctrl-, i':       'chlorine:inspect-block'
  'ctrl-, I':       'chlorine:inspect-top-block'
  'ctrl-, s':       'chlorine:evaluate-selection'
  'ctrl-, c':       'chlorine:source-for-var'
  'ctrl-, d':       'chlorine:doc-for-var'
  'ctrl-, x':       'chlorine:run-tests-in-ns'
  'ctrl-, t':       'chlorine:run-test-for-var'
```

## How to work with ClojureScript
For now, it only works with Shadow-CLJS or Lumo.

With Lumo, you fire up lumo with `lumo -n 3322` to start a socket REPL on port `3322` (or any other port), then connect Chlorine with "Connect ClojureScript Socket REPL".

With Shadow-CLJS, after watching (or after starting a server, or anything that starts a socket REPL) you connect with "Connect Clojure Socket REPL", then run the command "Connect Embedded ClojureScript REPL". Then you can run code on .cljs files too.

## WARNING!

This package is still in ALPHA! Expect breakages!

**So far, what's working:**
1. Connect to Socket REPL from Clojure
1. Connect a ClojureScript REPL (only shadow-cljs supported for now)
1. Disconnect (and handling when a REPL goes away)
1. Doc for vars (Clojure and ClojureScript)
1. Auto complete with Compliment
1. Simple auto complete for ClojureScript
1. Refresh for Clojure code with clojure.tools.namespace

**What is still crude:**
1. There are no configurations for what to run before and after refresh
1. Debug information is being dumped in console
1. There are still some rough edges when parsing Java classes because of the way
  unrepl works
1. Console does not show exceptions when an error occurs
1. There are still some problems when parsing exceptions (Clojure sometimes misses stacktrace)
1. There's no "goto definition", no "refactoring" yet
1. The option to connect to a self-hosted ClojureScript socket REPL does nothing for now
1. No support for Figwheel, Sidecar, etc (yet)
1. We only connect on shadow-cljs if there a `dev` named build
1. No way to toggle between Clojure and ClojureScript (for now, when you eval a CLJS file,
  it'll evaluate as ClojureScript, and every other file it'll evaluate as Clojure)
1. There are APIs for AutoComplete with ClojureScript (emacs uses then!), but they are
  not yet supported (in fact, Shadow-CLJS does support then but it's still not wired correctly)
1. There are no tests! Most of the tests are in REPL-Tooling library, but they are
  outdated

## TODO
* [x] Auto-complete on Clojure with Compliment
* [x] Auto-complete on ClojureScript with Simple-Complete (somewhat)
* [ ] Auto-complette on ClojureScript with cljs-tooling
* [x] Auto-complete on Lumo
* [x] Refresh with Clojure (require :reload)
* [x] Refresh with Clojure tools.nrepl
* [ ] Debugger
* [ ] Auto-import?
* [ ] Remove unused NS
