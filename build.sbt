name := "scalajs-experiment"

version := "0.1"

scalaVersion := "2.12.8"
val scalaJSVersion = "0.6.27"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  "org.scala-js" % "scalajs-compiler_2.12.8" % scalaJSVersion,
  "org.scala-js" %% "scalajs-tools" % scalaJSVersion,
  "org.scala-js" %% "scalajs-library" % scalaJSVersion
)

