import javax.script._

object Main extends App {

  val (key, storage, className, mainMethod) = ("arg1", "result", "HelloWorld", "main")

  val code =
    s"""
      |import scala.scalajs.js.Dynamic.{ global => g }
      |
      |object $className {
      |  def $mainMethod(args: Array[String]): Unit = {
      |    // args(0) = "$key"
      |    val name = g.selectDynamic(args(0))
      |    g.$storage = "Hello " + name + "!"
      |  }
      |}
    """.stripMargin

  val jsCode = ScalaJSCompiler.compile(code, className, mainMethod, args = List(key))

  /*
  // write ScalaJSCompiler output to file
  val fileWriter = new java.io.FileWriter("src/main/resources/test.js", false)
  fileWriter.flush()
  fileWriter.write(jsCode)
  fileWriter.close()
   */

  val engine: ScriptEngine = new ScriptEngineManager(null).getEngineByName("nashorn")
  val compilerEngine: ScriptEngine with Compilable = engine match {
    case se: ScriptEngine with Compilable => se
    case _ => throw new Exception("ScriptEngine not compilable")
  }


  val compiledScript = compilerEngine.compile(jsCode)
  println(code)


  val bindings = compilerEngine.getBindings(ScriptContext.GLOBAL_SCOPE)
  val getResult = {
    val scope = compilerEngine.getBindings(ScriptContext.ENGINE_SCOPE)
    scope.get(_)
  }

  bindings.put(key, "World")
  compiledScript.eval
  println(getResult(storage))

  bindings.put(key, "Finland")
  compiledScript.eval
  println(getResult(storage))

  bindings.put(key, "Italy")
  compiledScript.eval
  println(getResult(storage))

}
