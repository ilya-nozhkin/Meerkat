name := "Meerkat"
lazy val commonSettings = Seq(
  organization              := "org.meerkat",
  version                   := "0.1.0",
  scalaVersion              := "2.12.3",
  parallelExecution in Test := false,
  logBuffered in Test       := false,
  EclipseKeys.withSource    := true
)

lazy val core = (project in file("core"))
  .settings(commonSettings)
  .settings(
    unmanagedSourceDirectories in Compile += baseDirectory.value / "src" / "macros" / "scala",
    libraryDependencies ++= Seq(
      "org.scalactic"                  %% "scalactic"    % "3.0.1",
      "org.scalatest"                  %% "scalatest"    % "3.0.1" % Test,
      "com.google.guava"               % "guava-testlib" % "23.0",
      "commons-io"                     % "commons-io"    % "2.4",
      "org.bitbucket.inkytonik.dsinfo" %% "dsinfo"       % "0.4.0",
      "org.scala-graph"                %% "graph-core"   % "1.12.0",
      "org.apache.jena"                % "jena-core"     % "3.4.0"
    )
  )

lazy val neo4j = (project in file("neo4j"))
  .settings(commonSettings)
  .dependsOn(core % "compile->compile;test->test")
  .settings(
    libraryDependencies ++= Seq(
      "org.scalactic" %% "scalactic"   % "3.0.1",
      "org.scalatest" %% "scalatest"   % "3.0.1" % "test",
      "org.neo4j"     % "neo4j"        % "3.2.6",
      "org.neo4j"     % "neo4j-kernel" % "3.2.6",
      "org.neo4j"     % "neo4j-io"     % "3.2.6",
      "org.neo4j"     % "neo4j-kernel" % "3.2.6" % "test" classifier "tests",
      "org.neo4j"     % "neo4j-io"     % "3.2.6" % "test" classifier "tests"
    )
  )

lazy val jgrapht = (project in file("jgrapht"))
  .settings(commonSettings)
  .dependsOn(core % "compile->compile;test->test")
  .settings(
    libraryDependencies ++= Seq(
      "org.jgrapht" % "jgrapht-core" % "1.1.0",
      "org.scalatest" %% "scalatest"   % "3.0.1" % "test"
    )
  )

lazy val root = (project in file("."))
  .aggregate(core, neo4j, jgrapht)
