package kz.greetgo.dt

import java.time.LocalDate
import java.util
import java.util.function.Consumer

import name.lakhin.eliah.projects.papacarlo.syntax.Node
import org.testng.annotations.Test

/**
  * Created by den on 05.08.16.
  */
class DtLangInterpreter2Test {

  val nativeFunctions: NativeFunctions = new NativeFunctions {
    override def today(): LocalDate = LocalDate.now

    override def generateId(): String = Math.random.toString

    override def businessDay(): LocalDate = LocalDate.now()

    override def nextNumber(): Long = 10

    override def echo(value: DtType): Unit = System.out.println(value)

    override def echoScope(scope: util.SortedMap[String, DtType]): Unit = System.out.println(scope)

    override def dictInc(dictCode: String, fieldCode: String, rowCode: String, incrementValue: BigDecimal): Boolean = false

    override def dictValueText(dictCode: String, fieldCode: String, rowCode: String): String = Math.random.toString

    override def dictValueNumber(dictCode: String, fieldCode: String, rowCode: String): BigDecimal = BigDecimal.apply(12);
  }

  @Test
  def tst() = {
    val map: util.HashMap[String, DtType] = new util.HashMap()
    val scope = new Obj(map);

    val assign = (name: String, value: Option[DtType]) => {
      println(name, value)
      value
    }

    val userProcedures: util.Map[String, Node] = new util.HashMap()
    val nativeProcedures: util.Map[String, Consumer[Obj]] = new util.HashMap
    val interpreter = new DtLangInterpreter2(scope, nativeProcedures, userProcedures, nativeFunctions, null)
    val lexer = DtLang.lexer
    val syntax = DtLang.syntax(lexer)

    lexer.input(
      """
        |    group (
        |      assign(z[1], true()),
        |      assign(y[1].b[2], "y"),
        |      assign(x[id: 5].type, "x2"),
        |      assign(t, x[1]),
        |      assign(dat1, date("2015-01-02")),
        |      assign(dat2, date(2015,1,2) + (4*day()+3*month()) ),
        |      assign(a, 5+2),
        |      assign(b, "hello"),
        |      assign(b, a~b),
        |
        |      assign(pathed, x[type:"x2"][id:5].type),
        |      assign(q[5].type, 5),
        |      assign(date20, setDay(date("2016-01-02"), 20)),
        |      assign(date40IsDefined, isDefined(setDay(date("2016-01-02"), 40))),
        |      assign(z[2], false()),
        |      assign(min, min(z)),
        |      assign(x[id: 6].type, "x6"),
        |      assign(index6, index(x[id:6])),
        |      assign(index7, index(x[id:7]))
        |    )
      """.stripMargin)
    //    assign(l, len(x)),

    interpreter.eval(syntax.getRootNode.get)
    println(scope)
  }

  @Test
  def breakpoints() = {
    val map: util.HashMap[String, DtType] = new util.HashMap()
    val scope = new Obj(map);

    val userProcedures: util.Map[String, Node] = new util.HashMap()
    val nativeProcedures: util.Map[String, Consumer[Obj]] = new util.HashMap
    // to find out node ids set breakpoint to "assign" and watch arg(0).getParent.get.getParent.get.getId
    val interpreter = new DtLangInterpreter2(scope, nativeProcedures, userProcedures, nativeFunctions, util.Collections.singletonList(40)) // 6 23 _40_ 57
    val lexer = DtLang.lexer
    val syntax = DtLang.syntax(lexer)

    lexer.input(
      """group(
        |      assign(client.accountX[id:5].sum, 20),
        |      assign(client.accountX[id:6].sum, 22),
        |      assign(client.accountX[id:5].sum, 21),
        |      assign(client.accountX[id:6].sum, 23)
        |)
      """.stripMargin)

    interpreter.eval(syntax.getRootNode.get)
    println(scope)
    println(interpreter.reached)
    assert(interpreter.reached.containsAll(util.Arrays.asList(6, 23, 40)))
    assert(!interpreter.reached.contains(57))
  }
}
