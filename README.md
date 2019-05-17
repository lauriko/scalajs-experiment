# Description
This is a sample program that compiles ScalaJS code into JavaScript and evaluates it. Main goal of this program is to
research how to compile ScalaJS at runtime and evaluate the result. Run program with command: `sbt run`.

# ScalaJSCompiler
ScalaJSCompiler has one method called `compile` which takes the ScalaJS code as `String` and returns JavaScript code as
`String`. It uses Scala compiler with `ScalaJSPlugin` to compile the code.

## Scala Compiler
Libraries are provided to the compiler via `scala.tools.nsc.Settings` class. Easiest way to add libraries was to use
`Settings.embeddedDefaults` to export class paths from `ClassLoader`.
```Scala
val settings = new Settings()
settings.embeddedDefaults(getClass.getClassLoader)
val compiler = new Global(settings, reporter) {
  override lazy val plugins: List[Plugin] =
    List(new org.scalajs.core.compiler.ScalaJSPlugin(this))
}
```
ScalaJS code can be then compiled into `.class` and `.sjsir` files using the `Global.Run` class
```Scala
val run = new compiler.Run()
run.compilerFiles(List(scalaJsFile))
```
## Linker
`StandarLinker` links the `.sjsir` files to JavaScript code. It can be configured to optimize the output to be smaller
in size.

The linker's `link` function requires the `.sjsir` files and `scalajs-library` in `Seq[VirtualScalaJSIRFile]`,
`ModuleInitializer` to invoke the main function, `WritableVirtualJSFile` as output and `Logger`. Arguments for the main
function are given in the `ModuleInitializer`.
```Scala
val irCache = new IRFileCache().newCache
val irContainers = IRFileCache.IRContainer.fromClasspath(
  Seq(new File(scalaJSLibrary)))
val linkerLibraries = irCache.cached(irContainers)

val sjsirFiles = for {
    x <- target.iterator.to[collection.immutable.Traversable]
    if x.name.endsWith(".sjsir")
  } yield {
    val f = new MemVirtualSerializedScalaJSIRFile(x.path)
    f.content = x.toByteArray
    f: VirtualScalaJSIRFile
  }
  
val mainModuleInitializer = ModuleInitializer.mainMethodWithArgs(className, mainMethod, args)
val output = WritableMemVirtualJSFile("output.js")
val linkerConfig = StandardLinker.Config()
val linker = StandardLinker(linkerConfig)
linker.link(linkerLibraries ++ sjsirFiles.toSeq,
  Seq(mainModuleInitializer),
  output,
  new ScalaConsoleLogger())
```
The output is one big immediately invoked function that doesn't return anything. Not even the output of the main
function. Ways to pass parameters and extract output will be discussed in the next section.

# Main
Main contains the main method of this program and it has small hardcoded ScalaJS code that will be compiled to
JavaScript and then evaluated by `Nashorn` JavaScript engine through `ScriptEngine` interface. Optional `Compilable`
interface is used to compile the script for later use.
```scala
val engine: ScriptEngine = new ScriptEngineManager(null).getEngineByName("nashorn")
val compilerEngine: ScriptEngine with Compilable = engine match {
  case se: ScriptEngine with Compilable => se
  case _ => throw new Exception("ScriptEngine not compilable")
}
val compiledScript = compilerEngine.compile(jsCode)
```

## Passing parameters and getting the result
The output of the `ScalaJSCompiler` does not return anything when evaluated. The only way to get results is to
exploit ScriptEngine's scope so that the arguments and results are passed through it.

```Scala
/**
* ScalaJS script which process value in global scope under "param" key
* and stores the result under "result"
*/
import scala.scalajs.js.Dynamic.{ global => g }

object Main extends {
  def main(args: Array[String]): Unit = {
    val name = g.selectDynamic("param")
    g.result = "Hello " + name + "!"
  }
}
```

```Scala
val bindings = compilerEngine.getBindings(ScriptContext.GLOBAL_SCOPE)
bindings.put("param", "World")
compiledScript.eval

val scope = compilerEngine.getBindings(ScriptContext.ENGINE_SCOPE)
val result = scope.get("result")
```
