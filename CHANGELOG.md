## Planned/TODO
* Connect to Figwheel
* Connect to Shadow-CLJS but allows to select build ID
* Connect to ClojureScript using an arbitrary command
* Before/After refresh
* Higlight current form
* GOTO definition for Clojure (inside JARs)
* GOTO definition for ClojureScript (as far as we can)
* Auto-import
* Clean unused imports

## 0.0.6
* Fix https://github.com/mauricioszabo/atom-chlorine/issues/22

## 0.0.5
* Goto definition for Clojure files outside JAR
* Fix wrong file and line number when evaluating
* Add commands `chlorine:load-file` to load full file in REPL, `chlorine:source-for-var` to show source (by @seancorfield)
* Add commands to run tests (by @seancorfield)
* Fixed auto-complete on Clojure when NS form is undetermined (by @seancorfield)

## 0.0.4
* Fixes #9 (Error while evaluating block, top-level block, and we're not on a form)
* Configuration for refresh-mode, save on refresh
* Better refreshable architecture
* More info on statusbar (about refresh)
* Connect embedded ClojureScript now tries to find a build from `shadow-cljs.edn`, if present

## 0.0.3 - Clojure's Autocomplete FIX
* Fixed autocomplete on Clojure (incorrect predictions, more... string, require, and other issues)

## 0.0.2 - Better CLJS support (ALPHA!)
* Support for self-hosted REPLs like Lumo
* Support for exceptons in CLJS

## 0.0.1 - First Release (ALPHA!)
* Connection to Clojure REPL with UnREPL
* Connection to Shadow CLJS from UnREPL
* Doc for var
* Evaluate (selection, block, top block)
* Reload (full) from Clojure
