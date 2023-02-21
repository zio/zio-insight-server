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

lazy val commonSettings = Seq()

lazy val core = project
  .in(file("core"))
  .settings(
    Test / run / fork := true,
    Test / run / javaOptions += "-Djava.net.preferIPv4Stack=true",
    // Test / run / mainClass := Some("sample.SampleApp"),
    cancelable        := true,
    stdSettings("zio.insight.server.core"),
    libraryDependencies ++= Seq(
      "com.github.ghostdogpr"       %% "caliban"                % Version.caliban,
      "com.github.ghostdogpr"       %% "caliban-tapir"          % Version.caliban,
      "com.softwaremill.sttp.tapir" %% "tapir-core"             % Version.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server"  % Version.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-json-zio"         % Version.tapir,
      "dev.zio"                     %% "zio"                    % Version.zio,
      "dev.zio"                     %% "zio-json"               % Version.zioJson,
      "dev.zio"                     %% "zio-streams"            % Version.zio,
      "dev.zio"                     %% "zio-metrics-connectors" % Version.zioMetricsConnectors,
      "dev.zio"                     %% "zio-http"               % Version.zioHttp,
      "dev.zio"                     %% "zio-test"               % Version.zio % Test,
      "dev.zio"                     %% "zio-test-sbt"           % Version.zio % Test,
    ),
    excludeDependencies ++= Seq(
      ExclusionRule("dev.zio", "zio-http"),
      ExclusionRule("dev.zio", "zio-http-logging"),
    ),
  )
  .settings(buildInfoSettings("zio.insight.server"))
  .enablePlugins(BuildInfoPlugin)

lazy val docs = project
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
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(core),
    ScalaUnidoc / unidoc / target              := (LocalRootProject / baseDirectory).value / "website" / "static" / "api",
    cleanFiles += (ScalaUnidoc / unidoc / target).value,
  )
  .dependsOn(core)
  .enablePlugins(MdocPlugin, ScalaUnidocPlugin)

lazy val root = project
  .in(file("."))
  .settings(name := "zio-insight-server")
  .aggregate(core, docs)
