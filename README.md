[![Build Status](https://github.com/malliina/util-base/workflows/Test/badge.svg)](https://github.com/malliina/util-base/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.malliina/primitives_3.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.malliina%22%20AND%20a%3A%22primitives_3%22)

# util-base

This repo contains the following reusable modules:

## primitives

Basic data types, compiled both to JVM and Scala.js.

```scala
libraryDependencies += "com.malliina" %% "primitives" % "3.7.17"
```

## okclient

A Scala Futures-enabled HTTP client. Wraps okhttp.

```scala
libraryDependencies += "com.malliina" %% "okclient" % "3.7.17"
```

## okclient-io

An HTTP client built on https://typelevel.org/cats-effect/. Wraps okhttp.

```scala
libraryDependencies += "com.malliina" %% "okclient-io" % "3.7.17"
```

## config

Wrapper for Typesafe config.

```scala
libraryDependencies += "com.malliina" %% "config" % "3.7.17"
```

# Releasing

    sbt release
