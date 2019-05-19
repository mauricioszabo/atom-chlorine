# Developing

Chlorine builds on [REPL-Tooling](https://github.com/mauricioszabo/repl-tooling/); the idea is that most of the code can be ported between multiple editors, and every specific plug-in (like Chlorine, for example) just needs to "watch" some specific elements. So, while developing new features, think carefully if that feature can be used in multiple editors - if so, put it on REPL-Tooling and then use it from here.

## Installing

Clone this repo and run the following commands:

```bash
cd atom-chlorine # or the name you gave when cloning
./scripts/setup
npx shadow-cljs watch dev
```

This will fire up a compiler for ClojureScript. If you symlink your directory on `~/.atom/packages`, you can use it while developing itself!

Please notice that Chlorine only activates if you run any of the "connect" commands. You don't **need** to connect into a Clojure REPL, just fire the command and then you'll see `JS runtime connected.` on shadow.

## Some design decisions
Most code should go to `repl-tooling` package. Because the strong dependency between both projects (and because I want to release often, release fast) currently they're being developed together, and that's the reason for repl-tooling to be registered as a submodule.

Currently, `./scripts/setup` will rewrite the submodule to use `http` clone URI on Github. If you want to develop Chlorine, you'll probably need to fork `repl-tooling` too, and update the submodule configuration accordingly.

To develop `repl-tooling`, please notice that there's a test namespace. If you run `npm start` on the console, you'll fire up a electron app, so you can literally see the test running. If you're going to develop a complicated feature, it'll probably be better to use the electron app, because it's a more controled environment (and you also can write tests for it). For example, all the renderer code is fully tested on `repl-tooling.integration.ui` namespace.

There's also a overview of ideas, new features, roadmap on REPL Tooling projects: https://github.com/mauricioszabo/repl-tooling/projects/1.

## UNREPL
This project parses the result of UNREPL and shows then on JS side. The way ClojureScript works today is to parse big numbers (`10N` and `10M`) into Javascript Numbers, and this will obviously not work when rendering data. So, we use a custom-made UNREPL blob: https://github.com/mauricioszabo/unrepl.

The additions were to change the way we parse big numbers (with reader tags) and also to add more functionality to Java classes (show methods, constructors, and enumerators).

As there's no UNREPL for ClojureScript, there's a "blob-like" to allow some customization to readers (for example, right now it is used to add stacktraces to exceptions and normalized Reagent's Atoms so it will not break the parsing on the editor's side). This "design decisions" is _not final_ and I'm still trying to find better ways to configure the printer (it doesn't work _that well_ and disallows customizations from the user side).

## Testing

Probably all simpler tests will be on REPL-Tooling. For Chlorine, there are two approaches:

### Simple inline tests
Because of some Atom limitations, is kinda difficult to write tests on ClojureScript (or any other language besides JS or CoffeeScript). On other packages I wrote, I tried multiple approaches and they all got me multiple false positives/negatives, so for ClojureScript you can write tests inline, together with implementation. They will show on Atom's dev console when you save the file, so it's a fast feedback.

Also, you probably don't want then running in production builds, so there's a development variable that toggles when to compile or not this code: `chlorine.aux/TESTS`. If you want to write some tests, you can use the following template:

```clojure
(ns chlorine.some-ns
  (:require [chlorine.aux :as aux]))

(when aux/TESTS
  (testing "this needs to fail"
    (is (= 1 2))))
```

### Real "user interations" tests
There are some integration tests right now on `integration` folder. These tests fire up a _real atom editor_ and then run commands inside the editor. This is as close you can get to your user interacting with Chlorine.

Also, there are some docker images so that these tests will be run on Travis CI (or any other CI that supports docker). You can test to see if your code can run inside a CI using `./scripts/ci` command (it'll run all the integration tests inside docker, the same as it runs on Travis today).

Please, notice that for the code to run on CI, it'll download the plug-in, update repl-tooling submodule, then it'll compile the code in `release` form: this means that dead code elimination and other optimizations will be applied. So, if you can't make the build pass on CI, probably you need to test on release.

## Releases
All releases are done automatically on the CI side. This means that if you can't make the integration builds pass, it'll be impossible to make a release. This is intentional: The first releases got some strange bugs, and multiple times the local code I was shipping was dirty (or the submodule was not updated) so I released some versions and other people were unable to replicate possible bugs or features in the latest versions.

The publishing is done on two tags: one is suffixed `-source`. For example, the version `0.1.1` have two tags: `v0.1.1` and `v0.1.1-source`. The `v0.1.1` is basically the other tag compiled on release mode (with `npx shadow-cljs release dev`) and with all source code removed. That way, the Atom package is smaller.
