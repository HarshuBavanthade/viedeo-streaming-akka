ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.18"

val AkkaVersion = "2.10.11"
val AkkaHttpVersion = "10.7.2"
val ScalaBaseVersion = "2.13"

lazy val root = (project in file("."))
  .settings(
    name := "VideoStreaming"
  )

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-pki" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion
)

ThisBuild / resolvers +=
  "Akka repository".at("https://repo.akka.io/biGwiqYxoOgxkaS8tbM4d1BJaDsigJScxD7IcuucAZpeyKDk/secure")
