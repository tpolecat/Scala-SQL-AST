/**
  * Large portions of this build are based on @tpolecat's (Rob Norris) build file for doobie. Any genius found here is courtesy of him.
  */

import UnidocKeys._
import ReleaseTransformations._
import OsgiKeys._

lazy val buildSettings = Seq(
  scalaVersion := "2.11.8",
  organization := "com.github.jacoby6000",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  crossScalaVersions := Seq("2.10.5", scalaVersion.value)
)



lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8", // 2 args
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:experimental.macros",
    "-unchecked",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-value-discard",
    "-Xmax-classfile-name", "128"
//    "-Ypatmat-exhaust-depth", "off"
  ),
  scalacOptions in (Compile, doc) ++= Seq(
    "-groups",
    "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath,
    "-doc-source-url", "https://github.com/jacoby6000/scoobie/tree/v" + version.value + "€{FILE_PATH}.scala"
  ),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.7.1")
)

lazy val publishSettings = osgiSettings ++ Seq(
  exportPackage := Seq("scoobie.*"),
  privatePackage := Seq(),
  dynamicImportPackage := Seq("*"),
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  homepage := Some(url("https://github.com/jacoby6000/scoobie")),
  pomIncludeRepository := Function.const(false),
  pomExtra :=
  <scm>
    <url>git@github.com:Jacoby6000/scoobie.git</url>
    <connection>scm:git:git@github.com:Jacoby6000/scoobie.git</connection>
  </scm>
  <developers>
    <developer>
      <id>Jacoby6000</id>
      <name>Jacob Barber</name>
      <url>http://jacoby6000.github.com/</url>
    </developer>
  </developers>,
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    ReleaseStep(action = Command.process("package", _)),
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = Command.process("publishSigned", _)),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  )
)

lazy val scoobieSettings = buildSettings ++ commonSettings ++ tutSettings

lazy val task71Compat =
  project.in(file("scalaz-compat"))
    .settings(scoobieSettings ++ noPublishSettings)
    .settings(name := "scoobie-task-compat")
    .settings(description := "Provides .unsafePerformSync to scalaz 7.1 task")
    .settings(libraryDependencies += "org.scalaz" %% "scalaz-concurrent" % "7.1.6")

lazy val scoobie =
  project.in(file("."))
    .settings(name := "scoobie")
    .settings(scoobieSettings ++ noPublishSettings)
    .settings(unidocSettings)
    .settings(unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(docs))
    .settings(
      tutSourceDirectory := file("doc") / "src" / "main" / "tut",
      tutTargetDirectory := file("doc") / "target" / "scala-2.11" / "tut"
    )
    .dependsOn(core, postgres, weakSqlDsl)
    .aggregate(core, doobieSupport, doobieSupport23, doobieSupport30, postgres, postgres23, postgres30, weakSqlDsl, docs)
    .settings(
      tutcp <<= (tut map { a =>

        val src = file(".") / "doc" / "target" / "scala-2.11" / "tut" / "readme.md"
        val dst = file(".") / "readme.md"

        println("Copying " + src + " to " + dst)

        if (src.exists())
          IO.copy(List(src -> dst), overwrite = true, preserveLastModified = true)
        else
          println("No tut output found at" + src.toString)

        a
      })
    )

lazy val core =
  project.in(file("core"))
    .enablePlugins(SbtOsgi)
    .settings(name := "scoobie-core")
    .settings(description := "AST for making convenient SQL DSLs in Scala.")
    .settings(scoobieSettings ++ publishSettings)
    .settings(libraryDependencies ++= Seq(shapeless, specs72))
    .settings(packageInfoGenerator("scoobie", "scoobie-core"))

lazy val doobieSupport =
  project.in(file("doobie-support"))
    .settings(scoobieSettings)
    .settings(description := "Introduces doobie support to scoobie.")
    .settings(noPublishSettings)
    .settings(libraryDependencies ++= Seq(doobieCore % doobieVersion30))
    .dependsOn(core)

lazy val doobieSupport23 =
  project.in(file("doobie-support"))
    .enablePlugins(SbtOsgi)
    .settings(target := file("doobie-support").getAbsoluteFile / "target23")
    .settings(publishSettings)
    .settings(name := "scoobie-contrib-doobie23-support")
    .settings(description := "Introduces doobie support to scoobie.")
    .settings(libraryDependencies ++= Seq(doobieCore % doobieVersion23 force(), specs71))
    .settings(packageInfoGenerator("scoobie.doobie", "scoobie-doobie23-support"))
    .settings(scoobieSettings)
    .dependsOn(core, task71Compat % "test")

lazy val doobieSupport30 =
  project.in(file("doobie-support"))
    .enablePlugins(SbtOsgi)
    .settings(target := file("doobie-support").getAbsoluteFile / "target30")
    .settings(publishSettings)
    .settings(name := "scoobie-contrib-doobie30-support")
    .settings(description := "Introduces doobie support to scoobie.")
    .settings(libraryDependencies ++= Seq(doobieCore % doobieVersion30, specs72))
    .settings(packageInfoGenerator("scoobie.doobie", "scoobie-doobie30-support"))
    .settings(scoobieSettings)
    .dependsOn(core)

lazy val postgres =
  project.in(file("postgres"))
    .settings(noPublishSettings)
    .settings(scoobieSettings)
    .settings(description := "Introduces doobie support to scoobie with postgres.")
    .settings(libraryDependencies ++= Seq(doobiePGDriver % doobieVersion30, specs72))
    .dependsOn(doobieSupport, weakSqlDsl % "test")

lazy val postgres23 =
  project.in(file("postgres"))
    .enablePlugins(SbtOsgi)
    .settings(target := file("postgres").getAbsoluteFile / "target23")
    .settings(publishSettings)
    .settings(scoobieSettings)
    .settings(name := "scoobie-contrib-doobie23-postgres")
    .settings(description := "Introduces doobie support to scoobie with postgres.")
    .settings(libraryDependencies ++= Seq(doobiePGDriver % doobieVersion23, specs71))
    .settings(packageInfoGenerator("scoobie.doobie.postgres", "scoobie-contrib-doobie23-postgres"))
    .dependsOn(doobieSupport23, weakSqlDsl % "test", task71Compat % "test")

lazy val postgres30 =
  project.in(file("postgres"))
    .enablePlugins(SbtOsgi)
    .settings(target := file("postgres").getAbsoluteFile / "target30")
    .settings(publishSettings)
    .settings(scoobieSettings)
    .settings(name := "scoobie-contrib-doobie30-postgres")
    .settings(description := "Introduces doobie support to scoobie with postgres.")
    .settings(libraryDependencies ++= Seq(doobiePGDriver % doobieVersion30, specs72))
    .settings(packageInfoGenerator("scoobie.doobie.postgres", "scoobie-contrib-doobie30-postgres"))
    .dependsOn(doobieSupport30, weakSqlDsl % "test")


lazy val weakSqlDsl =
  project.in(file("mild-sql-dsl"))
    .enablePlugins(SbtOsgi)
    .settings(scoobieSettings ++ publishSettings)
    .settings(name := "scoobie-contrib-mild-sql-dsl")
    .settings(description := "Introduces a weakly typed SQL DSL to scoobie.")
    .settings(libraryDependencies += specs72)
    .settings(packageInfoGenerator("scoobie.dsl.weaksql", "scoobie-contrib-mild-sql-dsl"))
    .dependsOn(core)


lazy val docs =
  project.in(file("doc"))
    .settings(scoobieSettings ++ noPublishSettings)
    .settings(
      ctut := {
        val src = crossTarget.value / "tut"
        val dst = file("../jacoby6000.github.io/_scoobie-" + version.value + "/")
        if (!src.isDirectory) {
          println("Input directory " + src + " not found.")
        } else if (!dst.isDirectory) {
          println("Output directory " + dst + " not found.")
        } else {
          println("Copying to " + dst.getPath)
          val map = src.listFiles.filter(_.getName.endsWith(".md")).map(f => (f, new File(dst, f.getName)))
          IO.copy(map, overwrite = true, preserveLastModified = false)
        }
      }
    )
    .dependsOn(postgres30, weakSqlDsl)


lazy val doobieVersion30 = "0.3.0"
lazy val doobieVersion23 = "0.2.3"
lazy val doobieCore = "org.tpolecat" %% "doobie-core"
lazy val doobiePGDriver = "org.tpolecat" %% "doobie-contrib-postgresql"

lazy val specs71 = "org.specs2" %% "specs2-core" % "3.8.3-scalaz-7.1" % "test"
lazy val specs72 = "org.specs2" %% "specs2-core" % "3.8.3" % "test"


lazy val shapeless = "com.chuusai" %% "shapeless" % "2.3.1"

lazy val ctut = taskKey[Unit]("Copy tut output to blog repo nearby.")

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val tutcp = taskKey[Seq[(sbt.File, String)]]("Copy tut readme output to projroot/readme.md")

def packageInfoGenerator(packageName: String, artifactName: String) =
  sourceGenerators in Compile += Def.task {
    val outDir = (sourceManaged in Compile).value / artifactName
    val outFile = new File(outDir, "buildinfo.scala")
    outDir.mkdirs
    val v = version.value
    val t = System.currentTimeMillis
    IO.write(outFile,
      s"""|package $packageName
          |
          |/** Auto-generated build information. */
          |object buildinfo {
          |  /** Current version of $artifactName ($v). */
          |  val version = "$v"
          |  /** Build date (${new java.util.Date(t)}). */
          |  val date    = new java.util.Date(${t}L)
          |}
          |""".stripMargin)
    Seq(outFile)
  }.taskValue
