package kz.greetgo.dt

import name.lakhin.eliah.projects.papacarlo.syntax.Node
import org.testng.annotations.Test

class ScalaDtLangTest {

  @Test
  def tst(): Unit = {
    val lexer = DtLang.lexer
    val syntax = DtLang.syntax(lexer)

    lexer.input("group(empty(), empty(), exit(), asd(1 + 2, 3), switch(case(1 < -3, empty())))")

    {
      val errors = syntax.getErrors
      if (errors.isEmpty) {
        println("* * *  NO Errors  * * *")
      } else {
        for (err <- errors) {
          println("ERROR " + err)
        }
      }
    }

    val root = syntax.getRootNode

    println("root.get.getKind = " + root.get.getKind)

    println("root.get.getId = " + root.get.getId)

    showBranches(root.get.getBranches, 0)
  }

  def showBranches(branches: Map[String, List[Node]], offset: Int): Unit = {
    var s = "_"
    for (i <- 0 until offset) {
      s = s + "__"
    }

    for ((k, v) <- branches) {
      for (x <- v) {
        println(s + k + " -- " + x.getKind + " " + x.getBegin + " " + x.getEnd + " [" + x.sourceCode + "]")
        showBranches(x.getBranches, offset + 1)
      }
    }
  }
}
