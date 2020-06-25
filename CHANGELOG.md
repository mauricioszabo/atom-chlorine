## Planned/TODO
* Connect to Figwheel
* Connect to ClojureScript using an arbitrary command
* Before/After refresh
* Higlight current form
* Auto-import
* Clean unused imports

## 0.8.1
- Possibility of running Shadow-CLJS Remote API commands (see: https://github.com/mauricioszabo/repl-tooling/pull/83)
- Small fix on inline results, with Experimental Features, when saving a file with errors on Shadow-CLJS

## 0.8.0
- Fix on regexp printing
- Using another way to connect to Shadow-CLJS
#### Experimental features on this version
- Implemented the new Websocket REPL of Shadow-CLJS (Remote API)
- For now, removed inspection of JS and resolving promises

## 0.7.5
- Fixes on nREPL imports for Orchard, Compliment, etc (https://github.com/mauricioszabo/atom-chlorine/issues/191)

## 0.7.4
- Fixed refresh code not working

## 0.7.3
- Forward-finding namespaces if a NS form was not found before the cursor (fixes https://github.com/mauricioszabo/atom-chlorine/issues/193)
- Waiting for REPL results before forwarding new commands to the REPL (probably fixes https://github.com/mauricioszabo/atom-chlorine/issues/192)
- Autocomplete shows docstrings and arities when completing functions

## 0.7.2
- Fixed that tried to run refresh on EDN files
- Fix on `get-selection` for configs in ClojureScript
- Remove Ink
- Performance fixes on inline results, removed LOTS of memory leaks
- `editor/eval-and-render` now resolves as promises

## 0.7.0
- Performance improvement while parsing Clojure code
- Runnable config
- Fixed error trying to connect to ClojureScript socket REPLs

### 0.6.2
- Fixed core Clojerl exception
- Fixed saving file when Clojure is disconnected and auto-refresh is turned on (Fixes https://github.com/mauricioszabo/atom-chlorine/issues/179)
- More commands comming from Orchard:
  - find-usages
  - clojure-doc-for-var

### 0.6.1
- Fixed connection for other Clojure implementations like Clojerl, Joker, etc.

### 0.6.0
- Interactive results (see [documentation](docs/extending.md))
- First support for `info` command, if "Orchard" is on classpath
- Code refactoring, fixed issues

### 0.5.4
- Loading tests on full refresh
- Fixed parsing of namespaces with metadata, and quotes (the kind you get when using clj-kondo)

### 0.5.3
- Fixed paths on dar*win* systems

### 0.5.2
- Faster nREPL messages parsing
- Load-file now prints the stacktrace when it fails to load
- Fixed paths on Windows, so goto var definition and clicking on stacktraces will work

### 0.5.1
- Simple fix for nREPL on slower sockets
- (Possible) load-file fix

### 0.5.0
- Alpha support for nREPL

### 0.4.16
- Fixed connection with Babashka

### 0.4.15
- Fixed issue with GOTO Var Definition when the current file's full path is too long.

### 0.4.14
- First try to fix opening an empty editor on MacOSX

### 0.4.13
- When refresh fails, show the stacktrace on the console
- Clickable stacktraces for Clojure and Shadow-CLJS
- Stacktraces on ClojureScript will use source-maps to parse their errors
- Experimental features fix: don't generate two inline results for the same line

### 0.4.12
- Redirecting `*test-out*` to the right output

### 0.4.11
- Fixes "Attempting to call unbound fn: #'unrepl.core/write" (https://github.com/mauricioszabo/atom-chlorine/issues/158)

### 0.4.10
- Fixes https://github.com/mauricioszabo/atom-chlorine/issues/156

### 0.4.9
- Fix for CSS of Ink inline results, and some edge-cases that could throw exceptions

### 0.4.7
- Autocomplete for Babashka

#### Experimental features
- Dropping Ink dependency

### 0.4.6
- Changed the detection of blocks (will consider the parens the cursor is in as the "block")
- Fixed https://github.com/mauricioszabo/atom-chlorine/issues/150

### 0.4.5
- Some fixes on the experimental features' renderer
- Fixed a bug that happens when something is evaluating, then you disconnect the REPL
#### Experimental features
- Added support for resolving promises

### 0.4.4
- Some fixes on evaluate-block
#### Experimental features on this version
- Changed the way to evaluate Shadow-CLJS commands
- Added some inspection to Javascript objects
- Fixed some issues on deeply nested invalid EDN objects on ClojureScript

### 0.4.3
- Supporting Suitable for CLJS autocomplete
- When evaluating top-level, considers the parens before the cursor if the selection is on the end of the line
- Fixes for strange issues happening on evaluation of Clojure forms (sometimes, things were evaluating out-of-order or returning nil incorrectly)
- Migrated autocomplete tools to use promises, to avoid crashing the Atom editor in case of failures while autocompleting
- Fixed a bug on Autocomplete (when disconnecting the REPL and adding/removing Compliment, the autocomplete would still try to use/not use Compliment after connecting to REPL)

### 0.4.2
- Fixed https://github.com/mauricioszabo/atom-chlorine/issues/139

### 0.4.1
- Fixed test results and output not appearing on console

### 0.4.0
- Fixed test output not rendering on console
- Promoted all "experimental features" as they passed the battle test
- All "evaluate" commands are implemented without using Atom's APIs. They also are aware of reader symbols, so now `evaluate-block` over `'(+ 1 2 3)` will return a list, not run the function.
- Added "interactive" evaluation
- Goto var definition now works on ClojureScript

### 0.3.9
- Fixed autocomplete error on dynamic vars (https://github.com/mauricioszabo/atom-chlorine/issues/132)
- Support for Clojerl
- Treating special windows chars on REPL output

### 0.3.8
- Fixed error when trying to execute a ClojureScript code when REPL is not clj-connected
- Fixed warnings when connecting to ClojureScript without reagent

### 0.3.7
- Fixed "get current var" when cursor is at the end of the variable
- Fixed warning messages connection to Shadow-CLJS when reagent is not present (https://github.com/mauricioszabo/atom-chlorine/issues/127)

### 0.3.6
- Added specs on doc-for-var (https://github.com/mauricioszabo/atom-chlorine/issues/100)
- Fixed an issue with goto var definition where sometimes it wasn't able to find the var

### 0.3.5
- Fixed issues with "Copy to Clipboard"
- Removed old connection methods like "Connect Clojure Socket REPL" and "Connect ClojureScript REPL"
- Removed deprecated extension points

### 0.3.4
- Fixed connection on Arcadia
- Some REPLs don't send a "disconnect" event when closing the socket, so we "simulate" a disconnect
- Fixed "Disconnected from REPLs" appearing twice
- Copy results to clipboard

### 0.3.3
- Fixed evaluation on CLR where it fails with "Unable to resolve symbol: str in this context"
- Load-file working on multiple REPLs
- Fixed UUIDs

### 0.3.2
- Fixed an edge-case with "Evaluate Selection" (sometimes when selecting the last line and sending to evaluate, it could crash).
- Fixed "load-file" on "Connect Clojure Socket REPL"
- Disabled "Connect ClojureScript Socket REPL"

### 0.3.1
- Fixed connection on Clojure over the "Connect Socket REPL" command

### 0.3.0
- Fixed sending incomplete forms freezing Clojure REPL
- When trying to connect to a wrong host/port, displays an error
- Disable commands that other REPLs don't support
- Fixed edge case of internal implementation "leaking" over the Chlorine console and appearing on the output of evaluations

#### Experimental support for new REPLs
- Support for Babashka >= 0.0.24
- Support for ClojureCLR
- Support for Joker
- Support for more ClojureScript REPLs that open a socket REPL

### 0.2.2
- Fixes problems when evaluating code that can't be compiled (https://github.com/mauricioszabo/atom-chlorine/issues/109)
- Fixes autocomplete crashing the editor

### 0.2.1
- Removed printing of `name.space=> ` on console for CLJS
- Fixes styling issues on console tab
- Fixed double-exception rendering on console tab

### 0.2.0
- Connection to ClojureScript with Shadow-CLJS allows us to select the build ID
- New autocomplete for Clojure without Compliment
- New autocomplete for ClojureScript **with** Compliment
- Detection of which autocomplete to use based detecting namespaces on the classpath
- Fix console CSS when using IDEs packages
- Better extension points for Atom

## 0.1.14
- Re-adding a rich console for evaluation and STDOUT/STDERR
- Exceptions are now rendered on the console

## 0.1.12
- Fixed InkConsole not opening (https://github.com/mauricioszabo/atom-chlorine/issues/93)
- Better evaluate-block and evaluate-top-block without using Atom API (still behind the experimental features)

### Experimental features on this version
- Commands "evaluate-selection", "evaluate-top-block" and "evaluate-block" without using Atom's APIs. They also are aware of reader symbols, so now `evaluate-block` over `'(+ 1 2 3)` will return a list, not run the function.

## 0.1.11
- Fixed some error with autocomplete in CLJS (https://github.com/mauricioszabo/atom-chlorine/issues/85)
- Added experimental features with a config to control
- Disabled editing on Console tab (https://github.com/mauricioszabo/atom-chlorine/issues/79)
- Changed defaults for auto-reload projects
- Rendering correctly the ratio (like `1/2`)
- Fixed load-file on non-saved or non-editor tabs (https://github.com/mauricioszabo/atom-chlorine/issues/75)

### Experimental features on this version
- To be able to support multiple NS on the same files and also to remove internal Atom's
API on detecting the current start/end forms, there's a new way to detect start/end
of forms. This affects the commands "evaluate-selection" and "evaluate-top-block" (but
not "evaluate-block" because of other issues). These two new ways of evaluating are
**not stable yet** and they'll fail with `::server/port` kind of symbols, for example.

## 0.1.10
- Fixed autocomplete on Clojure

## 0.1.9
- Fix goto var definition failing when the var was defined evaluating blocks, or with load-file

## 0.1.8
- Fix GOTO definition
- Takes up less space on status bar

## 0.1.7
- Fixes Chlorine not activating on MacOSX (https://github.com/mauricioszabo/atom-chlorine/issues/67)

## 0.1.5
- Fixes compilation problems

## 0.1.4 (unpublished)
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
