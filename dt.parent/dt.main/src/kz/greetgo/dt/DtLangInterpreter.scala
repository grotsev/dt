package kz.greetgo.dt

import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.time.temporal.ChronoUnit
import java.time.{LocalDate, Period}
import java.util
import java.util.function.Consumer

import name.lakhin.eliah.projects.papacarlo.syntax.Node

import scala.collection.JavaConversions._

/**
  * Created by den on 11.07.16.
  */
class DtLangInterpreter(scope: util.SortedMap[String, DtType],
                        nativeProcedures: util.Map[String, Consumer[util.SortedMap[String, DtType]]],
                        userProcedures: util.Map[String, Node],
                        nativeFunctions: NativeFunctions,
                        breakpoints: util.List[Integer]) {
  val messages: util.List[String] = new util.ArrayList
  var procedureStack: List[String] = Nil
  var reached: util.Set[Integer] = new util.HashSet[Integer]()

  def eval(expr: Node): Unit = {
    try {
      evalExpr(expr)
    } catch {
      case e: BreakException =>
      case e: ContinueException =>
      case e: ExitException =>
      case e: StopException =>
      case e: BreakPointException =>
      //case e: ErrorException => error = e.msg
    }
  }

  private def assign(ref: String, value: Option[DtType]) = {
    if (value.isEmpty) {
      scope.put(ref, null)
    } else {
      scope.put(ref, value.get)
    }
    value
  }

  def foreach(ref: String, from: BigDecimal, to: Option[BigDecimal], body: Node): Option[DtType] = {
    var i = from
    try {
      while (to.isEmpty || i <= to.get) {
        assign(ref, Some(Num(i)))
        try {
          evalExpr(body)
        } catch {
          case e: ContinueException if e.label.isEmpty || e.label.get == ref => // skip
        }
        i = i + 1
      }
    } catch {
      case e: BreakException if e.label.isEmpty || e.label.get == ref => // skip
    }
    None // foreach is always None
  }

  private def toStr(v: DtType): Option[String] = {
    v match {
      case Str(v) => Some(v)
      case _ => None
    }
  }

  private def unaryOp(expr: Node, f: Function[Option[DtType], Option[DtType]]) = {
    val operand = evalExpr(expr.getBranches("operand").head)
    f(operand)
  }

  private def binaryOp(expr: Node, f: Function2[Option[DtType], Option[DtType], Option[DtType]]) = {
    val left = evalExpr(expr.getBranches("left").head)
    val right = evalExpr(expr.getBranches("right").head)
    f(left, right)
  }

  private def numOp(expr: Node, f: Function2[BigDecimal, BigDecimal, BigDecimal]) = {
    binaryOp(expr, (left, right) => (left, right) match {
      case (Some(Num(l)), Some(Num(r))) => Some(Num(f(l, r)))
      case _ => None
    })
  }

  private def toStr(value: Option[DtType]): String = value match {
    case None => ""
    case Some(Bool(v)) => v.toString
    case Some(Num(v)) => v.toString
    case Some(Str(v)) => v.toString
    case Some(Dat(v)) => v.toString
    case Some(Per(v)) => v.toString
  }

  private def compareOp(expr: Node, p: Int => Boolean): Option[DtType] = {
    binaryOp(expr, (left, right) => (left, right) match {
      case (Some(Bool(l)), Some(Bool(r))) => Some(Bool(p(l.compareTo(r))))
      case (Some(Num(l)), Some(Num(r))) => Some(Bool(p(l.compare(r))))
      case (Some(Str(l)), Some(Str(r))) => Some(Bool(p(l.compareTo(r))))
      case (Some(Dat(l)), Some(Dat(r))) => Some(Bool(p(l.compareTo(r))))
      case (Some(Num(l)), None) => Some(Bool(p(l.compare(0))))
      case (None, Some(Num(r))) => Some(Bool(p(BigDecimal(0).compare(r))))
      case (None, None) => Some(Bool(p(0)))
      case (None, _) => Some(Bool(p(-1)))
      case (_, None) => Some(Bool(p(1)))
      case (None, Some(Str(_))) => Some(Bool(p(-1)))
      case (Some(Str(_)), None) => Some(Bool(p(1)))
      case _ => None
    })
  }

  private def evalExpr(expr: Node): Option[DtType] = {
    if (breakpoints != null) {
      reached.add(expr.getId);
      if (breakpoints.contains(expr.getId)) throw new BreakPointException()
    }
    try expr.getKind match {
      case "expr" => evalExpr(expr.getBranches("result").head)

      case "+" =>
        if (expr.getBranches.contains("operand"))
          unaryOp(expr, {
            case x@Some(Num(_)) => x
            case x@Some(Per(_)) => x
            case x@Some(Bool(_)) => x
            case _ => None
          })
        else binaryOp(expr, (left, right) => (left, right) match {
          case (Some(Num(l)), Some(Num(r))) => Some(Num(l + r))
          case (Some(Dat(l)), Some(Per(r))) => Some(Dat(l plus r))
          case (Some(Per(l)), Some(Per(r))) => Some(Per(l plus r))
          case (Some(Per(l)), Some(Dat(r))) => Some(Dat(r plus l))
          case (l@Some(Num(_)), None) => l
          case (None, r@Some(Num(_))) => r
          case (None, None) => Some(Num(BigDecimal(0)))
          case _ => None
        })
      case "-" =>
        if (expr.getBranches.contains("operand"))
          unaryOp(expr, {
            case Some(Num(num)) => Some(Num(-num))
            case Some(Per(per)) => Some(Per(per.negated()))
            case Some(Bool(bool)) => Some(Bool(!bool))
            case _ => None
          })
        else binaryOp(expr, (left, right) => (left, right) match {
          case (Some(Num(l)), Some(Num(r))) => Some(Num(l - r))
          case (Some(Dat(l)), Some(Per(r))) => Some(Dat(l minus r))
          case (Some(Per(l)), Some(Per(r))) => Some(Per(l minus r))
          case (Some(Dat(l)), Some(Dat(r))) => Some(Per(l until r))
          case (l@Some(Num(_)), None) => l
          case (None, Some(Num(r))) => Some(Num(-r))
          case (None, None) => Some(Num(BigDecimal(0)))
          case _ => None
        })
      case "!" =>
        unaryOp(expr, {
          case Some(Bool(o)) => Some(Bool(!o))
          case None => Some(Bool(true))
          case _ => None
        })

      case "*" =>
        binaryOp(expr, (left, right) => (left, right) match {
          case (Some(Num(l)), Some(Num(r))) => Some(Num(l * r))
          case (Some(Num(num)), Some(Per(per))) => Some(Per(per multipliedBy num.toInt))
          case (Some(Per(per)), Some(Num(num))) => Some(Per(per multipliedBy num.toInt))
          case (Some(Num(_)), None) => Some(Num(BigDecimal(0)))
          case (None, Some(Num(_))) => Some(Num(BigDecimal(0)))
          case (None, None) => Some(Num(BigDecimal(0)))
          case _ => None
        })
      case "/" =>
        binaryOp(expr, (left, right) => (left, right) match {
          case (Some(Num(l)), None) => throw new ArithmeticException(location(expr) + ": Division by zero")
          case (Some(Num(l)), Some(Num(r))) => Some(Num(l / r))
          case _ => None
        })
      case "%" =>
        binaryOp(expr, (left, right) => (left, right) match {
          case (Some(Num(l)), None) => throw new ArithmeticException("Division by zero")
          case (Some(Num(l)), Some(Num(r))) => Some(Num(l % r))
          case _ => None
        })

      case "~" =>
        binaryOp(expr, (left, right) => Some(Str(toStr(left) + toStr(right))))

      case "<" =>
        compareOp(expr, _ < 0)
      case ">" =>
        compareOp(expr, _ > 0)
      case "<=" =>
        compareOp(expr, _ <= 0)
      case ">=" =>
        compareOp(expr, _ >= 0)

      case "=" =>
        compareOp(expr, _ == 0)
      case "!=" =>
        compareOp(expr, _ != 0)

      case "&" =>
        evalExpr(expr.getBranches("left").head) match {
          case x@Some(Bool(false)) => x
          case Some(Bool(_)) => evalExpr(expr.getBranches("right").head)
          case _ => {
            evalExpr(expr.getBranches("right").head) match {
              case x@Some(Bool(false)) => x
              case _ => None
            }
          }
        }
      case "|" =>
        evalExpr(expr.getBranches("left").head) match {
          case x@Some(Bool(true)) => x
          case Some(Bool(_)) => evalExpr(expr.getBranches("right").head)
          case _ => {
            evalExpr(expr.getBranches("right").head) match {
              case x@Some(Bool(true)) => x
              case _ => None
            }
          }
        }

      case "atom" =>
        if (expr.getValues.contains("num")) {
          Some(Num(BigDecimal(expr.getValues("num").head)))
        } else if (expr.getValues.contains("str")) {
          val s: String = expr.getValues("str").head
          Some(Str(s.substring(1, s.length - 1)))
        } else {
          val path = expr.getBranches("path").head
          if (expr.getBranches.contains("call"))
            evalFun(path.sourceCode, expr.getBranches("call").head.getBranches.getOrElse("expr", List()))
          else
            evalPath(path).flatMap(x => Option(scope get x)) // variable access
        }
    } catch {
      case e: BreakException => throw e
      case e: ContinueException => throw e
      case e: ExitException => throw e
      case e: StopException => throw e
      case e: BreakPointException => throw e
      case e: ErrorException => throw e
      case e: ExprException => throw e
      case e: Exception => throw new ExprException(e, location(expr) + ": " + e.getMessage)
    }
  }

  private def tryInt(x: String): Either[String, Int] = {
    try Right(x.toInt)
    catch {
      case e: NumberFormatException => Left(x)
    }
  }

  private def children(path: String): Seq[String] = {
    val subMap = scope.subMap(path + "." + "\u0000", path + ('.' + 1).toChar.toString)
    val split: String => String = _.substring(path.length + 1).split("\\.", 2)(0)
    subMap.keySet.map(split).toSeq.distinct.sortWith((a, b) => {
      (tryInt(a), tryInt(b)) match {
        // String < Int
        case (Right(x), Right(y)) => x < y
        case (Left(x), Right(y)) => true
        case (Right(x), Left(y)) => false
        case (Left(x), Left(y)) => x < y
      }
    })
  }

  private def evalFun(name: String, arg: List[Node]): Option[DtType] = {
    name match {
      case "empty" => None
      case "assign" => {
        val to = evalPath(arg(0).getBranches("result").head.getBranches("path").head, isAssign = true)
        if (to.isDefined) assign(to.get, evalExpr(arg(1))) else None
      }
      case "group" => {
        var last: Option[DtType] = None
        for (expr <- arg)
          last = evalExpr(expr)
        last
      }

      case "condition" => {
        for (expr <- arg)
          evalExpr(expr) match {
            case None => // skip
            case x => return x
          }
        None
      }
      case "case" => evalExpr(arg(0)) match {
        case Some(Bool(b)) if b => {
          for (expr <- arg.tail)
            evalExpr(expr)
          Some(Bool(true))
        }
        case _ => None
      }

      case "foreach" => {
        val ref = arg(0).sourceCode // just simple path with one segment
        evalExpr(arg(1)) match {
          case Some(Num(from)) =>
            arg.size match {
              case 3 => foreach(ref, from, None, arg(2))
              case 4 => evalExpr(arg(2)) match {
                case Some(Num(to)) => foreach(ref, from, Some(to), arg(3))
                case _ => None // to is not num
              }
              case _ => None // exprs size is not 3 or 4
            }
          case _ => None // from is not num
        }
      }
      case "break" => throw new BreakException(if (arg.size == 0) None else Some(arg(0).sourceCode))
      case "continue" => throw new ContinueException(if (arg.size == 0) None else Some(arg(0).sourceCode))

      case "procedure" => {
        val name = arg(0).sourceCode
        val nativeProcedure = nativeProcedures.get(name)
        if (nativeProcedure != null) {
          nativeProcedure.accept(scope)
          None
        }
        else {
          val node = userProcedures.get(name)
          if (node == null) throw new ErrorException(location(arg(0)) + ": No procedure " + name)
          try {
            procedureStack = name :: procedureStack
            val res = evalExpr(node)
            res
          } catch {
            case e: BreakException => None
            case e: ContinueException => None
            case e: ExitException => e.value
          } finally {
            procedureStack = procedureStack.tail
          }
        }
      }
      case "exit" => throw new ExitException(if (arg.size == 0) None else evalExpr(arg(0)))
      case "stop" => throw new StopException
      case "message" => {
        evalExpr(arg(0)) match {
          case Some(Str(str)) => messages.add(str)
          case _ =>
        }
        None
      }
      case "error" => throw new ErrorException(
        if (arg.size == 0) ""
        else evalExpr(arg(0)) match {
          case Some(Str(s)) => s
          case _ => throw new ExprException(null, location(arg(0)) + ": Invalid error message")
        })

      case "len" =>
        val path = evalPath(arg(0).getBranches("result")(0).getBranches("path")(0))
        Some(Num(if (path.isEmpty) 0
        else {
          val ch = children(path.get)
          if (ch.isEmpty) 0 else maxChild(ch) + 1
        }))
      case "index" =>
        val path = evalPath(arg(0).getBranches("result")(0).getBranches("path")(0), isPostAssign = false)
        val n: BigDecimal = if (path.isEmpty) -1 else BigDecimal(path.get.substring(path.get.lastIndexOf('.') + 1))
        Some(Num(n))
      case "subs" =>
        arg.size match {
          case 2 => (evalExpr(arg(0)), evalExpr(arg(1))) match {
            case (Some(Str(s)), Some(Num(from))) => Some(Str(s.substring(crop(from, s))))
            case _ => None
          }
          case 3 => (evalExpr(arg(0)), evalExpr(arg(1)), evalExpr(arg(2))) match {
            case (Some(Str(s)), Some(Num(from)), Some(Num(to))) => Some(Str(s.substring(crop(from, s), crop(to, s))))
            case _ => None
          }
          case _ => None
        }
      case "indexOf" =>
        arg.size match {
          case 2 => (evalExpr(arg(0)), evalExpr(arg(1))) match {
            case (Some(Str(s)), Some(Str(subStr))) => Some(Num(s.indexOf(subStr)))
            case _ => None
          }
          case 3 => (evalExpr(arg(0)), evalExpr(arg(1)), evalExpr(arg(2))) match {
            case (Some(Str(s)), Some(Str(subStr)), Some(Num(index))) => Some(Num(BigDecimal(s.indexOf(subStr, index.intValue()))))
            case _ => None
          }
          case _ => None
        }
      case "lastIndexOf" =>
        arg.size match {
          case 2 => (evalExpr(arg(0)), evalExpr(arg(1))) match {
            case (Some(Str(s)), Some(Str(subStr))) => Some(Num(s.lastIndexOf(subStr)))
            case _ => None
          }
          case 3 => (evalExpr(arg(0)), evalExpr(arg(1)), evalExpr(arg(2))) match {
            case (Some(Str(s)), Some(Str(subStr)), Some(Num(index))) => Some(Num(BigDecimal(s.lastIndexOf(subStr, index.intValue()))))
            case _ => None
          }
          case _ => None
        }
      case "startsWith" =>
        (evalExpr(arg(0)), evalExpr(arg(1))) match {
          case (Some(Str(s)), Some(Str(subStr))) => Some(Bool(s.startsWith(subStr)))
          case _ => None
        }
      case "endsWith" =>
        (evalExpr(arg(0)), evalExpr(arg(1))) match {
          case (Some(Str(s)), Some(Str(subStr))) => Some(Bool(s.endsWith(subStr)))
          case _ => None
        }

      case "min" => {
        val prefix = arg(0).sourceCode
        children(prefix).map(ch => Option(scope.get(prefix + "." + ch))).reduceOption(
          (left, right) => (left, right) match {
            case (Some(Num(l)), Some(Num(r))) => Some(Num(l.min(r)))
            case (Some(Bool(l)), Some(Bool(r))) => Some(Bool(l && r))
            case (Some(Str(l)), Some(Str(r))) => Some(Str(if (l <= r) l else r))
            case (Some(Dat(l)), Some(Dat(r))) => Some(Dat(if (l.compareTo(r) <= 0) l else r))
            //case (Some(Per(l)), Some(Per(r))) => None // depends on base date
            case _ => None
          }).flatten
      }
      case "max" => {
        val prefix = arg(0).sourceCode
        children(prefix).map(ch => Option(scope.get(prefix + "." + ch))).reduceOption(
          (left, right) => (left, right) match {
            case (Some(Num(l)), Some(Num(r))) => Some(Num(l.max(r)))
            case (Some(Bool(l)), Some(Bool(r))) => Some(Bool(l || r))
            case (Some(Str(l)), Some(Str(r))) => Some(Str(if (l >= r) l else r))
            case (Some(Dat(l)), Some(Dat(r))) => Some(Dat(if (l.compareTo(r) >= 0) l else r))
            //case (Some(Per(l)), Some(Per(r))) => None // depends on base date
            case _ => None
          }).flatten
      }
      case "round" => arg.size match {
        case 1 => evalExpr(arg(0)) match {
          case Some(Num(num)) => Some(Num(num.setScale(0, BigDecimal.RoundingMode.HALF_UP)))
          case _ => None
        }
        case 2 => (evalExpr(arg(0)), evalExpr(arg(1))) match {
          case (Some(Num(num0)), Some(Num(num1))) => Some(Num(num0.setScale(num1.toInt, BigDecimal.RoundingMode.HALF_UP)))
          case _ => None
        }
      }
      case "floor" => evalExpr(arg(0)) match {
        case Some(Num(num)) => Some(Num(num.setScale(0, BigDecimal.RoundingMode.FLOOR)))
        case _ => None
      }
      case "power" => (evalExpr(arg(0)), evalExpr(arg(1))) match {
        case (Some(Num(l)), Some(Num(r))) => Some(Num(BigDecimal(Math.pow(l.toDouble, r.toDouble))))
        case (Some(Num(_)), None) => Some(Num(BigDecimal(1)))
        case (None, Some(Num(_))) => Some(Num(BigDecimal(0)))
        case _ => None
      }
      case "log" => (evalExpr(arg(0)), evalExpr(arg(1))) match {
        case (Some(Num(base)), Some(Num(num))) => Some(Num(BigDecimal(Math.log(num.toDouble) / Math.log(base.toDouble))))
        case _ => None
      }

      case "true" => Some(Bool(true))
      case "false" => Some(Bool(false))
      case "if" => evalExpr(arg(0)) match {
        case Some(Bool(true)) => evalExpr(arg(1))
        case Some(Bool(false)) => evalExpr(arg(2))
        case _ => None
      }

      case "isEmpty" => evalExpr(arg(0)) match {
        case None => Some(Bool(true))
        case _ => Some(Bool(false))
      }
      case "isDefined" => evalExpr(arg(0)) match {
        case None => Some(Bool(false))
        case _ => Some(Bool(true))
      }

      case "date" => arg.size match {
        case 1 => evalExpr(arg(0)) match {
          case Some(Str(str)) => try Some(Dat(LocalDate.parse(str.substring(0, 10)))) catch {
            case e: DateTimeParseException => None
          }
          case _ => None
        }
        case 3 => (evalExpr(arg(0)), evalExpr(arg(1)), evalExpr(arg(2))) match {
          case (Some(Num(year)), Some(Num(month)), Some(Num(day))) => try Some(Dat(LocalDate.of(year.toInt, month.toInt, day.toInt))) catch {
            case e: java.time.DateTimeException => None
          }
          case _ => None
        }
      }
      case "day" => arg.size match {
        case 0 => Some(Per(Period.ofDays(1)))
        case 1 => evalExpr(arg(0)) match {
          case Some(Dat(dat)) => Some(Num(dat.getDayOfMonth))
          case _ => None
        }
      }
      case "month" => arg.size match {
        case 0 => Some(Per(Period.ofMonths(1)))
        case 1 => evalExpr(arg(0)) match {
          case Some(Dat(dat)) => Some(Num(dat.getMonthValue))
          case _ => None
        }
      }
      case "year" => arg.size match {
        case 0 => Some(Per(Period.ofYears(1)))
        case 1 => evalExpr(arg(0)) match {
          case Some(Dat(dat)) => Some(Num(dat.getYear))
          case _ => None
        }
      }
      case "daysBetween" => (evalExpr(arg(0)), evalExpr(arg(1))) match {
        case (Some(Dat(from)), Some(Dat(to))) => Some(Num(ChronoUnit.DAYS.between(from, to)))
        case _ => None
      }
      case "daysInMonth" => evalExpr(arg(0)) match {
        case Some(Dat(dat)) => Some(Num(dat.lengthOfMonth()))
        case _ => None
      }
      case "setDay" => (evalExpr(arg(0)), evalExpr(arg(1))) match {
        case (Some(Dat(dat)), Some(Num(day))) => try
          Some(Dat(dat.withDayOfMonth(day.intValue())))
        catch {
          case e: java.time.DateTimeException => None
        }
        case _ => None
      }
      case "format" => (evalExpr(arg(0)), evalExpr(arg(1))) match {
        case (Some(Dat(d)), Some(Str(f))) => try {
          Some(Str(d.format(DateTimeFormatter.ofPattern(f))))
        } catch {
          case e: java.time.DateTimeException => None
          case e: IllegalArgumentException => None
        }
        case _ => None
      }

      case "today" => Some(Dat(nativeFunctions.today))
      case "businessDay" => Some(Dat(nativeFunctions.businessDay))
      case "generateId" => Some(Str(nativeFunctions.generateId))
      case "nextNumber" => Some(Num(nativeFunctions.nextNumber))
      case "echo" => nativeFunctions.echo(evalExpr(arg(0)).orNull)
        None
      case "echoScope" => nativeFunctions.echoScope(scope)
        None
      case "dictInc" => (evalExpr(arg(0)), evalExpr(arg(1)), evalExpr(arg(2)), evalExpr(arg(3))) match {
        case (Some(Str(dictCode)), Some(Str(fieldCode)), Some(Str(rowCode)), Some(Num(incrementValue))) =>
          Some(Bool(nativeFunctions.dictInc(dictCode, fieldCode, rowCode, incrementValue)))
        case _ => None
      }
      case "dictDec" => (evalExpr(arg(0)), evalExpr(arg(1)), evalExpr(arg(2)), evalExpr(arg(3))) match {
        case (Some(Str(dictCode)), Some(Str(fieldCode)), Some(Str(rowCode)), Some(Num(incrementValue))) =>
          Some(Bool(nativeFunctions.dictInc(dictCode, fieldCode, rowCode, -incrementValue)))
        case _ => None
      }
      case "dictValueNumber" => (evalExpr(arg(0)), evalExpr(arg(1)), evalExpr(arg(2))) match {
        case (Some(Str(dictCode)), Some(Str(fieldCode)), Some(Str(rowCode))) => {
          val r = nativeFunctions.dictValueNumber(dictCode, fieldCode, rowCode)
          if (r == null) None else Some(Num(r))
        }
        case _ => None
      }
      case "dictValueText" => (evalExpr(arg(0)), evalExpr(arg(1)), evalExpr(arg(2))) match {
        case (Some(Str(dictCode)), Some(Str(fieldCode)), Some(Str(rowCode))) => {
          val r = nativeFunctions.dictValueText(dictCode, fieldCode, rowCode)
          if (r == null) None else Some(Str(r))
        }
        case _ => None
      }

      case "toNum" => try {
        evalExpr(arg(0)) match {
          case Some(Str(str)) => Some(Num(BigDecimal(str.replaceAll("[_ ]", "").replace(',', '.'))))
          case _ => None
        }
      }
      catch {
        case e: NumberFormatException => None
      }

      case _ => None
    }
  }

  private def crop(n: BigDecimal, s: String) = n.intValue.max(0).min(s.length)

  private def evalPath(path: Node, isPostAssign: Boolean = true, isAssign: Boolean = false): Option[String] = {
    val segs = path.getBranches("segment")
    segs.foldLeft(Some(""): Option[String]) {
      (acc: Option[String], seg: Node) => {
        if (acc.isEmpty) return acc
        val a = acc.get
        val name = seg.getValues("name").head
        val prefix: String = if (a.isEmpty) name else a + "." + name
        val indexes = seg.getBranches.getOrElse("index", List())
        if (indexes.isEmpty) Some(prefix)
        else {
          // indexes is not empty
          if (indexes(0).getValues.contains("field")) {
            // all indexes should be in form [field1: value1]...[fieldN: valueN]
            val chsVal: Seq[String] = children(prefix)
            var chs: Seq[String] = chsVal
            val toAssign = new util.HashMap[String, Option[DtType]]()
            for (index <- indexes) {
              val field: String = index.getValues("field").head // must contains "field"
              val filter: Option[DtType] = evalExpr(index.getBranches("filter").head)
              if (filter.isDefined) toAssign.put(field, filter)
              chs = chs.filter(ch => Option(scope.get(prefix + "." + ch + "." + field)) == filter)
            }
            if (chs.isEmpty) {
              if (prefix == "client.account") throw new ErrorException(location(path) + ": Can not create client.account")
              postAssign(if (chsVal.isEmpty) prefix + ".0" else prefix + "." + (maxChild(chsVal) + 1), toAssign, isPostAssign)
            }
            else Some(prefix + "." + chs(0))

          } else evalExpr(indexes(0).getBranches("filter").head) match {
            // just one index [num]
            case Some(Num(num)) => {
              val n = num.toInt
              if (n >= 0) Some(prefix + "." + n)
              else {
                val i = maxChild(children(prefix)) + n + 1
                if (i >= 0) Some(prefix + "." + i)
                else {
                  if (isAssign) throw new ArrayIndexOutOfBoundsException(location(path)) else None
                }
              }
            }
            case _ => throw new IllegalArgumentException(location(path) + ": Filter is not num.")
          }


        } // indexes is not empty
      }
    } // foldLeft

  }

  private def location(node: Node): String =
    node.getBegin + " " + procedureStack.toString().substring(4) + " '" + node.sourceCode + "'"

  private def postAssign(p: String, toAssign: util.HashMap[String, Option[DtType]], postAssign: Boolean): Option[String] = {
    if (postAssign) for (a <- toAssign.entrySet()) assign(p + "." + a.getKey, a.getValue)
    Some(p)
  }

  private def maxChild(seq: Seq[String]): Int = {
    seq.flatMap(str =>
      try Seq(Integer.parseInt(str))
      catch {
        case e: NumberFormatException => Seq()
      }
    ).max // throws if nonempty and no ints
  }

}

private class BreakException(val label: Option[String]) extends RuntimeException {
  override def fillInStackTrace(): Throwable = this
}

private class ContinueException(val label: Option[String]) extends RuntimeException {
  override def fillInStackTrace(): Throwable = this
}

private class ExitException(val value: Option[DtType]) extends RuntimeException {
  override def fillInStackTrace(): Throwable = this
}

private class StopException() extends RuntimeException {
  override def fillInStackTrace(): Throwable = this
}

class BreakPointException() extends RuntimeException {
  override def fillInStackTrace(): Throwable = this
}

abstract class InterpretationException(msg: String) extends RuntimeException(msg)

class ExprException(val cause: Throwable, val msg: String) extends InterpretationException(msg)

class ErrorException(val msg: String) extends InterpretationException(msg)