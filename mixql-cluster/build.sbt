credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")

organization := "org.mixql"

name := "mixql-cluster"

version := "0.1.0"

organizationName := "MixQL"

organizationHomepage := Some(url("https://mixql.org/"))

description := "MixQL engine interface."

run / fork := true

scalaVersion := scala3Version

resolvers +=
  "Sonatype OSS Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= {
  val vScallop = "4.1.0"
  Seq(
    "org.rogach" %% "scallop" % vScallop,
    "com.typesafe" % "config" % "1.4.2",
    "org.scalameta" %% "munit" % "0.7.29" % Test,
    "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % "2.9.6-0",
    "org.zeromq" % "jeromq" % "0.5.2"
  )
}

licenses := List(
  "Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")
)

homepage := Some(url("https://github.com/mixql/mixql-engine"))

pomIncludeRepository := { _ => false }

publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

scmInfo := Some(
  ScmInfo(
    url("https://github.com/mixql/mixql-engine"),
    "scm:git@github.com:mixql/mixql-engine.git"
  )
)

val scala3Version = "3.2.0"


