<p align="center">
  <img src="https://github.com/damn/gdx/blob/main/logo.png" width="250" height="105"/>
</p>

# What is GDL?

A clojure framework for building 2D games.

# Hello World

You can run the hello world example in this repository with:

```
lein run -m gdl.hello-world
```

https://github.com/damn/gdl/blob/f34a451e559363a1876e9d386516108333bdc5ce/test/gdl/hello_world.clj#L1-L25

# On Mac

You need to set this environment variable:

```
export JVM_OPTS=-XstartOnFirstThread
```

# Installation

[![](https://jitpack.io/v/damn/gdl.svg)](https://jitpack.io/#damn/gdl)

Add the following to your project.clj file:

```clojure
:repositories [["jitpack" "https://jitpack.io"]]

:dependencies [[com.github.damn/gdl "main-SNAPSHOT"]]
```

# [API Documentation](https://damn.github.io/gdl/)

# Games made with GDL

* [Cyber Dungeon Quest](https://github.com/damn/Cyber-Dungeon-Quest)
