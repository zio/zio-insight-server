import BuildHelper._

Global / cancelable           := true
Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  List(
    organization   := "dev.zio",
    homepage       := Some(url("https://zio.github.io/zio.insight.server")),
    licenses       := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers     := List(
      Developer(
        "jdegoes",
        "John De Goes",
        "john@degoes.net",
        url("http://degoes.net"),
      ),
    ),
    pgpPassphrase  := sys.env.get("PGP_PASSWORD").map(_.toArray),
    pgpPublicRing  := file("/tmp/public.asc"),
    pgpSecretRing  := file("/tmp/secret.asc"),
    resolvers ++= Seq(
      "s01 Sonatype OSS Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    ),
    scmInfo        := Some(
      ScmInfo(url("https://github.com/zio/zio-insight-server"), "scm:git:git@github.com:zio/zio-insight-server.git"),
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  ),
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val commonSettings = Seq(
  Test / run / fork        := true,
  Test / run / javaOptions += "-Djava.net.preferIPv4Stack=true",
  Test / parallelExecution := false,
  cancelable               := true,
)

lazy val zioCommonDeps = Seq(
  "dev.zio" %% "zio"          % Version.zio,
  "dev.zio" %% "zio-json"     % Version.zioJson,
  "dev.zio" %% "zio-streams"  % Version.zio,
  "dev.zio" %% "zio-test"     % Version.zio % Test,
  "dev.zio" %% "zio-test-sbt" % Version.zio % Test,
)

val api = project
  .in(file("api"))
  .settings(
    commonSettings,
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = false) -> (Compile / sourceManaged).value / "scalapb",
    ),
    stdSettings("zio.insight.server.core"),
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime"        % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "dev.zio"              %% "zio-metrics-connectors" % Version.zioMetricsConnectors,
      "dev.zio"              %% "zio-http"               % Version.zioHttp,
    ) ++ zioCommonDeps,
    excludeDependencies ++= Seq(
      ExclusionRule("dev.zio", "zio-http"),
      ExclusionRule("dev.zio", "zio-http-logging"),
    ),
  )
  .settings(buildInfoSettings("zio.insight.server.core"))
  .enablePlugins(BuildInfoPlugin)

val agent = project
  .in(file("agent"))
  .settings(
    commonSettings,
    stdSettings("zio.insight.agent"),
    libraryDependencies ++= zioCommonDeps,
  )
  .dependsOn(api)

val redis = project
  .in(file("redis"))
  .settings(
    commonSettings,
    stdSettings("zio.insight.redis"),
    libraryDependencies ++= zioCommonDeps,
  )
  .dependsOn(api)

val server = project
  .in(file("server"))
  .settings(
    commonSettings,
    stdSettings("zio.insight.server"),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"      % Version.zio,
      "dev.zio" %% "zio-http" % Version.zioHttp,
    ),
  )
  .dependsOn(api, redis)

// Examples

lazy val exampleSimple = project
  .in(file("examples/simple"))
  .settings(
    commonSettings,
    stdSettings("zio.insight.examples.simple"),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-metrics-connectors" % Version.zioMetricsConnectors,
      "dev.zio" %% "zio-http"               % Version.zioHttp,
    ) ++
      zioCommonDeps,
    excludeDependencies ++= Seq(
      ExclusionRule("dev.zio", "zio-http"),
      ExclusionRule("dev.zio", "zio-http-logging"),
    ),
  )
  .dependsOn(api)

val docs = project
  .in(file("genDocs"))
  .settings(
    commonSettings,
    publish / skip                             := true,
    moduleName                                 := "zio-insight-server-docs",
    scalacOptions -= "-Yno-imports",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"      % Version.zio,
      "dev.zio" %% "zio-http" % Version.zioHttp,
    ),
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(api, agent, server),
    ScalaUnidoc / unidoc / target              := (LocalRootProject / baseDirectory).value / "website" / "static" / "api",
    cleanFiles += (ScalaUnidoc / unidoc / target).value,
  )
  .dependsOn(server, api, agent, redis)
  .enablePlugins(MdocPlugin, ScalaUnidocPlugin)

val root = project
  .in(file("."))
  .settings(name := "zio-insight-server")
  .aggregate(server, api, agent, redis, docs, exampleSimple)
