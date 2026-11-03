// Visitor project build file.

organization := "com.phasmidsoftware"

version := "1.7.0"

val scalaVersionNumber  = "3.9.0" // the current LTS
val scalaTestVersion    = "3.2.20"
val logbackVersion      = "1.5.32"
val slf4jVersion        = "2.0.17"

ThisBuild / scalaVersion := scalaVersionNumber

// ============================================================================
// COMPILER OPTIONS
// ============================================================================

// The compiler options that all the Phasmid libraries share.
scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Wunused:all",
  "-Wvalue-discard",
  "-Wnonunit-statement"
)
// Tests may use a value as a statement (for example, in a matcher), so they skip -Wnonunit-statement.
Test / scalacOptions ~= (_.filterNot(_ == "-Wnonunit-statement"))

// ============================================================================
// MODULE DEFINITION
// ============================================================================

lazy val root = (project in file("."))
  .settings(
    name         := "visitor",
    scalaVersion := scalaVersionNumber,

    libraryDependencies ++= Seq(
      "org.slf4j"       %  "slf4j-api"         % slf4jVersion,
      "ch.qos.logback"  %  "logback-classic"   % logbackVersion  % Runtime,
      "org.scalatest"   %% "scalatest"         % scalaTestVersion % Test,
      "org.junit.jupiter" % "junit-jupiter-api"    % "5.10.2" % Test,
      "org.junit.jupiter" % "junit-jupiter-engine" % "5.10.2" % Test,
      "com.github.sbt"    % "junit-interface"      % "0.13.3" % Test
    )
  )
  // Scaladoc is published to https://rchillyard.github.io/Visitor/latest/api/ by `sbt ghpagesPushSite`
  .enablePlugins(GhpagesPlugin, SiteScaladocPlugin)
  .settings(git.remoteRepo := "git@github.com:rchillyard/Visitor.git")

// ============================================================================
// GLOBAL SETTINGS
// ============================================================================

Test / parallelExecution := false

Test / testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

// ============================================================================
// USAGE NOTES
// ============================================================================
// The compiler options are the ones that all the Phasmid libraries share.
//
// Key options:
//   -Wvalue-discard      : catches silently discarded non-Unit values
//   -Wnonunit-statement  : catches non-Unit expressions used as statements
//                          (filtered out for test code)
//   -Wunused:all         : catches unused imports, privates, locals, parameters, etc.
// ============================================================================
