package kz.greetgo.dt

import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.time.temporal.ChronoUnit
import java.time.{LocalDate, Period}
import java.util
import java.util.function.Consumer

import name.lakhin.eliah.projects.papacarlo.syntax.Node

import scala.collection.JavaConversions._

/**
  * Second variant of DtLang interpreter
  * Created by den on 03.08.16.
  */
class DtLangInterpreter2(scope: Obj,
                         nativeProcedures: util.Map[String, Consumer[Obj]],
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

  private def evalExpr(expr: Node): Option[DtType] = {
    if (breakpoints != null) {
      reached.add(expr.getId)
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
          case (Some(Num(l)), None) => throw new ArithmeticException("Division by zero")
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
            access(Some(scope), path.getBranches("segment"), createAbsent = false)
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

  private def evalFun(name: String, arg: List[Node]): Option[DtType] = {
    name match {
      case "empty" => None
      case "assign" =>
        assign(Some(scope), arg.head.getBranches("result").head.getBranches("path").head.getBranches("segment"), evalExpr(arg(1)))
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
      case "case" => evalExpr(arg.head) match {
        case Some(Bool(b)) if b => {
          for (expr <- arg.tail)
            evalExpr(expr)
          Some(Bool(true))
        }
        case _ => None
      }

      case "foreach" => {
        val path = arg.head // just simple path with one segment
        evalExpr(arg(1)) match {
          case Some(Num(from)) =>
            arg.size match {
              case 3 => foreach(path, from, None, arg(2))
              case 4 => evalExpr(arg(2)) match {
                case Some(Num(to)) => foreach(path, from, Some(to), arg(3))
                case _ => None // to is not num
              }
              case _ => None // expressions size is not 3 or 4
            }
          case _ => None // from is not num
        }
      }
      case "break" => throw new BreakException(if (arg.isEmpty) None else Some(arg.head.sourceCode))
      case "continue" => throw new ContinueException(if (arg.isEmpty) None else Some(arg.head.sourceCode))

      case "procedure" => {
        val name = arg.head.sourceCode
        val nativeProcedure = nativeProcedures.get(name)
        if (nativeProcedure != null) {
          nativeProcedure.accept(scope)
          None
        }
        else {
          val node = userProcedures.get(name)
          if (node == null) throw new ErrorException(location(arg.head) + ": No procedure " + name)
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
      case "exit" => throw new ExitException(if (arg.isEmpty) None else evalExpr(arg.head))
      case "stop" => throw new StopException
      case "message" => {
        evalExpr(arg.head) match {
          case Some(Str(str)) => messages.add(str)
          case _ =>
        }
        None
      }
      case "error" => throw new ErrorException(
        if (arg.isEmpty) ""
        else evalExpr(arg.head) match {
          case Some(Str(s)) => s
          case _ => throw new ExprException(null, location(arg.head) + ": Invalid error message")
        })

      case "len" =>
        access(Some(scope), arg.head.getBranches("result").head.getBranches("path").head.getBranches("segment"), createAbsent = false) match {
          case Some(Arr(arr)) => Some(Num(arr.size()))
          case Some(_) => Some(Num(1))
          case _ => Some(Num(0))
        }
      case "index" =>
        val path = evalPath(Some(scope), arg.head.getBranches("result").head.getBranches("path").head.getBranches("segment"), createAbsent = false, "")
        path match {
          case Some(ArrRef(_, i)) => Some(Num(BigDecimal(i)))
          case x => Some(Num(-1))
        }
      case "subs" =>
        arg.size match {
          case 2 => (evalExpr(arg.head), evalExpr(arg(1))) match {
            case (Some(Str(s)), Some(Num(from))) => Some(Str(s.substring(crop(from, s))))
            case _ => None
          }
          case 3 => (evalExpr(arg.head), evalExpr(arg(1)), evalExpr(arg(2))) match {
            case (Some(Str(s)), Some(Num(from)), Some(Num(to))) => Some(Str(s.substring(crop(from, s), crop(to, s))))
            case _ => None
          }
          case _ => None
        }
      case "indexOf" =>
        arg.size match {
          case 2 => (evalExpr(arg.head), evalExpr(arg(1))) match {
            case (Some(Str(s)), Some(Str(subStr))) => Some(Num(s.indexOf(subStr)))
            case _ => None
          }
          case 3 => (evalExpr(arg.head), evalExpr(arg(1)), evalExpr(arg(2))) match {
            case (Some(Str(s)), Some(Str(subStr)), Some(Num(index))) => Some(Num(BigDecimal(s.indexOf(subStr, index.intValue()))))
            case _ => None
          }
          case _ => None
        }
      case "lastIndexOf" =>
        arg.size match {
          case 2 => (evalExpr(arg.head), evalExpr(arg(1))) match {
            case (Some(Str(s)), Some(Str(subStr))) => Some(Num(s.lastIndexOf(subStr)))
            case _ => None
          }
          case 3 => (evalExpr(arg.head), evalExpr(arg(1)), evalExpr(arg(2))) match {
            case (Some(Str(s)), Some(Str(subStr)), Some(Num(index))) => Some(Num(BigDecimal(s.lastIndexOf(subStr, index.intValue()))))
            case _ => None
          }
          case _ => None
        }
      case "startsWith" =>
        (evalExpr(arg.head), evalExpr(arg(1))) match {
          case (Some(Str(s)), Some(Str(subStr))) => Some(Bool(s.startsWith(subStr)))
          case _ => None
        }
      case "endsWith" =>
        (evalExpr(arg.head), evalExpr(arg(1))) match {
          case (Some(Str(s)), Some(Str(subStr))) => Some(Bool(s.endsWith(subStr)))
          case _ => None
        }

      case "min" =>
        access(Some(scope), arg.head.getBranches("result").head.getBranches("path").head.getBranches("segment"), createAbsent = false) match {
          case Some(Arr(arr)) => arr.filter(_ != null).reduceOption(
            (left, right) => (left, right) match {
              case (Num(l), Num(r)) => Num(l.min(r))
              case (Bool(l), Bool(r)) => Bool(l && r)
              case (Str(l), Str(r)) => Str(if (l <= r) l else r)
              case (Dat(l), Dat(r)) => Dat(if (l.compareTo(r) <= 0) l else r)
              //case (Some(Per(l)), Some(Per(r))) => None // depends on base date
            })
          case _ => throw new ErrorException("Not array min")
        }
      case "max" =>
        access(Some(scope), arg.head.getBranches("result").head.getBranches("path").head.getBranches("segment"), createAbsent = false) match {
          case Some(Arr(arr)) => arr.filter(_ != null).reduceOption(
            (left, right) => (left, right) match {
              case (Num(l), Num(r)) => Num(l.max(r))
              case (Bool(l), Bool(r)) => Bool(l || r)
              case (Str(l), Str(r)) => Str(if (l >= r) l else r)
              case (Dat(l), Dat(r)) => Dat(if (l.compareTo(r) >= 0) l else r)
              //case (Some(Per(l)), Some(Per(r))) => None // depends on base date
            })
          case _ => throw new ErrorException("Not array max")
        }
      case "round" => arg.size match {
        case 1 => evalExpr(arg.head) match {
          case Some(Num(num)) => Some(Num(num.setScale(0, BigDecimal.RoundingMode.HALF_UP)))
          case _ => None
        }
        case 2 => (evalExpr(arg.head), evalExpr(arg(1))) match {
          case (Some(Num(num0)), Some(Num(num1))) => Some(Num(num0.setScale(num1.toInt, BigDecimal.RoundingMode.HALF_UP)))
          case _ => None
        }
      }
      case "floor" => evalExpr(arg.head) match {
        case Some(Num(num)) => Some(Num(num.setScale(0, BigDecimal.RoundingMode.FLOOR)))
        case _ => None
      }
      case "power" => (evalExpr(arg.head), evalExpr(arg(1))) match {
        case (Some(Num(l)), Some(Num(r))) => Some(Num(BigDecimal(Math.pow(l.toDouble, r.toDouble))))
        case (Some(Num(_)), None) => Some(Num(BigDecimal(1)))
        case (None, Some(Num(_))) => Some(Num(BigDecimal(0)))
        case _ => None
      }
      case "log" => (evalExpr(arg.head), evalExpr(arg(1))) match {
        case (Some(Num(base)), Some(Num(num))) => Some(Num(BigDecimal(Math.log(num.toDouble) / Math.log(base.toDouble))))
        case _ => None
      }

      case "true" => Some(Bool(true))
      case "false" => Some(Bool(false))
      case "if" => evalExpr(arg.head) match {
        case Some(Bool(true)) => evalExpr(arg(1))
        case Some(Bool(false)) => evalExpr(arg(2))
        case _ => None
      }

      case "isEmpty" => evalExpr(arg.head) match {
        case None => Some(Bool(true))
        case _ => Some(Bool(false))
      }
      case "isDefined" => evalExpr(arg.head) match {
        case None => Some(Bool(false))
        case _ => Some(Bool(true))
      }

      case "date" => arg.size match {
        case 1 => evalExpr(arg.head) match {
          case Some(Str(str)) => try Some(Dat(LocalDate.parse(str.substring(0, 10)))) catch {
            case e: DateTimeParseException => None
          }
          case _ => None
        }
        case 3 => (evalExpr(arg.head), evalExpr(arg(1)), evalExpr(arg(2))) match {
          case (Some(Num(year)), Some(Num(month)), Some(Num(day))) => try Some(Dat(LocalDate.of(year.toInt, month.toInt, day.toInt))) catch {
            case e: java.time.DateTimeException => None
          }
          case _ => None
        }
      }
      case "day" => arg.size match {
        case 0 => Some(Per(Period.ofDays(1)))
        case 1 => evalExpr(arg.head) match {
          case Some(Dat(dat)) => Some(Num(dat.getDayOfMonth))
          case _ => None
        }
      }
      case "month" => arg.size match {
        case 0 => Some(Per(Period.ofMonths(1)))
        case 1 => evalExpr(arg.head) match {
          case Some(Dat(dat)) => Some(Num(dat.getMonthValue))
          case _ => None
        }
      }
      case "year" => arg.size match {
        case 0 => Some(Per(Period.ofYears(1)))
        case 1 => evalExpr(arg.head) match {
          case Some(Dat(dat)) => Some(Num(dat.getYear))
          case _ => None
        }
      }
      case "daysBetween" => (evalExpr(arg.head), evalExpr(arg(1))) match {
        case (Some(Dat(from)), Some(Dat(to))) => Some(Num(ChronoUnit.DAYS.between(from, to)))
        case _ => None
      }
      case "daysInMonth" => evalExpr(arg.head) match {
        case Some(Dat(dat)) => Some(Num(dat.lengthOfMonth()))
        case _ => None
      }
      case "setDay" => (evalExpr(arg.head), evalExpr(arg(1))) match {
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
        evalExpr(arg.head) match {
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

  private def unaryOp(expr: Node, f: Function[Option[DtType], Option[DtType]]) = {
    val operand = evalExpr(expr.getBranches("operand").head)
    f(operand)
  }

  private def binaryOp(expr: Node, f: (Option[DtType], Option[DtType]) => Option[DtType]): Option[DtType] = {
    val left = evalExpr(expr.getBranches("left").head)
    val right = evalExpr(expr.getBranches("right").head)
    f(left, right)
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
      case (None, Some(Str(_))) => Some(Bool(p(-1)))
      case (Some(Str(_)), None) => Some(Bool(p(1)))
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
    case _ => throw new ErrorException("Can not concatenate Array or Object")
  }

  private def location(node: Node): String =
    node.getBegin + " " + procedureStack.toString().substring(4) + " '" + node.sourceCode + "'"

  private def crop(n: BigDecimal, s: String) = n.intValue.max(0).min(s.length)

  private def foreach(path: Node, from: BigDecimal, to: Option[BigDecimal], body: Node): Option[DtType] = {
    val segments: List[Node] = path.getBranches("result").head.getBranches("path").head.getBranches("segment")
    val pathSrc = path.sourceCode
    var i = from
    try {
      while (to.isEmpty || i <= to.get) {
        assign(Some(scope), segments, Some(Num(i)))
        try {
          evalExpr(body)
        } catch {
          case e: ContinueException if e.label.isEmpty || e.label.get == pathSrc => // skip
        }
        i = i + 1
      }
    } catch {
      case e: BreakException if e.label.isEmpty || e.label.get == pathSrc => // skip
    }
    None // foreach is always None
  }

  private def access(base: Option[DtType], segments: List[Node], createAbsent: Boolean): Option[DtType] =
    evalPath(base, segments, createAbsent, "") match {
      case Some(ArrRef(Arr(arrRef), index)) => Option(DtManagerUtil.arrGet(arrRef, index))
      case Some(ObjRef(Obj(objRef), field)) => Option(objRef.get(field))
      case _ => if (createAbsent) throw new ErrorException("Assign to empty") else None
    }

  private def assign(base: Option[DtType], segments: List[Node], value: Option[DtType]): Option[DtType] = {
    evalPath(base, segments, createAbsent = true, "") match {
      case Some(ArrRef(Arr(arrRef), index)) => DtManagerUtil.arrPut(arrRef, index, value.orNull)
      case Some(ObjRef(Obj(objRef), field)) => objRef.put(field, value.orNull)
      case _ => throw new ErrorException("Assign to empty")
    }
    value
  }

  // ref

  private def evalSegment(base: Obj, segment: Node, createAbsent: Boolean, prefix: String): Option[DtRef] = {
    val name = segment.getValues("name").head
    val indexes = segment.getBranches.getOrElse("index", Nil)

    if (indexes isEmpty) Some(ObjRef(base, name))
    else base match {
      // array field in obj
      case Obj(obj) =>
        Option(obj.get(name)) match {
          case None => if (createAbsent) {
            val newArr = Arr(new java.util.ArrayList[DtType]())
            obj.put(name, newArr)
            doArr(newArr, indexes, createAbsent, prefix)
          } else None
          case Some(a@Arr(arr)) => doArr(a, indexes, createAbsent, prefix)
          case _ => throw new ErrorException("Not array access")
        }
    }
  }

  private def doArr(a: Arr, indexes: List[Node], createAbsent: Boolean, prefix: String): Option[ArrRef] =
    a match {
      case Arr(arr) =>
        if (indexes.head.getValues.contains("field")) {
          // field access
          val fields = evalIndexes(indexes)

          val foundIndex = arr.indexWhere(e => allFieldsMatch(Option(e), fields))
          if (foundIndex < 0) {
            if (createAbsent) {
              if (prefix == ".client.account") throw new ErrorException(location(indexes.head) + ": Can not create client.account")
              val i: Integer = arr.size()
              val newObj = new util.HashMap[String, DtType]
              fields.foreach(e => newObj.put(e._1, e._2.orNull))
              DtManagerUtil.arrPut(arr, i, Obj(newObj))
              Some(ArrRef(a, i))
            } else None
          } else {
            Some(ArrRef(a, foundIndex))
          }
        }
        else evalExpr(indexes.head.getBranches("filter").head) /* just one index [num] */ match {
          case Some(Num(num)) =>
            val index = num.intValue()
            val i: Integer = if (index >= 0) index else arr.size() + index
            if (createAbsent && i < 0) throw new ArrayIndexOutOfBoundsException(location(indexes.head))
            Option(DtManagerUtil.arrGet(arr, i)) match {
              case None => if (createAbsent) {
                DtManagerUtil.arrPut(arr, i, Obj(new util.HashMap[String, DtType]))
                Some(ArrRef(a, i))
              } else None
              case Some(_) => Some(ArrRef(a, i))
            }
          case _ => throw new ErrorException("Not number array access")
        }
    }

  private def allFieldsMatch(el: Option[DtType], fields: List[(String, Option[DtType])]): Boolean = {
    el match {
      case Some(Obj(obj)) =>
        for (field <- fields) {
          if (Option(obj.get(field._1)) != field._2) return false
        }
        true
      case _ => false
    }
  }

  private def evalIndexes(indexes: List[Node]): List[(String, Option[DtType])] =
    indexes.map(index => {
      val field: String = index.getValues("field").head // must contains "field"
      val filter: Option[DtType] = evalExpr(index.getBranches("filter").head)
      (field, filter)
    })

  private def evalPath(base: Option[DtType], segments: List[Node], createAbsent: Boolean, prefix: String): Option[DtRef] = {
    segments match {
      case segment :: Nil => // last segment
        base match {
          case None => None
          case Some(o@Obj(obj)) => evalSegment(o, segment, createAbsent, prefix + "." + segment.getValues("name").head)
          case _ => throw new ErrorException("Not object assign")
        }
      case segment :: segmentTail => base match {
        case None => None
        case Some(o@Obj(obj)) =>
          val p = prefix + "." + segment.getValues("name").head
          evalSegment(o, segment, createAbsent, p) match {
            case Some(ObjRef(matchedObject, matchedField)) => evalPath(Option(matchedObject.obj.get(matchedField))
              .orElse(if (createAbsent) {
                val v = Obj(new util.HashMap[String, DtType]())
                matchedObject.obj.put(matchedField, v)
                Some(v)
              } else None),
              segmentTail, createAbsent, p)
            case Some(ArrRef(arr, index)) =>
              evalPath(Option(DtManagerUtil.arrGet(arr.arr, index))
                .orElse(if (createAbsent) {
                  val v = Obj(new util.HashMap[String, DtType]())
                  DtManagerUtil.arrPut(arr.arr, index, v)
                  Some(v)
                } else None),
                segmentTail, createAbsent, p)
            case _ => if (createAbsent) throw new ErrorException("Not object access") else None
          }
        case _ => throw new ErrorException("Not object access")
      }
      case Nil => throw new ErrorException("Access to empty")
    }
  }

}
