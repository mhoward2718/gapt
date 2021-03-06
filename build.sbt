lazy val commonSettings = Seq(
  organization := "at.logic.gapt",
  organizationHomepage := Some(url("https://code.google.com/p/gapt/")),
  licenses += ("GNU GPL v3", url("http://www.gnu.org/licenses/gpl.html")),
  startYear := Some(2008),
  version := "1.9",

  scalaVersion := "2.11.4",
  testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "junitxml", "console"),
  libraryDependencies ++= testDependencies map(_ % Test)
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  disablePlugins(JUnitXmlReportPlugin).
  settings(
    name := "gapt",
    description := "General Architecture for Proofs",

    mainClass := Some("at.logic.cli.CLIMain"),

    // Release stuff
    test in assembly := {}, // don't execute test when assembling jar
    releaseZip <<= (sbtassembly.AssemblyKeys.assembly, Keys.baseDirectory, Keys.target, Keys.version) map {
        (assemblyJar: File, baseDirectory: File, target: File, version: String) =>
      val zipFile = target / s"gapt-$version.zip"

      val filesToIncludeAsIs = List("README", "COPYING", "cli.sh", "gui.sh", "atp.sh", "include.sh", "examples")
      val entries = List((assemblyJar, s"gapt-$version.jar")) ++
        filesToIncludeAsIs.flatMap{fn => recursiveListFiles(baseDirectory / fn)}
          .map{f => (f, baseDirectory.toPath.relativize(f.toPath))}

      IO.zip(entries.map{ case (file, pathInZip) => (file, s"gapt-$version/$pathInZip") }, zipFile)
      zipFile
    },

    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.3",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
      "org.apache.commons" % "commons-lang3" % "3.3.2",
      "org.slf4j" % "slf4j-api" % "1.7.9",
      "org.slf4j" % "slf4j-log4j12" % "1.7.9",
      "xml-resolver" % "xml-resolver" % "1.2"),

    // UI
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "jline" % "jline" % "2.12",
      "org.scala-lang.modules" %% "scala-swing" % "1.0.1",
      "com.itextpdf" % "itextpdf" % "5.5.4",
      "org.scilab.forge" % "jlatexmath" % "1.0.2"),

    // Start each test class in a separate JVM, otherwise resolutionSchemaParserTest and nTapeTest fail.
    testGrouping in Test <<= definedTests in Test map oneJvmPerTest
  )

lazy val testing = (project in file("testing")).
  dependsOn(root).
  settings(commonSettings: _*).
  disablePlugins(JUnitXmlReportPlugin).
  settings(
    name := "gapt-testing",
    description := "gapt extended regression tests",
    sourcesInBase := false
  )

lazy val releaseZip = TaskKey[File]("release-zip", "Creates the release zip file.")

lazy val testDependencies = Seq(
  "junit" % "junit" % "4.12",
  "org.specs2" %% "specs2-core" % "2.4.12",
  "org.specs2" %% "specs2-matcher" % "2.4.12",
  "org.specs2" %% "specs2-mock" % "2.4.12",
  "org.specs2" %% "specs2-junit" % "2.4.12",
  "org.scalacheck" %% "scalacheck" % "1.12.1")

def oneJvmPerTest(tests: Seq[TestDefinition]) =
  tests map { test =>
    new Tests.Group(
      name = test.name,
      tests = Seq(test),
      runPolicy = Tests.SubProcess(ForkOptions()))
  }

def recursiveListFiles(f: File): List[File] =
  if (f.isDirectory)
    IO.listFiles(f).toList.flatMap(recursiveListFiles)
  else
    List(f)

