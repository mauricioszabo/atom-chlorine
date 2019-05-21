## Planned/TODO
* Connect to Figwheel
* Connect to Shadow-CLJS but allows to select build ID
* Connect to ClojureScript using an arbitrary command
* Before/After refresh
* Higlight current form
* GOTO definition for ClojureScript (as far as we can)
* Auto-import
* Clean unused imports
* Problems when we stack multiple evaluations (see FIXME on repl-tooling)
* Fix problems with unreadable forms

## 0.1.4
- Autocomplete now honors config _Minimum Word Length_ from Atom's config

## 0.1.3
- Break on Clojure (https://github.com/mauricioszabo/repl-tooling/issues/6)
- Fixed "burst commands" (https://github.com/mauricioszabo/repl-tooling/issues/24)
- Fix Lumo's console error (https://github.com/mauricioszabo/atom-chlorine/issues/60)

## 0.1.2
- Auto-adding port number if the project file is a shadow-cljs build project
- Config to open console on bottom
- Fixed console errors (https://github.com/mauricioszabo/atom-chlorine/issues/33) and (https://github.com/mauricioszabo/atom-chlorine/issues/32)

## 0.1.1
- Fix `#js` tagged literal
- Shadow-CLJS can now evaluate multiple forms
- Fix stacktraces on ClojureScript
- Removed "shadow" on exception on CLJS (conflicts with light theme)
- Fixed link spacing on renderer

## 0.1.0
- New renderer for results
- Fixed "leaking internal implementation" on some exceptions
- New renderer for errors
- Fixed error when trying to expand tagged literals
- De-emphasis on Java's stacktrace lines
- Fixes stacktrace not appearing on big exceptions (https://github.com/mauricioszabo/atom-chlorine/issues/50)
- Fixes REBL integration (https://github.com/mauricioszabo/atom-chlorine/pull/51)
- Focus on fields when connecting to Socket REPL (https://github.com/mauricioszabo/atom-chlorine/pull/47)
- Install dependencies (https://github.com/mauricioszabo/atom-chlorine/pull/45)

## 0.0.10
* Update of UNREPL broke objects that implements `nav` (all objects, really). Temporary fix so things keep working until it's solved for once.

## 0.0.9
* Evaluate of `10M` and `10N` is rendered correctly
* Rendering Java Objects that implement `print-method`
* Better rendering of Java objects that have no `print-method` implemented
* Better rendering of Java classes / methods
* Using a custom UNREPL blob

## 0.0.8
* GOTO definition for Clojure (inside JARs)
* Add support for REBL
* Fixes https://github.com/mauricioszabo/repl-tooling/issues/14 (unreadable keywords and symbols)
* Fixes some edge-cases with clojure/tools.namespace refresh
* Allows to load file with windows on WSL

## 0.0.7
* Fixes autocomplete error: https://github.com/mauricioszabo/atom-chlorine/issues/26
* Fixes backpressure error: https://github.com/mauricioszabo/repl-tooling/issues/7
* When we close the REPL console, we now disconnect from Socket REPLs

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
