# ZIO Clippy

A Scala compiler plugin for better ZIO type mismatch errors.

Go from this:

![](.github/img/before.png)

To this:

![](.github/img/after.png)

IntelliJ IDEA   |  VSCode / Metals
:--------------:|:---------------:
![](.github/img/main.png) | ![](.github/img/vscode.png)

## Zymposium video

Adam Fraser recently interviewed me, and we chatted about ZIO Clippy, how it came to be, and how it works internally. Watch the interview on YouTube!

[<img src="https://i.ytimg.com/vi/EmhgOS5V1pA/maxresdefault.jpg" width="50%">](https://www.youtube.com/watch?v=EmhgOS5V1pA "Zymposium with Igal Tabachnik - ZIO Clippy")

## Getting started

The recommended way to install this plugin is by installing it as a global sbt plugin:

1. Clone this repository to your local computer
2. Run `sbt +install`
3. In your project, reload sbt/bsp

Running `sbt +install` builds the plugin jar for all compatible Scala versions and places the [`ZIOPlugin.scala`](https://github.com/hmemcpy/zio-clippy/blob/master/project/ZIOPlugin.scala) file in the global `~/.sbt/1.0/plugins` directory, allowing any sbt project to load the plugin automatically. To remove, delete the ZIOPlugin.scala file from `~/.sbt/1.0/plugins`.

The plugin supports Scala 2.12, 2.13 with Scala 3 support coming soon! The plugin supports both ZIO 1 and ZIO 2.

## Additional configuration

Note: The recommended way to specify additional configuration is via a global sbt configuration file, without directly modifying your project's `build.sbt`.

Create a file `clippy.sbt` in your global sbt directory, `~/.sbt/1.0`. You can specify the options in this file, and they will be loaded automatically by your project.

### Original type mismatch error

To render the original type mismatch error in addition to the plugin output, add the following flag to your `scalacOptions`:

```scala
scalacOptions += "-P:clippy:show-original-error"
```
![](.github/img/full-error.png)

### Additional types for detection

ZIO Clippy support additional, *ZIO-like* types when parsing type mismatch errors. Any type that has 3 type parameters (e.g. `org.company.Saga[R, E, A]`) can be specified. To enable, provide a comma-separated *fully-qualified* list of names to the following option:

```scala
scalacOptions += "-P:clippy:additional-types:zio.flow.ZFlow,org.company.Saga"
```

## Technical information

This plugin implements a custom `Reporter` class, intercepting any `type mismatch` errors that contain ZIO-specific information (it's all regex!) with all other errors passing through to the underlying reporter.

The plugin tries to extract the *found* and *required* dependencies from the error message and performs a set diff to remove the found types from the required ones. This leaves just the type(s) that are missing/were not provided to the effect.

## Acknowledgments

The project borrows some ideas from Sam Halliday's excellent [Ensime TNG](https://ensime.github.io/), for both the local development setup and the _hack_ to replace the reporter.

In addition, the ANSI rendering is heavily inspired by Kit Langton's excellent work on [zio-magic](https://github.com/kitlangton/zio-magic), which is also part of ZIO 2's default error rendering.

## Bugs? Suggestions?

Let us know! Report an issue or send a PR!
