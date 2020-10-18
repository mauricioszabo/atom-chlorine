# Using Clojure with Atom Quickstart

This is a quick guide. If you are new to Clojure or Atom, start here. It will cover all steps to set up environment and start coding!

## Requirements

First, you will need the [Java Development Kit](http://openjdk.java.net/), [Leiningen](http://leiningen.org/) and [Atom](https://atom.io/) installed.

### Java

You may check if the JDK is installed with

```bash
java --version
```

If something like this is returned, go to Leiningen.

```bash
openjdk 14.0.1 2020-04-14
OpenJDK Runtime Environment (build 14.0.1+7-Ubuntu-1ubuntu1)
OpenJDK 64-Bit Server VM (build 14.0.1+7-Ubuntu-1ubuntu1, mixed mode, sharing)
```

Else, you may install OpenJDK these way:

> Debian-based Systems (Ubuntu, Mint):

```bash
sudo apt update
sudo apt install openjdk-14-jre-headless
```

> RHEL-based Systems (CentOS, Red Hat):

```bash
sudo yum update
sudo yum install java-14-openjdk-devel
```

> Mac Systems:

Homebrew is the de-facto standard package manager for macOS. If you haven't installed it yet, Matthew Broberg's [Introduction to Homebrew](https://opensource.com/article/20/6/homebrew-mac) walks you through the steps.

```bash
brew cask install java
```

### Leiningen

Easy way to install:

> Debian-based Systems (Ubuntu, Mint):

```bash
cd /usr/local/bin
sudo wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
sudo chmod +x lein
./lein
```

> RHEL-based Systems (CentOS, Red Hat):

```bash
wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
sudo su -c "chown root:root ./lein && chmod a+x ./lein"
sudo su -c "mv ./lein /usr/local/bin/"
```

> Mac Systems

```bash
curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > lein
sudo mv lein /usr/local/bin/lein
sudo chmod a+x /usr/local/bin/lein
```

You may check the installation with

```bash
lein -v
```

Something like this should be returned:

```bash
OpenJDK 64-Bit Server VM warning: Options -Xverify:none and -noverify were deprecated in JDK 13 and will likely be removed in a future release.
Leiningen 2.9.4 on Java 14.0.1 OpenJDK 64-Bit Server VM
```

Now, you can use the [REPL](https://clojure.org/guides/repl/introduction) by typing

```bash
lein repl
```

### Atom

To install Atom, follow the [installation instructions](https://flight-manual.atom.io/getting-started/sections/installing-atom/).

## Atom packages

A list useful Atom packages to install. You may find them in Atom typing `ctrl+,` click on `Install` on the left and search for the package name.

### Highly Recommended

-   [Chlorine](https://github.com/mauricioszabo/atom-chlorine)
-   [Parinfer](https://github.com/oakmac/atom-parinfer)
-   [Parinfer Plus](https://github.com/mauricioszabo/atom-parinfer-plus)

### Useful optional packages

-   [Keymap Control](https://github.com/hanslivingstone/keymap-control)
-   [linter-kondo](https://github.com/gerred/linter-kondo)

## Configuring and Using Chlorine

### How it works:

Chlorine will connect with the REPL environment and let you evaluate code right from the Atom, without need to change windows.

Parinfer Plus will autocomplete and indent code easily for you.

### How to use Chlorine

#### Connecting to a running REPL

Chlorine needs to connect to a running REPL to evaluate code. Following is the shortest and easiest way to do this:

1.  Change to a directory with a Clojure project or create one with and moves to its directory:

```bash
lein new app <APP_NAME>
cd <APP_NAME>
```

> You do not exactly _need_ to have a previous clj project or even a .clj file, but this step makes your life easier by handling the app's files

2.  Inside the app directory, start a REPL enviroment with an specific port to connect to it later

```bash
lein repl :start :port 5555
```

> Again, you do not _need_ to specify port, but believe me, you want to do it

3.  In Atom, open the app folder or a single .clj file (or create one if you didn't do this before).

4.  Connect Chlorine to the running REPL, typing `ctrl+shift+p` and then, `Repl`, to open `Connect Socket Repl`. In the next window set `Host: localhost` and `Port: 5555` or any other port that you have specified.

5.  Be happy with your parentheses ;)

#### Suggested Keymaps

You may configure your shortcuts in Atom for a better Chlorine experience.

1.  In Atom, type `ctrl+shift+p` and then, `keymap` to open `Application: Open your keymap`.

2.  Then, input your keymap shortcut setting. Mine is this way:

```text
'atom-text-editor[data-grammar="source clojure"]':
  'ctrl-; r':       'chlorine:connect-socket-repl'
  'ctrl-; u':       'chlorine:disconnect'
  'ctrl-; c':       'chlorine:clear-console'
  'ctrl-; l':       'chlorine:load-file'
  'ctrl-; e':       'chlorine:evaluate-block'
  'ctrl-; E':       'chlorine:evaluate-top-block'
  'ctrl-; s':       'chlorine:evaluate-selection'
  'ctrl-; b':       'chlorine:break-evaluation'
  'ctrl-; S':       'chlorine:source-for-var'
  'ctrl-; d':       'chlorine:doc-for-var'
  'ctrl-; n':       'chlorine:run-tests-in-ns'
  'ctrl-; v':       'chlorine:run-test-for-var'
  'ctrl-; i':       'chlorine:clear-inline-results'
```

This way, all Chlorine related commands start with `ctrl+;` and then, press the respective letter. Note that the keybindings from the example above use mnemonics to make it easier to memorize them. But this is just a suggestion and you can pick whatever you feel most comfortable with

3.  Save it

## Final words

This is just a really quickstart to give a little help to avoid get lost or stuck in basic things. You still need to explore [Atom](https://atom.io/), [Chlorine](https://github.com/mauricioszabo/atom-chlorine), [Clojure](https://clojure.org/api/cheatsheet) and [Leiningen](https://leiningen.org/).

[This playlist of Between Two Parens](https://www.youtube.com/watch?v=XJ4DUFjqDuQ&list=PLaGDS2KB3-AqeOryQptgApJ6M7mfoFXIp) may help you through this process.

I also recommend [Clojure for the Brave and True](https://www.braveclojure.com/) and this [Clojure Tutorial from Derek Banas](https://www.youtube.com/watch?v=ciGyHkDuPAE).

### Extra tip for Brazilian Portuguese speakers

Eu aprendi Clojure com [o livro do Gregório Melo](https://www.casadocodigo.com.br/products/livro-programacao-funcional-clojure?_pos=1&_sid=e2ee9f78c&_ss=r).

Participe dessa [comunidade super receptiva](https://t.me/clojurebrasil) de Clojure.
