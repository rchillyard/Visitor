
organization := "com.phasmidsoftware"

version := "0.0.2-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0"

lazy val root = (project in file("."))
  .settings(
    name := "visitor"
  )

val scalaTestVersion = "3.2.19"

libraryDependencies += "org.slf4j" % "slf4j-api" % "2.0.17"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.18" % "runtime"
libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVersion % "test"

