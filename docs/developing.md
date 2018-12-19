# Developing

Chlorine builds on [REPL-Tooling](https://github.com/mauricioszabo/repl-tooling/); the idea is that most of the code can be ported between multiple editors, and every specific plug-in (like Chlorine, for example) just needs to "watch" some specific elements. So, while developing new features, think carefully if that feature can be used in multiple editors - if so, put it on REPL-Tooling and then use it from here.

## Installing

Clone this repo and run the following commands:

```bash
cd atom-chlorine # or the name you gave when cloning
git submodule init
git submodule update
npm install
cp repl-tooling/resources .
npx shadow-cljs watch dev
```

This will fire up a compiler for ClojureScript. If you symlink your directory on `~/.atom/packages`, you can use it while developing itself!

Please notice that Chlorine only activates if you run any of the "connect" commands. You don't **need** to connect into a Clojure REPL, just fire the command and then you'll see `JS runtime connected.` on shadow.

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
