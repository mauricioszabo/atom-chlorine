# Using Clojure with Atom Quickstart

This is a quick guide - if you are new to Clojure or Atom, start here. It will cover all steps to set up environment and start coding!

## Requirements

First, you will need the [Java Development Kit](http://openjdk.java.net/), [Leiningen](http://leiningen.org/) or [Clojure CLI tools](https://clojure.org/guides/getting_started) and [Atom](https://atom.io/) installed.

### Java

You may check if the JDK is installed with

```bash
java --version
```

If something like this is returned, go to Leiningen or CLI tools.

```bash
openjdk 14.0.1 2020-04-14
OpenJDK Runtime Environment (build 14.0.1+7-Ubuntu-1ubuntu1)
OpenJDK 64-Bit Server VM (build 14.0.1+7-Ubuntu-1ubuntu1, mixed mode, sharing)
```

Else, you may install OpenJDK these way:

> Debian-based Systems (Ubuntu, Mint):

```bash
sudo apt update
sudo apt install openjdk-11-jdk
```

> RHEL-based Systems (CentOS, Red Hat):

```bash
sudo yum update
sudo yum install java-11-openjdk-devel
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

### Clojure CLI tools
Please notice that you don't need to install both `lein` and `clj` - you can choose only one of then.

To install Clojure CLI tools, follow the steps on the site, or as a quick guide:

> For Linux:

```bash
curl -O https://download.clojure.org/install/linux-install-1.10.1.739.sh
chmod +x linux-install-1.10.1.739.sh
sudo ./linux-install-1.10.1.739.sh
```
> For Mac with Homebrew:

```bash
brew install clojure/tools/clojure
```

> On Windows with PowerShell 5 (or later), .NET Core SDK 2.1+ or .NET Framework 4.5+ (or later):

```powershell
Invoke-Expression (New-Object System.Net.WebClient).DownloadString('https://download.clojure.org/install/win-install-1.10.1.739.ps1')
```

And follow the instructions

### Atom

To install Atom, follow the [installation instructions](https://flight-manual.atom.io/getting-started/sections/installing-atom/).

## Atom packages

A list useful Atom packages to install. You may find them in Atom typing `ctrl+,` click on `Install` on the left and search for the package name.

### Highly Recommended

-   [Chlorine](https://github.com/mauricioszabo/atom-chlorine)
-   [Lisp Paredit](https://github.com/neil-lindquist/lisp-paredit)
-   [Parinfer](https://github.com/oakmac/atom-parinfer) or [Parinfer Plus](https://github.com/mauricioszabo/atom-parinfer-plus)

### Useful optional packages
-   [linter-kondo](https://github.com/gerred/linter-kondo)

## Configuring and Using Chlorine

### How it works:

Chlorine will connect with the REPL environment and let you evaluate code right from the Atom, without needing to change windows.

Parinfer (or Parinfer Plus) will indent code easily for you.

#### Connecting to a running REPL

Chlorine needs to connect to a running REPL to evaluate code. The shortest and easiest way to do this is the following:

1.  Change to a directory with a Clojure project or create one with and moves to its directory:

```bash
lein new app <APP_NAME>
cd <APP_NAME>
```

> You do not exactly _need_ to have a previous clj project or even a .clj file, but this step makes your life easier by handling the app's files

2.  Inside the app directory, start a REPL enviroment with an specific port to connect to it later

```bash
# Recomended, pure Socket REPL
JVM_OPTS='-Dclojure.server.myrepl={:port,5555,:accept,clojure.core.server/repl}' lein repl
# OR you can use nREPL
lein repl :start :port 5555
```

3.  In Atom, open the app folder or a single .clj file (or create one if you didn't do this before).

4.  Connect Chlorine to the running REPL, typing `ctrl+shift+p` and then, `Repl`, to open `Connect Socket Repl`. In the next window set host to `localhost` and port to `5555` (or any other port that you have specified).

5.  Be happy with your parenthesis ;)

#### Suggested Keymaps

You may configure your shortcuts in Atom for a better Chlorine experience.

1.  In Atom, type `ctrl+shift+p` and then, `keymap` to open `Application: Open your keymap`.
2.  Copy-paste the recomended keymaps on the [README](../README.md#keybindings) file or change the keymaps to your own liking
3.  Save it

If you follow the keymaps on the [README](../README.md#keybindings) (the ones that do not use vim-mode), all Chlorine related commands will be activated by pressing `ctrl+;` and then pressing the respective letter.

## Final words

[This playlist of Between Two Parens](https://www.youtube.com/watch?v=XJ4DUFjqDuQ&list=PLaGDS2KB3-AqeOryQptgApJ6M7mfoFXIp) may help you through this process.

I also recommend [Clojure for the Brave and True](https://www.braveclojure.com/) and this [Clojure Tutorial from Derek Banas](https://www.youtube.com/watch?v=ciGyHkDuPAE).

## If you speak Brazilian Portuguese

Cheque também [o livro do Gregório Melo](https://www.casadocodigo.com.br/products/livro-programacao-funcional-clojure?_pos=1&_sid=e2ee9f78c&_ss=r) sobre Clojure.
