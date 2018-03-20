name := "accounts-api"

git.gitTagToVersionNumber := { tag: String => Some(tag) }
git.useGitDescribe := false

scalaVersion := "2.12.4"

libraryDependencies += "io.monix" %% "monix" % "3.0.0-RC1"

libraryDependencies += "io.circe" %% "circe-generic" % "0.9.1"
libraryDependencies += "io.circe" %% "circe-parser" % "0.9.1"
libraryDependencies += "io.circe" %% "circe-fs2" % "0.9.0"

libraryDependencies += "org.http4s" %% "http4s-blaze-server" % "0.18.3"
libraryDependencies += "org.http4s" %% "http4s-circe" % "0.18.3"
libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.18.3"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

logBuffered in Test := false

enablePlugins(GitVersioning)
