name := "scalajs-experiment"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  "org.scala-js" % "scalajs-compiler_2.12.8" % "0.6.27",
  "org.scala-js" % "scalajs-tools_2.12" % "0.6.27"
)