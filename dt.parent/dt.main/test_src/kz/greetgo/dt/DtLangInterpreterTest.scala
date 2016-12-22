package kz.greetgo.dt

import java.time.LocalDate
import java.util
import java.util.function.Consumer

import name.lakhin.eliah.projects.papacarlo.syntax.Node
import org.testng.annotations.Test

class DtLangInterpreterTest {

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
    val scope: util.SortedMap[String, DtType] = new util.TreeMap()
    scope.put("a", Str("AA"))
    scope.put("b", Str("BB"))
    scope.put("x.1.type", Str("x1"))
    scope.put("x.2.type", Str("x2"))
    scope.put("x.2.1", Str("x2.1"))

    scope.put("m.0", Str("3"))
    scope.put("m.1", Str("1"))
    scope.put("m.2", Str("2"))

    val assign = (name: String, value: Option[DtType]) => {
      println(name, value)
      value
    }

    val userProcedures: util.Map[String, Node] = new util.HashMap()
    val nativeProcedures: util.Map[String, Consumer[util.SortedMap[String, DtType]]] = new util.HashMap
    val interpreter = new DtLangInterpreter(scope, nativeProcedures, userProcedures, nativeFunctions, null)
    val lexer = DtLang.lexer
    val syntax = DtLang.syntax(lexer)

    lexer.input(
      """
        |condition (
        |  case (1=1,
        |    group (
        |      assign(a, 5+2),
        |      assign(b, a~b),
        |      assign(l, len(x)),
        |      assign(dat1, date("2015-01-02")),
        |      assign(dat2, date(2015,1,2) + (4*day()+3*month()) ),
        |      assign(pathed, x[type:"x2"][id:5].type),
        |      assign(q[5].type, 5),
        |      assign(date20, setDay(date("2016-01-02"), 20)),
        |      assign(date40IsDefined, isDefined(setDay(date("2016-01-02"), 40))),
        |      assign(min, min(m))
        |    )
        |  )
        |)
      """.stripMargin)

    interpreter.eval(syntax.getRootNode.get)
    println(scope)
  }

  @Test
  def tst2() = {
    val scope: util.SortedMap[String, DtType] = new util.TreeMap()

    val assign = (name: String, value: Option[DtType]) => {
      println(name, value)
      value
    }

    val userProcedures: util.Map[String, Node] = new util.HashMap()
    val nativeProcedures: util.Map[String, Consumer[util.SortedMap[String, DtType]]] = new util.HashMap

    {
      val interpreter = new DtLangInterpreter(scope, nativeProcedures, userProcedures, nativeFunctions, null)
      val lexer = DtLang.lexer
      val syntax = DtLang.syntax(lexer)

      lexer.input(
        """
          |group (
          |foreach (i, 0, 2,
          |    group
          |       (
          |       assign (tmpId, generateId()),
          |       assign (repaymentPlan[id:tmpId], i)
          |       )
          |    )
          |assign (l, len(repaymentPlan))
          |)
        """.
          stripMargin)

      interpreter.eval(syntax.getRootNode.get)
      println(scope)
    }

    {
      val interpreter = new DtLangInterpreter(scope, nativeProcedures, userProcedures, nativeFunctions, null)
      val lexer = DtLang.lexer
      val syntax = DtLang.syntax(lexer)

      lexer.input(
        """
          |group (
          |foreach (i, 0, len(repaymentPlan),
          |       assign (repaymentPlan[i].num, i)
          |       )
          |)
        """.
          stripMargin)

      interpreter.eval(syntax.getRootNode.get)
      println(scope)
    }
  }

  @Test
  def tst3() = {
    val scope: util.SortedMap[String, DtType] = new util.TreeMap()

    val assign = (name: String, value: Option[DtType]) => {
      println(name, value)
      value
    }

    val userProcedures: util.Map[String, Node] = new util.HashMap()
    val nativeProcedures: util.Map[String, Consumer[util.SortedMap[String, DtType]]] = new util.HashMap
    val interpreter = new DtLangInterpreter(scope, nativeProcedures, userProcedures, nativeFunctions, null)
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
  }

  @Test
  def breakpoints() = {
    val scope: util.SortedMap[String, DtType] = new util.TreeMap()

    val assign = (name: String, value: Option[DtType]) => {
      println(name, value)
      value
    }

    val userProcedures: util.Map[String, Node] = new util.HashMap()
    val nativeProcedures: util.Map[String, Consumer[util.SortedMap[String, DtType]]] = new util.HashMap
    // to find out node ids set breakpoint to "assign" and watch arg(0).getParent.get.getParent.get.getId
    val interpreter = new DtLangInterpreter(scope, nativeProcedures, userProcedures, nativeFunctions, util.Collections.singletonList(40)) // 6 23 _40_ 57
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
