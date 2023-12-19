<p align="center">
  <img src="https://github.com/damn/gdx/blob/main/logo.png" width="250" height="105"/>
</p>

#  Details

Based on [libGDX](https://libgdx.com/).

Supporting desktop backend and 2D graphics API only at the moment.

Feedback appreciated.

This library is the backend for a roguelike action RPG game I am developing.

# Installation

[![](https://jitpack.io/v/damn/gdl.svg)](https://jitpack.io/#damn/gdl)

Add the following to your project.clj file:

``` clojure
:repositories [["jitpack" "https://jitpack.io"]]

:dependencies [[com.github.damn/gdl "main-SNAPSHOT"]]
```

# Documentation

* [API docs](https://damn.github.io/gdl/)

# Namespace dependency graph

<p align="center">
  <img src="https://github.com/damn/gdx/blob/main/namespaces.png"/>
</p>

# On Mac

You need to set this environment variable for the lwjgl3 backend to work on mac:

```
export JVM_OPTS=-XstartOnFirstThread
```

# Test

Start the test with `lein dev`.

# Games made with GDL

* [Cyber Dungeon Quest](https://github.com/damn/Cyber-Dungeon-Quest)

# Reloaded Workflow

The command `lein dev` starts a __dev-loop__.
When closing the app window all namespaces will be reloaded with `clojure.tools.namespace.repl/refresh-all`.

There is also a function `gdl.dev/restart!` in case of errors on app-start or refresh, so there is no need to restart the JVM.

You can bind this on a key , here in VIM :
``` vimscript
nmap <F5> :Eval (do (in-ns 'gdl.dev)(restart!))
```
