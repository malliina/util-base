[![Build Status](https://github.com/malliina/util-base/workflows/Test/badge.svg)](https://github.com/malliina/util-base/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.malliina/primitives_3.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.malliina%22%20AND%20a%3A%22primitives_3%22)

# util-base

This repo contains the following reusable modules:

## primitives

Basic data types, compiled both to JVM and Scala.js.

```scala
libraryDependencies += "com.malliina" %% "primitives" % "3.5.0"
```

## okclient

A Scala Futures-enabled HTTP client. Wraps okhttp.

## okclient-io

An HTTP client built on https://typelevel.org/cats-effect/. Wraps okhttp.

## config

Wrapper for Typesafe config.

# Releasing

    sbt release
