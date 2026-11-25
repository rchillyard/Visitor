// Visitor project build file.

organization := "com.phasmidsoftware"

version := "1.9.0"

val scalaVersionNumber  = "3.9.0" // the current LTS
val scalaTestVersion    = "3.2.20"
val logbackVersion      = "1.5.32"
val slf4jVersion        = "2.0.17"
val jupiterVersion      = "6.0.3"
val jupiterInterfaceVersion = "0.19.0"

ThisBuild / scalaVersion := scalaVersionNumber

// ============================================================================
// BINARY COMPATIBILITY
// ============================================================================

// MiMa checks the published artifact for binary compatibility with the last release: `sbt mimaReportBinaryIssues`.
// The version scheme is PVP (see publish.sbt): a patch release must pass; a minor release may break compatibility,
// on purpose and with a release note, by listing the breaks in the filters file (or, after the release, by moving
// previousRelease on). Note moduleName, not name — it is the published artifactId, not the display name.
val previousRelease = "1.8.0"
mimaPreviousArtifacts := Set(organization.value %% moduleName.value % previousRelease)

// Deliberate breaks are listed in src/main/mima-filters/<previousRelease>.backwards.excludes,
// which MiMa reads automatically. Keeping them there rather than here means they retire by
// themselves: when previousRelease moves on, the old file stops being consulted.

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
  "-Wnonunit-statement",
  "-Werror"
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
      // Test, not Runtime: a library must not publish a logging backend onto its consumers' classpath.
      "ch.qos.logback"  %  "logback-classic"   % logbackVersion  % Test,
      "org.scalatest"   %% "scalatest"         % scalaTestVersion % Test,
      // JUnit 5 — for the Java façade tests in src/test/java.
      // jupiter-interface is the sbt test interface: without it sbt compiles these tests and
      // never runs them. (The old junit-interface 0.13.3 was the JUnit *4* interface, so the
      // jupiter dependencies it sat beside could never have been executed.)
      "org.junit.jupiter"    % "junit-jupiter-api"    % jupiterVersion % Test,
      "org.junit.jupiter"    % "junit-jupiter-engine" % jupiterVersion % Test,
      "com.github.sbt.junit" % "jupiter-interface"    % jupiterInterfaceVersion % Test
    )
  )
  // Scaladoc is published to https://rchillyard.github.io/Visitor/latest/api/ by `sbt ghpagesPushSite`
  .enablePlugins(GhpagesPlugin, SiteScaladocPlugin)
  .settings(git.remoteRepo := "git@github.com:rchillyard/Visitor.git")

// ============================================================================
// MUTATION TESTING (Stryker4s)
// ============================================================================

// Stryker4s parses the sources with scalameta before instrumenting them. The default
// dialect is Scala 2.13, which cannot parse `given`/`using`, `enum`, or indentation
// syntax, so Scala 3 must be stated explicitly or every file fails to parse.
// Scope and thresholds live in stryker4s.conf. Run with `sbt stryker`.
strykerScalaDialect := scala.meta.dialects.Scala3

// ============================================================================
// GLOBAL SETTINGS
// ============================================================================

Test / parallelExecution := false

// ============================================================================
// JAVA API DOCUMENTATION
// `sbt doc` documents Scala sources only, so the Java façade in src/main/java would
// otherwise publish no API docs at all — and its -javadoc.jar on Central would have the
// same hole. This task puts Javadoc beside the Scaladoc on GitHub Pages.
// ============================================================================

lazy val javaApiDoc = taskKey[File]("Generates Javadoc for the Java façade in src/main/java.")

javaApiDoc := {
  val log = streams.value.log
  val _ = (Compile / compile).value // the façade's Javadoc needs the compiled Scala classes on the classpath
  val out = target.value / "javadoc"
  IO.delete(out)
  IO.createDirectory(out)
  val javaSources = ((Compile / javaSource).value ** "*.java").get
  if (javaSources.isEmpty) sys.error("No Java sources found in " + (Compile / javaSource).value)
  val classpath =
    ((Compile / dependencyClasspath).value.files :+ (Compile / classDirectory).value)
            .mkString(java.io.File.pathSeparator)
  val args = Seq(
    "-d", out.getAbsolutePath,
    "-classpath", classpath,
    "-quiet",
    // Report malformed HTML and broken @link targets, but not undocumented members: the façade
    // documents its public API, while its package-private plumbing is deliberately bare.
    "-Xdoclint:all,-missing",
    "-windowtitle", s"${name.value} ${version.value} Java API",
    "-doctitle", s"${name.value} ${version.value} — Java API"
  ) ++ javaSources.map(_.getAbsolutePath)
  val exit = scala.sys.process.Process("javadoc", args).!(log)
  if (exit != 0) sys.error(s"javadoc failed with exit code $exit")
  out
}

lazy val javaApiDocSubdir = settingKey[String]("Where the Java façade's Javadoc goes within the site.")

javaApiDocSubdir := "latest/javadoc"

addMappingsToSiteDir(Def.task(Path.allSubpaths(javaApiDoc.value).toSeq), javaApiDocSubdir)

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
