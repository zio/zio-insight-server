import BuildHelper._

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
    Test / run / fork      := true,
    Test / run / javaOptions += "-Djava.net.preferIPv4Stack=true",
    Test / run / mainClass := Some("sample.SampleApp"),
    cancelable             := true,
    stdSettings("zio.insight.server.core"),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"                    % Version.zio,
      "dev.zio" %% "zio-json"               % Version.zioJson,
      "dev.zio" %% "zio-streams"            % Version.zio,
      "dev.zio" %% "zio-metrics-connectors" % Version.zioMetricsConnectors,
      "io.d11"  %% "zhttp"                  % Version.zioHttp,
      "dev.zio" %% "zio-test"               % Version.zio % Test,
      "dev.zio" %% "zio-test-sbt"           % Version.zio % Test,
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
      "dev.zio" %% "zio"   % Version.zio,
      "io.d11"  %% "zhttp" % Version.zioHttp,
    ),
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(core),
    ScalaUnidoc / unidoc / target              := (LocalRootProject / baseDirectory).value / "website" / "static" / "api",
    cleanFiles += (ScalaUnidoc / unidoc / target).value,
  )
  .dependsOn(core)
  .enablePlugins(MdocPlugin, ScalaUnidocPlugin)

lazy val root = project
  .in(file("."))
  .aggregate(core, docs)
