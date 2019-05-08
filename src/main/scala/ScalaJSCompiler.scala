import scala.reflect.io
import scala.reflect.io.{VirtualDirectory, VirtualFile}
import scala.tools.nsc
import scala.tools.nsc.classpath.AggregateClassPath
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.{Global, Settings}
import scala.tools.nsc.reporters.{ConsoleReporter, StoreReporter}


object ScalaJSCompiler {

  def makeFile(src: Array[Byte]) = {
    val singleFile = new io.VirtualFile("ScalaFiddle.scala")
    val output     = singleFile.output
    output.write(src)
    output.close()
    singleFile
  }

  def main(args: Array[String]): Unit = {
    val code: String =
      """
        |object foo {
        |  def bar: String = "foobar"
        |}
      """.stripMargin

    val singleFile = makeFile(code.getBytes("UTF-8"))

    val target = new VirtualDirectory("", None)

    val settings = new Settings()
    settings.outputDirs.setSingleOutput(target)
    settings.usejavacp.value = true
    settings.embeddedDefaults(getClass.getClassLoader)

    val reporter = new ConsoleReporter(settings)

    val compiler: Global = new nsc.Global(settings, reporter) { g =>
      override lazy val plugins: List[Plugin] = List[Plugin](
        new org.scalajs.core.compiler.ScalaJSPlugin(this)
      )
    }


    val run = new compiler.Run()

    run.compileFiles(List(singleFile))
  }
}
