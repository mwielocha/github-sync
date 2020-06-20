
// Generated with scalagen

lazy val root = (project in file(".")).
  settings(
    name := "github-sync",
    version := "1.0",
    scalaVersion := "2.13.2"
  )

mainClass in (Compile, run) := Some("io.mwielocha.githubsync.Boot")

val akkaVersion = "2.6.4"
val slickVersion = "3.3.2"
val circeVersion = "0.13.0"
val slickpgVersion = "0.19.0"
val logbackVersion = "1.2.3"
val factorioVersion = "0.2.0"
val akkaHttpVersion = "10.1.12"
val slicklessVersion = "0.3.6"
val shapelessVersion = "2.3.3"
val scalatestVersion = "3.1.1"
val pureConfigVersion = "0.12.3"
val scalaLoggingVersion = "3.9.2"
val akkaHttpCirceVersion = "1.31.0"

scalacOptions ++= Seq(
  "-language:postfixOps",
  "-language:implicitConversions",
  "-deprecation",
  "-feature",
  "-Yrangepos",
  "-language:existentials",
  "-language:higherKinds",
  "-language:postfixOps",
  "-Xmacro-settings:factorio-verbose"
)

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "com.github.tminglei" %% "slick-pg" % slickpgVersion,
  "com.github.tminglei" %% "slick-pg_circe-json" % slickpgVersion,
  "io.underscore"      %% "slickless" % slicklessVersion,
  "com.chuusai" %% "shapeless" % shapelessVersion,
  "io.mwielocha" %% "factorio-core" % factorioVersion,
  "io.mwielocha" %% "factorio-annotations" % factorioVersion,
  "io.mwielocha" %% "factorio-macro" % factorioVersion % "provided",
  "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirceVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % scalatestVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
)

