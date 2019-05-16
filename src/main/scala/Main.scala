import javax.script._

object Main extends App {

  val (key, storage, className, mainMethod) = ("arg1", "result", "HelloWorld", "main")

  val code =
    s"""
      |import scala.scalajs.js.Dynamic.{ global => g }
      |
      |object $className extends {
      |  def main(args: Array[String]): Unit = {
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
  val compilerEngine: ScriptEngine  with Compilable = engine match {
    case se: ScriptEngine with Compilable => se
    case _ => throw new Exception("ScriptEngine not compilable")
  }

  // bind parameters
  val bindings = new SimpleBindings()
  compilerEngine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE)

  val compiledScript = compilerEngine.compile(jsCode)
  println(code)

  bindings.put(key, "World")
  compiledScript.eval
  println(compilerEngine.getBindings(ScriptContext.ENGINE_SCOPE).get(storage))

  bindings.put(key, "Finland")
  compiledScript.eval
  println(compilerEngine.getBindings(ScriptContext.ENGINE_SCOPE).get(storage))

}
