resolvers += "Sonatype OSS Snapshots" at
  "https://oss.sonatype.org/content/repositories/releases"

lazy val commonSettings = Seq(
  organization              := "org.meerkat",
  version                   := "0.1.0",
  scalaVersion              := "2.12.3",
  parallelExecution in Test := false,
  logBuffered in Test       := false,

  assemblyMergeStrategy in assembly := {
   case PathList("META-INF", xs @ _*) => MergeStrategy.discard
   case x                             => MergeStrategy.first
  }
)

lazy val core = (project in file("core"))
  .settings(commonSettings)
  .settings(
    name := "MeerkatCore",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "src" / "macros" / "scala",
    libraryDependencies ++= Seq(
      "org.scalactic"                  %% "scalactic"    % "3.0.1",
      "org.scalatest"                  %% "scalatest"    % "3.0.1" % Test,
      "org.bitbucket.inkytonik.dsinfo" %% "dsinfo"       % "0.4.0",
      "org.scala-graph"                %% "graph-core"   % "1.12.0",
      "org.apache.jena"                % "jena-core"     % "3.4.0",
      "com.storm-enroute"              %% "scalameter"   % "0.8.2"
    ),
    testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
    parallelExecution in Test := false
  )

/*lazy val neo4j = (project in file("neo4j"))
  .settings(commonSettings)
  .dependsOn(core % "compile->compile;test->test")
  .settings(
    name := "MeerkatNeo4j",
    libraryDependencies ++= Seq(
      "org.scalactic" %% "scalactic"   % "3.0.1",
      "org.scalatest" %% "scalatest"   % "3.0.1" % "test",
      "org.neo4j"     % "neo4j"        % "3.2.6",
      "org.neo4j"     % "neo4j-kernel" % "3.2.6",
      "org.neo4j"     % "neo4j-io"     % "3.2.6",
      "org.neo4j"     % "neo4j-kernel" % "3.2.6" % "test" classifier "tests",
      "org.neo4j"     % "neo4j-io"     % "3.2.6" % "test" classifier "tests"
    )
  )*/

lazy val assemblySettings = Seq(
  assemblyJarName in assembly := "lockchecker.jar"
)

lazy val lockchecker = (project in file("lockchecker"))
  .settings(commonSettings)
  .settings(assemblySettings)
  .dependsOn(core % "compile->compile;test->test")
  .settings(
    name := "MeerkatLockChecker",
    libraryDependencies ++= Seq(
      "org.scalactic" %% "scalactic"   % "3.0.1",
      "org.scalatest" %% "scalatest"   % "3.0.1" % "test",
    )
  )

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "MeerkatRoot"
  )
  .aggregate(core, lockchecker)
  .dependsOn(core, lockchecker)

mainClass in assembly := Some("org.meerkat.lockchecker.Main")