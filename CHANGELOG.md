## 0.2.4 - Shadow Element
* Removed pseudo ::shadow element on CSS (thanks @barbagrigia)
* Some debugging information when loading CLJS REPL (but still locking somewhere)

## 0.2.3 - Bugfix on Windows
* Removed dependency from HOME env variable (fixes
[#19](https://github.com/mauricioszabo/clojure-plus/issues/19))
* Refresh and Refresh-all are now commands instead of files

## 0.2.2 - More helpers
* Added "interrupt" command. It'll delegate to proto but will not lock anymore our execution context
* Refresh will now clear dependency cache. This means that refresh will recover from fatal error in more cases
* Added option "invert stacktrace" - better debugging or errors
* Corrected some errors with stacktrace parsing
* Corrected mark current SEXP

## 0.2.0 - ClojureScript support
* Added configuration to start a CLJS repl inside Atom
* Added code to differentiate between Clojure and ClojureScript
* Possibility to force Clojure or ClojureScript
* More info on statusbar
* New commands:
  * evaluate-block
  * evaluate-selection
* Watches in ClojureScript
* Renamed --check-deps-- ns (problems with CLJS)

## 0.1.2 - Bugfix
* Fixed cases when the first exception was being repeated when running code multiple times

## 0.1.2 - Fix on EVRY and Simple Refresh
* Simple refresh option on settings
* Fixed simple refresh causing an exception when there's no namespace in file
* Fixed "Everything plugin" integration
* Adding option to toggle simple refresh
* Simple correction when an exception returned nil

## 0.1.1 - Simple fix
* Exceptions will not show debug info in console anymore
* Exceptions are now block decorations

## 0.1.0 - First Release
* Watch expressions added
* Eval for top block (for watch expressions to work)
* Before / After refresh commands
* Stacktraces on errors
* Goto vars with configurable directory
