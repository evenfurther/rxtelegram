import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

SbtScalariform.scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignArguments, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(SpacesWithinPatternBinders, false)
  .setPreference(SpacesAroundMultiImports, false)

name := "rxtelegram"

organization := "net.rfc1149"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.8"

resolvers ++= Seq("Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Sonatype OSS Releases"  at "http://oss.sonatype.org/content/repositories/releases/",
                  Resolver.jcenterRepo)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.10",
  "com.typesafe.akka" %% "akka-stream" % "2.4.10",
  "com.typesafe.akka" %% "akka-http-core" % "2.4.10",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.10",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.9.0",
  "com.iheart" %% "ficus" % "1.2.6",
  "commons-io" % "commons-io" % "2.5",
  "org.specs2" %% "specs2-core" % "3.8.4" % "test"
)

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

fork in Test := true

publishTo := {
  val path = "/home/sam/rfc1149.net/data/ivy2/" + (if (version.value.trim.endsWith("SNAPSHOT")) "snapshots/" else "releases")
  Some(Resolver.ssh("rfc1149 ivy releases", "rfc1149.net", path) as "sam" withPermissions("0644"))
}
