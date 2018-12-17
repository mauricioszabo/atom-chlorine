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

Please notice that Chlorine only activates if you run any of the "connect" commands. You don't **need** to connect into a Clojure REPL, just fire the command and then you'll probably
