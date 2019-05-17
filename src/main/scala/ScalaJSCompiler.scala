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

    val scalaJSFile = makeFile(code.getBytes("UTF-8"))

    val target = new VirtualDirectory("", None)

    val settings = new Settings()
    //settings.processArgumentString("-Ydebug -Ypartial-unification -Ylog-classpath")
    settings.outputDirs.setSingleOutput(target)
    settings.embeddedDefaults(getClass.getClassLoader)

    val classPaths = settings.classpath.value.split(":")
    val scalaJSLibrary = classPaths.find(_.contains("scalajs-library")).get

    val reporter = new ConsoleReporter(new Settings)
    val compiler = new Global(settings, reporter) {
      override lazy val plugins: List[Plugin] = List(new org.scalajs.core.compiler.ScalaJSPlugin(this))
    }

    val run = new compiler.Run()

    run.compileFiles(List(scalaJSFile))
    //run.compileSources(List(file))

    val irCache = new IRFileCache().newCache
    val irContainers = IRFileCache.IRContainer.fromClasspath(Seq(new File(scalaJSLibrary)))
    val linkerLibraries = irCache.cached(irContainers)

    val sjsirFiles = for {
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
    linker.link(linkerLibraries ++ sjsirFiles.toSeq, Seq(mainModuleInitializer), output, new ScalaConsoleLogger())

    output.content

  }

}
