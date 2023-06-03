# zio-clippy

A Scala compiler plugin for better ZIO type mismatch errors.

Go from this:

![](.github/img/before.png)

To this:

![](.github/img/after.png)

## Getting started

![Maven Central](https://img.shields.io/maven-central/v/com.hmemcpy/zio-clippy_2.12.svg?style=flat-square)

Add the following to your `build.sbt` to install the plugin in your project:

```scala
addCompilerPlugin("com.hmemcpy" %% "zio-clippy" % "0.0.1")
```

(replace with the latest available version from Maven Central)

Reload your project, and any ZIO type mismatch errors will now be rendered in a nice output. This works with all build servers (sbt, bsp) and all editors (IntelliJ IDEA, VSCode/Metals).

The plugin supports Scala 2.12, 2.13 with Scala 3 support coming soon!

## Technical information

This plugin implements a custom `Reporter` class, intercepting any `type mismatch` errors that contain ZIO-specific information (it's all regex!) with all other errors passing through to the underlying reporter.

The plugin tries to extract the *found* and *required* dependencies from the error message and performs a set operation (`--`, an alias for `removeAll``) to remove the found types from the required ones. This leaves just the type(s) that are missing/were not provided to the effect.

## Additional configuration

To render the original type mismatch error in addition to the plugin output, add the following flag to your `scalacOptions`:

```scala
"-P:clippy:show-original-error"
```

![](.github/img/full-error.png)

## Bugs? Suggestions?

Let us know! Report an issue or send a PR!
