# Chlorine
[![Build Status](https://travis-ci.org/mauricioszabo/atom-chlorine.svg?branch=master)](https://travis-ci.org/mauricioszabo/atom-chlorine)

Cl + Atom = Chlorine

Socket-REPL integration with Clojure and ClojureScript with Atom.

This package requires `Ink` to work. Install it on Atom package manager

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

## How to work with ClojureScript

## Usage

As with proto-repl, Cider, and others, you need to fire up a REPL in the console, then
connect it with Atom. If you're using Clojure, it'll use the wonderful UnREPL project
to send and receive commands.

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
