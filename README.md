# Clojure Plus

Lots of utilities to work with Clojure Code. This package depends on Proto-REPL package, and a bunch of nREPL middlewares - refactor-nrep, proto-rep, and (in the future), cider-nrep.

Clojure-plus adds some extensions to Proto REPL - it adds lighttable-like watch expressions (with some limitations), beautiful stacktraces, goto-symbol in stacktraces, notifications on refresh (and syntax checking on it), and the possibility to control when, and how, to refresh.

It'll add coloring to the current S-Expression we're editing. We can style it editing the `atom-text-editor::shadow .clojure-sexp .region` CSS selector, or we can just disable it.


## Refresh and notifications

Controlled by some parameters - **refresh all cmd** and **refresh cmd** defines which file contains the commands to refresh the current REPL. By default, uses files inside this plugin, which will just delegate to `tools.namespace.repl/refresh` functions.

**after** and **before** refresh commands are bunch of commands that'll run before and after each refresh. By default, these commands just set `clojure.test/*load-tests*` to false (before) and true (after) each refresh, so it'll not run Clojure or Midje tests. You can tune these parameters to stop an start servers. Clojure-Plus refresh will, too, update the state of each "watch expression" on Atom, so it's preferable to run this code in place of Proto REPL's.

**Simple Refresh** will just require the current namespace, with `:reload`. This will not remove old vars, its syntax checking is not ideal, and it's particularly a bad idea overall. Use it only when you're working with strange code that doesn't supports at all `tools.namespace` refreshes.

![Refresh and Notifications](https://raw.githubusercontent.com/mauricioszabo/clojure-plus/master/docs/refresh_notifications.gif)

## Watch expressions

In Clojure Plus, we just treat watch expressions as a rewrite of code before sending it to REPL. This approach is fundamentally different from LightTable, when each watch is updated immediately when the code runs. In this package, we just rewrite the code before sending it to REPL, converting every mark to a `println` and `swap!` expression, then running it - and, after we run each watch, we aggregate the Clojure's Atom results and display then.

### Customizing Watch Expressions

Clojure Plus adds a command - `addWatcher` - that allows anyone to define new "watch" expressions. These may do anything you want - from displaying something on the console to anything you want (as far as it's rewritable). For instance, this would be able to define a global var in code for any seelection you want:

```coffeescript
cljPlus = atom.packages.getActivePackage('clojure-plus').mainModule
cljPlus.addWatcher('global', '(do (def global-var ..SEL..) global-var)')
```

This would rewrite the code, so if you have the code below, and you watch "x" value (in the second line), it'll rewrite it before sending it to REPL:

```clojure
(fn [x y]
  (+ x y))

; Adding a watcher with the "global" we defined above
; on "x" after the "+" will rewrite it as:
(fn [x y]
  (+ (do (def global-var x) global-var) y)

; Please, be aware that if you change the watch expression to the first
; "x", in the function definition, you'll get a syntax error:
(fn [(do (def global-var x) global-var) y]
  (+ x y))
```

Unfortunately, watch expressions doesn't work correctly for macros like `->>` (it'll try to rewrite the macro code and will probably do the wrong thing).

![Watch Expressions](https://raw.githubusercontent.com/mauricioszabo/clojure-plus/master/docs/watches.gif)

## Stacktraces

Let's face it, Clojure stacktraces are awful. This package tries to ease this pain by providing inline stacktraces when something goes wrong - it'll try to parse Java stacktrace objects and provide it a better experience:

![Stacktraces](https://raw.githubusercontent.com/mauricioszabo/clojure-plus/master/docs/stacktrace.gif)

Please notice that it'll try to decompress and open files that are outside your project in the same way as goto-var (from Proto REPL) does. In the future, I'll try to parse stderr messages so that they display this same behavior, and probably we'll be able to parse then too and display then on editor (block decorations, maybe?).

## Problems

Nothing is perfect, and this package is not an exception. There are still things that, somehow, doesn't work. A bunch of then is because the way Clojure works, and others are for other problems:

* Watch expressions doesn't work with inline or static functions. This is a limitation of Clojure - inline or static functions (like `conj`) become something like `conj-1211` when compiled, and Clojure doesn't get the change.
* We're implementing "clojure-plus:evaluate-top-block" by copy-pasting code from proto-repl. We need to find a better way.
* Sometimes, the code execution halts. I'm not sure why (we use REPL with promises to sync code) so if this happens, just refresh your namespace and it'll un-hang it.

## Future
Ideas for the project's future

* There is code to add imports for missing symbols, but it's not really working right now (and, the first time it runs, it is SLOW!)
* Similar, there's a code to remove unused imports, but it's not working in most of the cases (for instance, if there are midje facts in the file, they'll be ignored and we'll remove imports that are being used only inside the `fact`).
  * Maybe we could just decorate the lines where there are imports we *think* are not being used, and the user decides to remove it or not.
* Find where a specific symbol is being used. I tried to use `refactor-nrepl` for it, but it needs a lot of parameters and most of the time just gives me a Null Pointer Exception. Furthermore, it suffers from the same problem as the midje one, above.
* Rename all symbols in a project - same as above.
* Debugger - try to wrap **CIDER**'s one.
* Parsing stacktraces when they're being shown on stdout and stderr. This would solve lots of problems with tests (when exceptions are captured by midje).
* Better code overall - the approach of this plugin is to try to bind Atom and Clojure code. Some problems happened because of this - there are times when the Clojure code we use to goto var and other things just stop working (Null pointer, again). We solve this with refresh, again, but it could be better (probably).
* Support for linters and other tools.
* Support for ClojureScript (making two connections to the same REPL and using piggieback to swap, maybe).
