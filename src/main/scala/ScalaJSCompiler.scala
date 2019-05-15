import java.io.File

import org.scalajs.core.tools.io.{IRFileCache, MemVirtualSerializedScalaJSIRFile, VirtualScalaJSIRFile, WritableMemVirtualJSFile}
import org.scalajs.core.tools.linker.{ModuleInitializer, StandardLinker}
import org.scalajs.core.tools.logging._

import scala.reflect.io
import scala.reflect.io.VirtualDirectory
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.{Global, Settings}

object ScalaJSCompiler {

  private def makeFile(src: Array[Byte], fileName: String = "ScalaCode.scala") = {
    val singleFile = new io.VirtualFile(fileName)
    val output     = singleFile.output
    output.write(src)
    output.close()
    singleFile
  }

  def compile(code: String, className: String, mainMethod: String = "main", args: List[String] = Nil) : String = {

    val file = makeFile(code.getBytes("UTF-8"))

    val target = new VirtualDirectory("", None)

    val settings = new Settings()
    settings.outputDirs.setSingleOutput(target)
    settings.embeddedDefaults(getClass.getClassLoader)

    val classPaths = settings.classpath.value.split(":")
    val scalaJSLibrary = classPaths.find(_.contains("scalajs-library")).get

    val reporter = new ConsoleReporter(settings)
    val compiler = new Global(settings, reporter) { g =>
      override lazy val plugins: List[Plugin] = List(new org.scalajs.core.compiler.ScalaJSPlugin(this))
    }

    val run = new compiler.Run()

    run.compileFiles(List(file))
    //run.compileSources(List(file))

    val irCache = new IRFileCache().newCache

    val irContainers = IRFileCache.IRContainer.fromClasspath(Seq(new File(scalaJSLibrary)))
    val sjsirFiles = irCache.cached(irContainers)

    val things = for {
      x <- target.iterator.to[collection.immutable.Traversable]
      if x.name.endsWith(".sjsir")
    } yield {
      val f = new MemVirtualSerializedScalaJSIRFile(x.path)
      f.content = x.toByteArray
      f: VirtualScalaJSIRFile
    }

    val output = WritableMemVirtualJSFile("output.js")


    val linkerConfig = StandardLinker.Config() /*
      .withSemantics(Semantics.Defaults.optimized)
      .withClosureCompilerIfAvailable(true)
      .withSourceMap(false) */

    val mainModuleInitializer = ModuleInitializer.mainMethodWithArgs(className, mainMethod, args)
    val linker = StandardLinker(linkerConfig)
    linker.link(sjsirFiles ++ things.toSeq, Seq(mainModuleInitializer), output, new ScalaConsoleLogger())

    output.content
  }

}
