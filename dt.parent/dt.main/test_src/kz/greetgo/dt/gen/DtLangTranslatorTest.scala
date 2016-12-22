package kz.greetgo.dt.gen

import java.util
import java.util.{Arrays, Collections, Formatter}
import java.lang.StringBuilder

import kz.greetgo.dt.{DtLang, DtLangInterpreter, DtType, Num}
import org.testng.annotations.Test
import scala.collection.JavaConversions._

/**
  * Created by den on 03.10.16.
  */
class DtLangTranslatorTest {
  @Test
  def testMethod {
    val lexer = DtLang.lexer
    val syntax = DtLang.syntax(lexer)
    lexer.input(input)

    val tr: DtLangTranslator = new DtLangTranslator(simpleAttrs)
    val out = tr.translate(syntax.getRootNode.get)
    System.out.println(out._1)
  }

  @Test
  def test {
    val sb: StringBuilder = new StringBuilder
    val formatter: Formatter = new Formatter(sb)
    val tr: DtLangTranslator = new DtLangTranslator(simpleAttrs)
    tr.registerEntryPoint("main", input)
    tr.generate(formatter, "kz.greetgo.dt.gen")
    System.out.println(sb.toString)
  }

  private val input =
    """
      |condition (
      |  case (1=1,
      |    group (
      |      assign(a, 5+2),
      |      assign(b, a~b),
      |      assign(l, len(x)),
      |      assign(dat2, date(2015,1,2) + (4*day()+3*month()) )
      |    )
      |  ),
      |  case (1=2, assign(x,2))
      |)
    """.stripMargin;

  private val simpleAttrs: AstObj = obj(
    "a" -> AstNum(),
    "b" -> AstStr(),
    "dat2" -> AstDat(),
    "x" -> AstNum()
  )

  private val clientAttrs: AstObj = obj(
    "surname" -> AstStr(),
    "account" -> objs("sum" -> AstNum(), "actual" -> AstBool())
  )
  private val productAttrs: AstObj = obj(
    "productId" -> AstStr(),
    "sumLimits" -> obj(
      "minSum" -> AstNum(),
      "maxSum" -> AstNum(),
      "operation" -> objs(
        "actual" -> AstBool(),
        "sum" -> AstNum(),
        "description" -> AstStr()
      )
    ),
    "info" -> AstStr()
  )

  private def obj(entries: (String, AstType)*): AstObj = {
    AstObj(new util.HashMap[String, AstType](Map(entries: _*)))
  }

  private def objs(entries: (String, AstType)*): AstArr = {
    AstArr(AstObj(new util.HashMap[String, AstType](Map(entries: _*))))
  }

  private val complexInput =
    """
      |group (
      |
      |  assign(n, len(client.account)),
      |  assign(n, index(client.account[id:in.accountId])),
      |  assign(client.tmp.bizDate, businessDay()),
      |
      |  //Validation of data
      |  condition (
      |    case (
      |        client.tmp.interestRate < 0,
      |        group (
      |        error("Field name {INTEREST RATE}, can not be less than or equal to 0"),
      |      exit()
      |        )
      |      ),
      |    case(
      |        client.tmp.contraExpireDate < businessDay(),
      |        group (
      |        error("Validity of the contract contraExpireDate "),
      |    exit()
      |        )
      |      ),
      |/*  case(
      |        //Bank code validation
      |        group (
      |        error("incorrect bank code"),
      |    exit()
      |        )
      |      ),*/
      |  case(
      |        client.country != "156",
      |        group (
      |        error("Field name {COUNTRY}, must be 156"),
      |    exit()
      |        )
      |      ),
      |  case(
      |        client.tmp.loanAmt <=0,
      |        group (
      |        error("Field name {CREDIT_LIMIT}, must be greater than 0"),
      |    exit()
      |        )
      |      )
      |  ),
      |  //Validation of product attributes
      |  condition (
      |
      |    case (
      |        client.account[id:in.accountId].product.LoanMold != "M" & client.account[id:in.accountId].product.LoanMold != "S",
      |        group (
      |            error("1052, Invalid loan products. Not a non-revolving loan products"),
      |            exit()
      |        )
      |      ),
      |    case (
      |        client.account[id:in.accountId].product.LoanStatus!= "A",
      |        group (
      |            error("1039, The loan product inactive, you can not do this"),
      |            exit()
      |        )
      |      )
      |  ),
      |
      |  case(
      |      !isDefined(in.accountId),
      |      group(
      |            assign(in.accountId, generateId())
      |      )
      |  ),
      |
      |    //Date (yyMMddhhmmddSSS) +4 bit random number +3 external end of the serial number
      |    procedure(genRefnbr),
      |
      |    //Generates account
      |    procedure(createAccountInner),
      |
      |    //Generate Payment Transaction
      |    assign(generateTransaction_transactionAmount, client.tmp.loanAmt),
      |    assign(orderStatus, "N"),
      |    assign(loanUsage, "L"),
      |    procedure(generateTransaction),
      |
      |
      |    assign (client.account[id:in.accountId].isFreezed, false()),
      |
      |    //Native procedure call - Sending order to payment platform SMS
      |    procedure (sendPayment),
      |
      |    //After response is received from payment platform
      |    condition (
      |      case (client.account[id:in.accountId].transaction[id:in.transId].orderStatus = "E",
      |        group (
      |          assign (client.account[id:in.accountId][isAbnormal:true()].isAbnormal, true()),
      |          exit()
      |        )
      |      ),
      |      case (1 = 1,
      |        group (
      |          assign(client.account[id:in.accountId].isAbnormal, false()),
      |
      |          condition(
      |            case (client.account[id:in.accountId].transaction[id:in.transId].orderStatus = "S",
      |              procedure (generatePayPlan)
      |
      |            )
      |          )
      |        )
      |      )
      |    ),
      |
      |    //Native procedure call - Send SMS
      |    procedure (sendSMS)
      |)
    """.stripMargin

  private val complexClient: AstObj = obj(
    "client" -> obj(
      "country" -> AstStr(),
      "tmp" -> obj(
        "bizDate" -> AstDat(),
        "interestRate" -> AstNum(),
        "contraExpireDate" -> AstDat(),
        "loanAmt" -> AstNum()
      ),
      "account" -> objs(
        "id" -> AstStr(),
        "product" -> obj(
          "LoanMold" -> AstStr(),
          "LoanStatus" -> AstStr()
        ),
        "isFreezed" -> AstBool(),
        "isAbnormal" -> AstBool(),
        "transaction" -> objs(
          "id" -> AstStr(),
          "orderStatus" -> AstStr()
        )
      )
    ),
    "in" -> obj(
      "accountId" -> AstStr(),
      "transId" -> AstStr()
    ),
    "generateTransaction" -> obj( // TODO user variables type analysys
      "transactionAmount" -> AstNum()
    ),
    "orderStatus" -> AstStr(),
    "loanUsage" -> AstStr(),
    "n" -> AstNum()
  )

  @Test
  def complextTest {
    val sb: StringBuilder = new StringBuilder
    val formatter: Formatter = new Formatter(sb)
    val tr: DtLangTranslator = new DtLangTranslator(complexClient)
    tr.registerEntryPoint("main", complexInput)
    tr.registerEntryPoint("genRefnbr", "message(\"Ok\")")
    tr.registerEntryPoint("createAccountInner", "message(\"Ok\")")
    tr.registerEntryPoint("generateTransaction", "message(\"Ok\")")
    tr.registerEntryPoint("sendPayment", "message(\"Ok\")")
    tr.registerEntryPoint("generatePayPlan", "message(\"Ok\")")
    tr.registerEntryPoint("sendSMS", "message(\"Ok\")")
    tr.generate(formatter, "kz.greetgo.dt.gen")
    System.out.println(sb.toString)
  }


  @Test
  def scanAllPathesTest {
    val lexer = DtLang.lexer
    val syntax = DtLang.syntax(lexer)
    lexer.input(complexInput)

    val tr: DtLangTranslator = new DtLangTranslator(complexClient)
    val map: util.Map[List[String], util.Set[Set[String]]] = new util.HashMap
    tr.scanAllPathes(map, syntax.getRootNode.get)
    System.out.println(map)
  }

  @Test
  def wholeCompileTest: Unit = {
    val sb: StringBuilder = new StringBuilder
    val formatter: Formatter = new Formatter(sb)

    val tr: DtLangTranslator = new DtLangTranslator(obj(
      "obj" -> obj(
        "arr" -> objs(
          "s" -> AstStr(),
          "b" -> AstBool(),
          "n" -> AstNum()
        )
      )
    ))

    tr.registerEntryPoint("main",
      """
        |group(
        |  Object(xyz, Num(id)),
        |  procedure(sub),
        |  assign(obj.arr[s:"sub"].n, 1)
        |)
      """.stripMargin)
    tr.registerEntryPoint("sub", """assign(obj.arr[1].s, "sub")""")

    tr.generate(formatter, "kz.greetgo.dt.gen")
    System.out.println(sb.toString)
  }

  @Test
  def wholeRunTest: Unit = {
    //val r: DtRunnable = new DtExec();
    //val scope = r.exec("main", new util.HashMap[String, DtType](), null)
    //System.out.println(scope)
  }

  @Test
  def tmpCompileTest: Unit = {
    val tr: DtLangTranslator = new DtLangTranslator(obj(
      "obj" -> obj(
        "arr" -> objs(
          "s" -> AstStr(),
          "b" -> AstBool(),
          "n" -> AstNum()
        )
      )
    ))

    tr.registerEntryPoint("main",
      """
        |group(
        |  procedure(sub),
        |  assign(obj.arr[s:"sub"].n, 1)
        |)
      """.stripMargin)
    tr.registerEntryPoint("sub", """assign(obj.arr[1].s, "sub")""")

    val cls = tr.compile(Collections.emptyList())
    val e = cls.newInstance()
    val scope = e.exec("main", new util.HashMap[String, DtType](), null)
    System.out.println(scope)
    e.set("obj.arr.2.n", Num(BigDecimal(5)))
    System.out.println(e.scope())
    System.out.println(e.get("obj.arr.2.n"))
  }

  @Test
  def breakpoints() = {
    val tr: DtLangTranslator = new DtLangTranslator(obj(
      "client" -> obj(
        "accountX" -> objs(
          "id" -> AstNum(),
          "sum" -> AstNum()
        )
      )
    ))

    tr.registerEntryPoint("main",
      """group(
        |      assign(client.accountX[id:5].sum, 20),
        |      assign(client.accountX[id:6].sum, 22),
        |      assign(client.accountX[id:5].sum, 21),
        |      assign(client.accountX[id:6].sum, 23)
        |)
      """.stripMargin)

    val cls = tr.compile(Collections.emptyList())
    val e = cls.newInstance()
    val scope = e.exec("main", new util.HashMap[String, DtType](), Collections.singleton(40)) // 6 23 _40_ 57
    System.out.println(scope)
    val reached = e.reached()
    assert(reached.containsAll(util.Arrays.asList(6, 23, 40)))
    assert(!reached.contains(57))
  }


}
