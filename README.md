# Chlorine
[![CircleCI](https://circleci.com/gh/mauricioszabo/atom-chlorine.svg?style=svg)](https://circleci.com/gh/mauricioszabo/atom-chlorine)

Cl + Atom = Chlorine

Socket-REPL integration with Clojure and ClojureScript with Atom.

This package requires `Ink` to work. Install it on Atom package manager

## Usage:
Fire up a clojure REPL with Socket REPL support. With `shadow-cljs`, when you `watch` some build ID it'll give you a port for nREPL and Socket REPL. With `lein`, invoke it in a folder where you have `project.clj` and you can use `JVM_OPTS` environment variable like:

```bash
JVM_OPTS='-Dclojure.server.myrepl={:port,5555,:accept,clojure.core.server/repl}' lein trampoline repl
```

You can use `lein trampoline repl` or `lein repl`: both work (but I found that using `trampoline` uses less memory. Notice that `trampoline` **will not work** with nREPL).

On Shadow-CLJS' newer versions, when you start a build with `shadow-cljs watch <some-id>`, it doesn't shows the Socket REPL port on the console, but it does create a file with the port number on `.shadow-cljs/socket-repl.port`. You can read that file to see the port number (Chlorine currently uses this file to mark the port as default).

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
  'ctrl-c':       'chlorine:break-evaluation'
  'space space':  'inline-results:clear-all'
  'space x':      'chlorine:run-tests-in-ns'
  'space t':      'chlorine:run-test-for-var'

'atom-text-editor.vim-mode-plus.insert-mode[data-grammar="source clojure"]':
  'shift-enter': 'chlorine:evaluate-block'
  'ctrl-enter': 'chlorine:evaluate-top-block'
```

**If you don't use vim bindings:**
```cson
'atom-text-editor[data-grammar="source clojure"]':
  'ctrl-, y':       'chlorine:connect-clojure-socket-repl'
  'ctrl-, e':       'chlorine:disconnect'
  'ctrl-, k':       'chlorine:clear-console'
  'ctrl-, f':       'chlorine:load-file'
  'ctrl-, b':       'chlorine:evaluate-block'
  'ctrl-, B':       'chlorine:evaluate-top-block'
  'ctrl-, i':       'chlorine:inspect-block'
  'ctrl-, I':       'chlorine:inspect-top-block'
  'ctrl-, s':       'chlorine:evaluate-selection'
  'ctrl-, c':       'chlorine:break-evaluation'
  'ctrl-, c':       'chlorine:source-for-var'
  'ctrl-, d':       'chlorine:doc-for-var'
  'ctrl-, x':       'chlorine:run-tests-in-ns'
  'ctrl-, t':       'chlorine:run-test-for-var'
```

## How to work with ClojureScript
For now, it only works with Shadow-CLJS or Lumo.

With Lumo, you fire up lumo with `lumo -n 3322` to start a socket REPL on port `3322` (or any other port), then connect Chlorine with "Connect ClojureScript Socket REPL".

With Shadow-CLJS, after watching (or after starting a server, or anything that starts a socket REPL) you connect with "Connect Clojure Socket REPL", then run the command "Connect Embedded ClojureScript REPL". Then you can run code on .cljs files too.

## How to contribute?
As Chlorine is in active development, it was starting to become tedious to publish a newer version of repl-tooling for every experiment, so for now, the library is registered as a submodule. To contribute, you clone this repository and run:

```
./scripts/setup
```

To register the submodule. More info on [Developing](docs/developing.md) document.
