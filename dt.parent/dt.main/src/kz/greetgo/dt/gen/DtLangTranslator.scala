package kz.greetgo.dt.gen

import java.io.{ByteArrayOutputStream, File}
import java.nio.file.{Files, Paths}
import java.time.{LocalDate, Period}
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util
import java.util.{Collections, Formatter}
import java.util.stream.Collectors

import kz.greetgo.dt.{BreakPointException, DtLang}
import kz.greetgo.java_compiler.char_sequence.CharSequenceCompiler
import kz.greetgo.util.ServerUtil
import name.lakhin.eliah.projects.papacarlo.{Lexer, Syntax}
import name.lakhin.eliah.projects.papacarlo.syntax.{Error, Node}

import scala.collection.JavaConversions._
import scala.util.Random

/**
  * Created by den on 03.10.16.
  */
class DtLangTranslator(astScope: AstObj, genSourceMap: Boolean = true, genBreakpoint: Boolean = true, genMetrics: Boolean = true) {
  private val trees: util.Map[String, Node] = new util.HashMap[String, Node]
  private val entryPoints: util.Set[String] = new util.HashSet[String]
  private val userProcedures: util.Set[String] = new util.HashSet[String]
  private var proc: String = ""

  private def mergeType(expr: Node, a: AstType, b: AstType): Unit = {
    (a, b) match {
      case (AstNum(), AstNum()) =>
      case (AstStr(), AstStr()) =>
      case (AstDat(), AstDat()) =>
      case (AstBool(), AstBool()) =>
      case (AstPer(), AstPer()) =>
      case (AstNull(), AstNull()) =>
      case (AstObj(aMap), AstObj(bMap)) =>
        for ((bName, bAst) <- bMap) {
          val aAst = aMap.get(bName)
          if (aAst != null) mergeType(expr, aAst, bAst)
          else aMap.put(bName, bAst)
        }
      case (AstArr(aArr), AstArr(bArr)) => mergeType(expr, aArr, bArr)
      case _ => error(expr, "Type mismatch " + a + " and " + b)
    }
  }

  def registerEntryPoint(treeName: String, code: String) {
    entryPoints.add(treeName)
    trees.put(treeName, parse(code))
  }

  def registerUserProcedure(treeName: String, code: String) {
    userProcedures.add(treeName)
    trees.put(treeName, parse(code))
  }

  private def parse(code: String): Node = {
    val lexer: Lexer = DtLang.lexer
    val syntax: Syntax = DtLang.syntax(lexer)
    lexer.input(code)
    val errors: util.List[Error] = new util.ArrayList[Error]
    errors.addAll(syntax.getErrors)
    if (!errors.isEmpty) throw new IllegalArgumentException("Syntax errors: " + errors)
    return syntax.getRootNode.get
  }

  def compile(options: java.lang.Iterable[String]): Class[DtRunnable] = {
    val pkg = Array("kz", "greetgo", "gcory", "dt" + System.nanoTime())

    val os = new ByteArrayOutputStream()
    val f = new Formatter(os)
    val packageName = String.join(".", pkg: _*)
    generate(f, packageName)
    f.close()

    val compiler = new CharSequenceCompiler(getClass.getClassLoader, options)
    val aClass = compiler.compile(packageName + ".DtExec", os.toString)
    aClass.asInstanceOf[Class[DtRunnable]]
  }

  def generate(f: Formatter, packageName: String): Unit = {
    f.format("package %s;\n\n", packageName)
    f.format("import java.util.*;\n")
    f.format("import java.util.function.Consumer;\n")
    f.format("import java.util.stream.Collectors;\n")
    f.format("import java.math.BigDecimal;\n")
    f.format("import java.time.LocalDate;\n")
    f.format("import java.time.Period;\n")
    f.format("import kz.greetgo.dt.*;\n")
    f.format("import kz.greetgo.dt.gen.*;\n")
    f.format("\n")

    f.format("public class DtExec implements DtRunnable {\n\n")

    f.format(
      """private final ArrayList<String> messages = new ArrayList<>();
        |private Set<Integer> breakpoints;
        |private BitSet reached;
        |private ArrayList<CallInstance> callStack = new ArrayList<>();
        |
        |@Override
        |public List<String> messages() {
        |  return messages;
        |}
        |
        |@Override
        |public Set<Integer> reached() {
        |  return reached == null ? null : reached.stream().boxed().collect(Collectors.toSet());
        |}
        |
        |private Map<String, Consumer<ScopeAccess>> nativeProcedures = new HashMap<>();
        |
        |private void callNativeProcedure(String name) {
        |  Consumer<ScopeAccess> nativeProcedure = nativeProcedures.get(name);
        |  if (nativeProcedure != null) nativeProcedure.accept(this); else throw new ErrorException("No procedure " + name);
        |}
        |
        |@Override
        |public void registerNativeProcedure(String name, Consumer<ScopeAccess> body) {
        |  nativeProcedures.put(name, body);
        |}
        |
        |@Override
        |public void setNativeProcedures(Map<String, Consumer<ScopeAccess>> nativeProcedures) {
        |  this.nativeProcedures = nativeProcedures;
        |}
        |
        |@Override
        |public void setNativeFunctions(NativeFunctions nativeFunctions) {
        |  this.nativeFunctions = nativeFunctions;
        |}
        |
        |@Override
        |public DtType get(String path) {
        |  return scope.get(Arrays.asList(path.split("\\.")));
        |}
        |
        |@Override
        |public void set(String path, DtType value) {
        |  scope.set(Arrays.asList(path.split("\\.")), value);
        |}
        |
        |@Override
        |public Map<String, DtType> scope() {
        |  Map<String, DtType> s = new HashMap<>();
        |  scope.fill(s, "");
        |  return s;
        |}
        |
        |@Override
        |public Set<String> allPaths() {
        |  return scope().keySet();
        |}
        |
        |private static class StopException extends RuntimeException {}
        |
        |private static void stop() {
        |  throw new StopException();
        |}
        |
        |private static class ExecMetrics {
        |  long prepareScope;
        |  long execute;
        |  long saveScope;
        |
        |  long count;
        |
        |  public void clean() {
        |    prepareScope = execute = saveScope = count = 0;
        |  }
        |}
        |
        |private final ExecMetrics execMetrics = new ExecMetrics();
        |
        |@Override
        |public void cleanExecMetrics() {
        |  execMetrics.clean();
        |}
        |
        |@Override
        |public Map<String, Long> getExecMetrics() {
        |    Map<String, Long> ret = new HashMap<>();
        |    ret.put("prepareScope", execMetrics.prepareScope);
        |    ret.put("execute", execMetrics.execute);
        |    ret.put("saveScope", execMetrics.saveScope);
        |    ret.put("count", execMetrics.count);
        |    return ret;
        |}
        |
        |@Override
        |public Map<String, DtType> exec(String treeName, Map<String, DtType> extScope, Set<Integer> breakpoints) {
        |  long time1 = 0, time2 = 0, time3 = 0, time4 = 0;
        |
        |  this.breakpoints = breakpoints;
        |  reached = breakpoints == null ? null : new BitSet(8192);
        |
        |  Map<String, DtType> resultScope = new HashMap<>();
        |  try {
        |    %2$s
        |    for (Map.Entry<String, DtType> e : extScope.entrySet()) {
        |      List<String> path = Arrays.asList(e.getKey().split("\\."));
        |      scope.set(path, e.getValue());
        |    }
        |    %3$s
        |    switch (treeName) {
        |      %1$s
        |      default: throw new IllegalArgumentException("No tree with name " + treeName);
        |    }
        |  } catch (BreakPointException e) {
        |  } catch (StopException e) {
        |  } catch (ErrorException e) {
        |    throw new ErrorException(e.getMessage());
        |  //} catch (Exception e) {
        |  //  throw new ExprException(e, callStack.toString() + " " + e);
        |  }
        |  %4$s
        |  scope.fill(resultScope, "");
        |  %5$s
        |  %6$s
        |  return resultScope;
        |}
        | """.stripMargin,
      trees.keySet().map(name => "      case \"" + name + "\": " + name + "(); break;").mkString("\n"),
      if (genMetrics) "time1 = System.nanoTime();" else "",
      if (genMetrics) "time2 = System.nanoTime();" else "",
      if (genMetrics) "time3 = System.nanoTime();" else "",
      if (genMetrics) "time4 = System.nanoTime();" else "",
      if (genMetrics)
        """  execMetrics.prepareScope += time2 - time1;
          |  execMetrics.execute += time3 - time2;
          |  execMetrics.saveScope += time4 - time3;
          |  execMetrics.count++;
        """.stripMargin
      else ""
    )

    val map: util.Map[List[String], util.Set[Set[String]]] = new util.HashMap;
    for (tree <- trees.values()) {
      scanTypes(tree)
      scanAllPathes(map, tree)
    }
    // Additional pass to inference assignment type
    for (tree <- entryPoints) {
      proc = tree
      translate(trees.get(tree))
    }
    for (tree <- userProcedures) {
      proc = tree
      translate(trees.get(tree))
    }
    f.format("public static final class Scope {\n")
    genAttrs(map, f, "  ", List(), astScope.obj)
    f.format("}\n\n")

    f.format("  public Scope scope = new Scope();\n\n")
    f.format("  public NativeFunctions nativeFunctions;\n\n")

    for (tree <- trees.entrySet()) {
      proc = tree.getKey
      f.format(
        """public void %1$s() {
          |  final CallInstance call = new CallInstance("%1$s");
          |  callStack.add(call);
          |  %2$s
          |  callStack.remove(callStack.size()-1);
          |}
          | """.stripMargin, tree.getKey, translate(tree.getValue)._1)
    }

    f.format("}\n")
  }

  //private val objs = Set(AttributeType.OBJECT, AttributeType.OBJECTS)

  private def isComplex(a: AstType): Boolean = a match {
    case AstObj(_) => true
    case AstArr(_) => true
    case _ => false
  }

  private def genAttrs(map: util.Map[List[String], util.Set[Set[String]]], f: Formatter, indent: String, parent: List[String], attrs: util.Map[String, AstType]): Unit = {
    for (attr <- attrs) {
      val fieldTyp = fieldType(attr)
      if (isComplex(attr._2)) {
        val parentAndThis = parent :+ attr._1
        f.format(indent)
        f.format("public static final class %s {\n", attr._1)
        val children: util.Map[String, AstType] = attr._2 match {
          case AstObj(obj) => obj
          case AstArr(AstObj(obj)) => obj
        }
        genAttrs(map, f, indent + "  ", parentAndThis, children)
        f.format(indent)
        f.format("}\n")

        f.format(indent)
        f.format("public final %1$s %2$s = new %1$s();\n", fieldTyp, attr._1)

        attr._2 match {
          case AstArr(AstObj(children)) => {
            genIndexFilter(f, indent, attr._1, parentAndThis)
            val typeMap: Map[String, String] = children.filter(attr => !isComplex(attr._2)).map(attr => attr._1 -> fieldType(attr))(collection.breakOut)
            if (!typeMap.isEmpty)
              for (set <- map.getOrDefault(parentAndThis, Collections.emptySet()))
                genFilter(map, f, indent, attr._1, set, typeMap, parentAndThis)
          }
          case _ =>
        }
      } else {
        f.format(indent)
        f.format("public %s %s;\n", fieldTyp, attr._1)
      }
    }
    genGet(f, indent, attrs, parent)
    genSet(f, indent, attrs, parent)
    genFill(f, indent, attrs)
  }

  private def genGet(f: Formatter, indent: String, attrs: util.Map[String, AstType], parent: List[String]): Unit = {
    f.format(indent)
    f.format("public DtType get(List<String> path) {\n")
    f.format(indent)
    f.format("  if (path.isEmpty()) throw new ErrorException(\"get from empty path\");\n")
    f.format(indent)
    f.format("  switch (path.get(0)) {\n")
    for (attr <- attrs) {
      f.format(indent)
      attr._2 match {
        case AstDat() =>
          f.format("    case \"%1$s\": return this.%1$s == null ? null : new Dat(this.%1$s);\n", attr._1)
        case AstNum() =>
          f.format("    case \"%1$s\": return this.%1$s == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.%1$s));\n", attr._1)
        case AstStr() =>
          f.format("    case \"%1$s\": return this.%1$s == null ? null : new Str(this.%1$s);\n", attr._1)
        case AstBool() =>
          f.format("    case \"%1$s\": return this.%1$s == null ? null : new Bool(this.%1$s);\n", attr._1)
        case AstObj(_) =>
          f.format("    case \"%1$s\": return this.%1$s.get(path.subList(1, path.size()));\n", attr._1)
        case AstArr(_) =>
          f.format("    case \"%1$s\": { int index = Integer.parseInt(path.get(1)); " +
            "if (index < 0 || index >= this.%1$s.size() || this.%1$s.get(index) == null) return null; " +
            "return this.%1$s.get(index).get(path.subList(2, path.size()));}\n", attr._1)
        //case AttributeType.NUMS =>
        //case AttributeType.STRS =>
        //case AttributeType.DATES =>
        case _ =>
          throw new IllegalArgumentException("(Get) Undefined field type " + attr)
      }
    }
    f.format(indent)
    f.format("    default: throw new ErrorException(\"(Get " + parent + ") Undefined field \"+path.get(0));\n")
    f.format(indent)
    f.format("  }\n")
    f.format(indent)
    f.format("}\n")
  }

  private def genSet(f: Formatter, indent: String, attrs: util.Map[String, AstType], parent: List[String]): Unit = {
    f.format(indent)
    f.format("public void set(List<String> path, DtType value) {\n")
    f.format(indent)
    f.format("  if (path.isEmpty()) throw new ErrorException(\"set to empty path\");\n")
    f.format(indent)
    f.format("  switch (path.get(0)) {\n")
    for (attr <- attrs) {
      f.format(indent)
      attr._2 match {
        case AstDat() =>
          f.format("    case \"%1$s\": this.%1$s = value==null ? null : ((Dat)value).dat(); break;\n", attr._1)
        case AstNum() =>
          f.format("    case \"%1$s\": this.%1$s = value==null ? null : ((Num)value).num().underlying(); break;\n", attr._1)
        case AstStr() =>
          f.format("    case \"%1$s\": this.%1$s = value==null ? null : ((Str)value).str(); break;\n", attr._1)
        case AstBool() =>
          f.format("    case \"%1$s\": this.%1$s = value==null ? null : ((Bool)value).bool(); break;\n", attr._1)
        case AstObj(_) =>
          f.format("    case \"%1$s\": this.%1$s.set(path.subList(1, path.size()), value); break;\n", attr._1)
        case AstArr(_) => {
          f.format("    case \"%1$s\": { int index = Integer.parseInt(path.get(1)); U.ensureSize(this.%1$s, index + 1); " +
            "if (this.%1$s.get(index) == null) this.%1$s.set(index, new %1$s()); this.%1$s.get(index).set(path.subList(2, path.size()), value); break; }\n", attr._1)
        }
        //case AttributeType.NUMS =>
        //case AttributeType.STRS =>
        //case AttributeType.DATES =>
        case _ =>
          throw new IllegalArgumentException("(Set) Undefined field type " + attr)
      }
    }
    f.format(indent)
    f.format("    default: throw new ErrorException(\"(Set " + parent + ") Undefined field \"+path.get(0));\n")
    f.format(indent)
    f.format("  }\n")
    f.format(indent)
    f.format("}\n")
  }

  private def genFill(f: Formatter, indent: String, attrs: util.Map[String, AstType]): Unit = {
    f.format(indent)
    f.format("public void fill(Map<String, DtType> scope, String prefix) {\n")
    for (attr <- attrs) {
      f.format(indent)
      attr._2 match {
        case AstDat() =>
          f.format("  if (this.%1$s != null) scope.put((prefix+\".%1$s\").substring(1), new Dat(this.%1$s));\n", attr._1)
        case AstNum() =>
          f.format("  if (this.%1$s != null) scope.put((prefix+\".%1$s\").substring(1), new Num(new scala.math.BigDecimal(this.%1$s)));\n", attr._1)
        case AstStr() =>
          f.format("  if (this.%1$s != null) scope.put((prefix+\".%1$s\").substring(1), new Str(this.%1$s));\n", attr._1)
        case AstBool() =>
          f.format("  if (this.%1$s != null) scope.put((prefix+\".%1$s\").substring(1), new Bool(this.%1$s));\n", attr._1)
        case AstObj(_) =>
          f.format("  if (this.%1$s != null) this.%1$s.fill(scope, prefix+\".%1$s\");\n", attr._1)
        case AstArr(_) => {
          f.format("  for (int i = 0; i < this.%1$s.size(); i++) if (this.%1$s.get(i) != null) this.%1$s.get(i).fill(scope, prefix+\".%1$s\"+\".\"+i);\n", attr._1)
        }
        //case AttributeType.NUMS =>
        //case AttributeType.STRS =>
        //case AttributeType.DATES =>
        case _ =>
          throw new IllegalArgumentException("Undefined field type " + attr)
      }
    }
    f.format(indent)
    f.format("}\n")
  }

  private def genFilter(map: util.Map[List[String], util.Set[Set[String]]], f: Formatter, indent: String, attr: String, fields: Set[String], typeMap: Map[String, String], parentAndThis: List[String]): Unit = {
    // filter
    f.format(indent)
    f.format("public final %1$s %1$s_%2$s(boolean save, %3$s) {\n", attr,
      fields.mkString("_"),
      fields.map(x => typeMap(x) + " " + x).mkString(", "))
    f.format(indent)
    f.format("  if (%1$s) for(%3$s x : %3$s) if (x != null && %2$s) return x;\n",
      fields.map(_ + " != null").mkString(" && "),
      fields.map(x => x + ".equals(x." + x + ")").mkString(" && "),
      attr)
    f.format(indent)
    f.format("  %1$s x = new %1$s(); %2$s;\n",
      attr,
      fields.map(x => "x." + x + " = " + x).mkString("; "))
    f.format(indent)
    if (parentAndThis == List("client", "account")) f.format("  if (save) throw new ErrorException(\"Can not create client.account\");\n")
    else f.format("  if (save) this.%1$s.add(x);\n", attr)
    f.format(indent)
    f.format("  return x;\n")
    f.format(indent)
    f.format("}\n")

    // fields index
    f.format(indent)
    f.format("public final BigDecimal %1$s_%2$s(%3$s) {\n", attr,
      fields.mkString("_"),
      fields.map(x => typeMap(x) + " " + x).mkString(", "))
    f.format(indent)
    f.format("  int i = 0; if (%1$s) for(%3$s x : this.%3$s) {if (x != null && %2$s) return new BigDecimal(i); i++;}\n",
      fields.map(_ + " != null").mkString(" && "),
      fields.map(x => x + ".equals(x." + x + ")").mkString(" && "),
      attr)
    f.format(indent)
    f.format("  return new BigDecimal(-1);\n")
    f.format(indent)
    f.format("}\n")
  }

  private def genIndexFilter(f: Formatter, indent: String, attr: String, parentAndThis: List[String]): Unit = {
    f.format(indent)
    f.format("public final %1$s %1$s(boolean save, BigDecimal num) {\n", attr)
    f.format(indent)
    f.format(" if (num != null) {int n = num.intValue(); int s = this.%1$s.size(); if (-s <= n && n < s) return this.%1$s.get(n >= 0 ? n : n + s);}\n", attr)
    f.format(indent)
    f.format("  %1$s x = new %1$s();\n", attr)
    f.format(indent)
    if (parentAndThis == List("client", "account")) f.format("  if (save && num != null) throw new ErrorException(\"Can not create client.account\");\n")
    else f.format("  if (save && num != null) {int n = num.intValue(); if (n >= 0) {U.ensureSize(this.%1$s, n + 1); this.%1$s.set(n, x);} else throw new ArrayIndexOutOfBoundsException(n);}\n", attr)
    f.format(indent)
    f.format("  return x;\n")
    f.format(indent)
    f.format("}\n")
  }

  private def fieldType(attr: (String, AstType)): String = {
    attr._2 match {
      case AstDat() =>
        "LocalDate"
      case AstNum() =>
        "BigDecimal"
      case AstStr() =>
        "String"
      case AstBool() =>
        "Boolean"
      case AstObj(_) =>
        attr._1
      case AstArr(AstObj(_)) =>
        "ArrayList<" + attr._1 + ">"
      //      case AttributeType.NUMS =>
      //        "ArrayList<BigDecimal>"
      //      case AttributeType.STRS =>
      //        "ArrayList<String>"
      //      case AttributeType.DATES =>
      //        "ArrayList<LocalDate>"
      case _ =>
        throw new IllegalArgumentException("Undefined field type " + attr)
    }
  }

  private def unaryOp(expr: Node, f: Function[(String, AstType), (String, AstType)]) = {
    val operand = translate(expr.getBranches("operand").head)
    f(operand)
  }

  private def binaryOp(expr: Node, f: Function2[(String, AstType), (String, AstType), (String, AstType)]) = {
    val left = translate(expr.getBranches("left").head)
    val right = translate(expr.getBranches("right").head)
    f(left, right)
  }

  private def compareOp(expr: Node, op: String): (String, AstType) = {
    binaryOp(expr, (left, right) => (left, right) match {
      case ((l, AstNum()), (r, AstNum())) => ("U.cmp(" + l + ", " + r + ") " + op + " 0", AstBool())
      case ((l, AstStr()), (r, AstStr())) => ("U.cmp(" + l + ", " + r + ") " + op + " 0", AstBool())
      case ((l, AstDat()), (r, AstDat())) => ("U.cmp(" + l + ", " + r + ") " + op + " 0", AstBool())
      case ((l, AstBool()), (r, AstBool())) => ("U.cmp(" + l + ", " + r + ") " + op + " 0", AstBool())
      case ((_, lt), (_, rt)) => error(expr, "Incompatible types comparison " + lt + " and " + rt)
    })
  }

  private def error(expr: Node, l: (String, AstType), r: (String, AstType)): Nothing = {
    error(expr, "Type mismatch in left " + l._2 + " and right " + r._2 + " arguments");
  }

  private def error(expr: Node, l: (String, AstType)): Nothing = {
    error(expr, "Type mismatch " + l._2);
  }

  private def error(expr: Node, name: String): Nothing = {
    throw new IllegalArgumentException("(" + proc + ") " + expr.getBegin.toString + ": " + expr.sourceCode + " " + name);
  }

  private val TYPES: Set[String] = Set("Object", "Objects", "Num", "Dat", "Str", "Bool")

  private def scanTypes(expr: Node): Unit = {
    expr.getKind match {
      case "expr" => scanTypes(expr.getBranches("result").head)
      case "atom" => if (!expr.getValues.contains("num") && !expr.getValues.contains("str")) {
        val path = expr.getBranches("path").head
        if (expr.getBranches.contains("call")) {
          if (TYPES contains path.sourceCode) mergeType(expr, astScope, AstObj(scanObj(new util.HashMap[String, AstType], expr)))
          else expr.getBranches("call").head.getBranches.getOrElse("expr", List()).foreach(scanTypes(_))
        }
      }
      case _ =>
        if (expr.getBranches.contains("operand")) expr.getBranches("operand").foreach(scanTypes(_))
        else {
          expr.getBranches("left").foreach(scanTypes(_))
          expr.getBranches("right").foreach(scanTypes(_))
        }
    }
  }

  private def scanObj(map: util.HashMap[String, AstType], expr: Node): util.HashMap[String, AstType] = {
    expr.getKind match {
      case "expr" => scanObj(map, expr.getBranches("result").head)
      case "atom" => {
        if (expr.getValues.contains("num") || expr.getValues.contains("str")) error(expr, "Type definition must not contain numbers or strings")

        val path = expr.getBranches("path").head
        if (!expr.getBranches.contains("call")) error(expr, "Type must be defined in call form, i.e. type()")
        val arg = expr.getBranches("call").head.getBranches.getOrElse("expr", List())
        map.put(arg(0).sourceCode,
          path.sourceCode match {
            case "Object" => AstObj {
              val subMap = new util.HashMap[String, AstType]
              arg.tail.foreach(scanObj(subMap, _))
              subMap
            }
            case "Objects" => AstArr(AstObj {
              val subMap = new util.HashMap[String, AstType]
              arg.tail.foreach(scanObj(subMap, _))
              subMap
            })
            case "Num" => AstNum()
            case "Dat" => AstDat()
            case "Str" => AstStr()
            case "Bool" => AstBool()
          }
        )
        map
      }
      case _ => error(expr, "Operations are prohibited in type definition")
    }
  }

  def scanAllPathes(map: util.Map[List[String], util.Set[Set[String]]], expr: Node): Unit = {
    expr.getKind match {
      case "expr" => scanAllPathes(map, expr.getBranches("result").head)
      case "atom" => if (!expr.getValues.contains("num") && !expr.getValues.contains("str")) {
        val path = expr.getBranches("path").head
        if (expr.getBranches.contains("call")) {
          if (path.sourceCode != "procedure") expr.getBranches("call").head.getBranches.getOrElse("expr", List()).foreach(scanAllPathes(map, _))
        }
        else scanFilter(path).foreach[Unit](p => {
          map.getOrElseUpdate(p._1, new util.HashSet()).add(p._2)
        })
      }
      case _ =>
        if (expr.getBranches.contains("operand")) expr.getBranches("operand").foreach(scanAllPathes(map, _))
        else {
          expr.getBranches("left").foreach(scanAllPathes(map, _))
          expr.getBranches("right").foreach(scanAllPathes(map, _))
        }
    }
  }

  private def scanFilter(path: Node): List[(List[String], Set[String])] = {
    val segs = path.getBranches("segment")

    segs.map(seg => {
      val name = seg.getValues("name").head
      val indexes = seg.getBranches.getOrElse("index", List())
      val set: Set[String] = if (indexes.isEmpty || !indexes(0).getValues.contains("field")) Set()
      else Set(indexes.map(_.getValues("field").head): _*)
      (name, set)
    }).inits.toList.init.filter(!_.last._2.isEmpty).map(p => {
      val pair = p.unzip
      (pair._1, pair._2.last)
    }).toList
  }

  def translate(expr: Node): (String, AstType) = {
    expr.getKind match {
      case "expr" => translate(expr.getBranches("result").head)

      case "+" =>
        if (expr.getBranches.contains("operand"))
          translate(expr.getBranches("operand").head)
        else binaryOp(expr, (left, right) => (left, right) match {
          case ((l, AstNum()), (r, AstNum())) => ("U.plus(" + l + ", " + r + ")", AstNum())
          case ((l, AstDat()), (r, AstPer())) => ("U.plus(" + l + ", " + r + ")", AstDat())
          case ((l, AstPer()), (r, AstPer())) => ("U.plus(" + l + ", " + r + ")", AstPer())
          case ((l, AstPer()), (r, AstDat())) => ("U.plus(" + r + ", " + l + ")", AstDat())
          case (l, r) => error(expr, l, r)
        })
      case "-" =>
        if (expr.getBranches.contains("operand"))
          unaryOp(expr, {
            case (l, AstNum()) => ("U.minus(" + l + ")", AstNum())
            case (l, AstPer()) => ("U.minus(" + l + ")", AstPer())
            case (l, AstBool()) => ("U.minus(" + l + ")", AstBool())
            case l => error(expr, l)
          })
        else binaryOp(expr, (left, right) => (left, right) match {
          case ((l, AstNum()), (r, AstNum())) => ("U.minus(" + l + ", " + r + ")", AstNum())
          case ((l, AstDat()), (r, AstPer())) => ("U.minus(" + l + ", " + r + ")", AstDat())
          case ((l, AstPer()), (r, AstPer())) => ("U.minus(" + l + ", " + r + ")", AstPer())
          case ((l, AstDat()), (r, AstDat())) => ("U.minus(" + l + ", " + r + ")", AstPer())
          case (l, r) => error(expr, l, r)
        })
      case "!" =>
        unaryOp(expr, {
          case (l, AstBool()) => ("U.minus(" + l + ")", AstBool())
          case l => error(expr, l)
        })

      case "*" =>
        binaryOp(expr, (left, right) => (left, right) match {
          case ((l, AstNum()), (r, AstNum())) => ("U.mult(" + l + ", " + r + ")", AstNum())
          case ((l, AstNum()), (r, AstPer())) => ("U.mult(" + r + ", " + l + ")", AstPer())
          case ((l, AstPer()), (r, AstNum())) => ("U.mult(" + l + ", " + r + ")", AstPer())
          case (l, r) => error(expr, l, r)
        })
      case "/" =>
        binaryOp(expr, (left, right) => (left, right) match {
          case ((l, AstNum()), (r, AstNum())) => ("U.div(" + l + ", " + r + ")", AstNum())
          case (l, r) => error(expr, l, r)
        })
      case "%" =>
        binaryOp(expr, (left, right) => (left, right) match {
          case ((l, AstNum()), (r, AstNum())) => ("U.rem(" + l + ", " + r + ")", AstNum())
          case (l, r) => error(expr, l, r)
        })

      case "~" =>
        binaryOp(expr, (left, right) => (left, right) match {
          case ((l, _: AstType), (r, _: AstType)) => ("U.toStr(" + l + ") + U.toStr(" + r + ")", AstStr())
        }
        )

      case "<" =>
        compareOp(expr, "<")
      case ">" =>
        compareOp(expr, ">")
      case "<=" =>
        compareOp(expr, "<=")
      case ">=" =>
        compareOp(expr, ">=")

      case "=" =>
        compareOp(expr, "==")
      case "!=" =>
        compareOp(expr, "!=")

      case "&" =>
        binaryOp(expr, (left, right) => (left, right) match {
          case ((l, AstBool()), (r, AstBool())) => ("U.and(" + l + ", " + r + ")", AstBool())
          case (l, r) => error(expr, l, r)
        })
      case "|" =>
        binaryOp(expr, (left, right) => (left, right) match {
          case ((l, AstBool()), (r, AstBool())) => ("U.or(" + l + ", " + r + ")", AstBool())
          case (l, r) => error(expr, l, r)
        })

      case "atom" =>
        if (expr.getValues.contains("num")) {
          ("U.num(\"" + expr.getValues("num").head + "\")", AstNum())
        } else if (expr.getValues.contains("str")) {
          (expr.getValues("str").head, AstStr())
        } else {
          val path = expr.getBranches("path").head
          if (expr.getBranches.contains("call"))
            evalFun(expr, path.sourceCode, expr.getBranches("call").head.getBranches.getOrElse("expr", List()))
          else
            translateSegs(path.getBranches("segment")) // variable access
        }
    }
  }

  private def visit(node: Node): String = {
    val s = node.sourceCode.replaceAll("\n", " ").replaceAll(" +", " ")
    val s2 = if (s.length <= 60) s else s.substring(0, 40) + "..."
    val smSrc = String.format(
      """call.nodeId = %s; call.nodeLoc = "%s"; call.nodeSrc = "%s"; """,
      node.getId.toString, node.getBegin.toString, s2.replace("\"", "\\\""))
    val bpSrc = """if (breakpoints != null) {reached.set(call.nodeId); if (breakpoints.contains(call.nodeId)) throw new BreakPointException();} """
    if (genSourceMap) if (genBreakpoint) smSrc + bpSrc else smSrc else ""
  }

  private def evalFun(node: Node, name: String, arg: List[Node]): (String, AstType) = {
    name match {
      case "empty" => ("null", AstNull())
      case "assign" => {
        val segs: List[Node] = arg(0).getBranches("result").head.getBranches("path").head.getBranches("segment")
        (translateSegs(segs, save = true), translate(arg(1))) match {
          case ((v, t), (v1, t1)) =>
            if (t != AstNull && (t == t1 || t1 == AstNull())) (visit(node) + v + " = " + v1 + ";", AstNull()) // defined assignment
            else if (t == null && t1 != AstNull() && segs.size == 1 && !segs.head.getBranches.contains("index")) {
              astScope.obj.put(segs(0).sourceCode, t1) // new simple var
              (v + " = " + v1 + ";", AstNull())
            }
            else throw error(arg(0), "Incompatible types assignment")
        }
      }
      case "group" => {
        val sb: StringBuilder = new StringBuilder
        sb.append(visit(node));
        for (expr <- arg) {
          val stmt: String = translate(expr)._1
          if (stmt != "null") sb.append(stmt);
          sb.append("\n")
        }
        (sb.toString(), AstNull())
      }

      case "condition" => {
        val s = arg.map(translate(_)).reduceRight((l, r) =>
          (l, r) match {
            case ((lStr, _), (rStr, _)) => (lStr + " else {" + rStr + "}", AstNull())
          }
        )._1
        ("{" + visit(node) + s + "}", AstNull())
      }
      case "case" => translate(arg(0)) match {
        case (l, AstBool()) => (visit(node) + " if (U.nvl(" + l + ")) {\n" + arg.tail.map(translate(_)._1).mkString("") + "}", AstNull())
        case l => error(arg(0), l)
      }

      case "foreach" => {
        val segs: List[Node] = arg(0).getBranches("result").head.getBranches("path").head.getBranches("segment")
        if (segs.size != 1 || segs.head.getBranches.contains("index")) error(arg(0), "Complex path in foreach")
        val ref = arg(0).sourceCode // just simple path with one segment
        val scopeRef: AstType = astScope.obj.get(ref)
        if (scopeRef == null) {
          astScope.obj.put(ref, AstNum()) // new simple var
        } else if (scopeRef != AstNum()) error(arg(0), "Foreach variable is not num")
        val from = translate(arg(1));
        from match {
          case (l, AstNum()) =>
            arg.size match {
              case 3 => ("{" + visit(node) + "BigDecimal from_" + ref + " = " + from._1 + "; " +
                "label_" + ref + ": for (scope." + ref + " = from_" + ref + "; ; scope." + ref + " = scope." + ref + ".add(BigDecimal.ONE)) {" + translate(arg(2))._1 + "}}",
                AstNull())
              case 4 => ("{" + visit(node) + "BigDecimal from_" + ref + " = " + from._1 + "; " +
                "BigDecimal to_" + ref + " = " + translate(arg(2))._1 + "; " +
                "label_" + ref + ": for (scope." + ref + " = from_" + ref + "; scope." + ref + ".compareTo(to_" + ref + ") <= 0; scope." + ref + " = scope." + ref + ".add(BigDecimal.ONE)) {" + translate(arg(3))._1 + "}}",
                AstNull())
              case _ => error(arg(0), "foreach must be in form foreach(var, from, body) or foreach(var, from, to, body)") // exprs size is not 3 or 4
            }
          case l => error(arg(1), l)
        }
      }
      case "break" => (if (arg.isEmpty) visit(node) + "break;" else visit(node) + "break label_" + arg(0).sourceCode + ";", AstNull())
      case "continue" => (if (arg.isEmpty) visit(node) + "continue;" else visit(node) + "continue label_" + arg(0).sourceCode + ";", AstNull())

      case "procedure" => ( {
        val name = arg(0).sourceCode
        if (trees.containsKey(name)) visit(node) + name + "();" else visit(node) + "callNativeProcedure(\"" + name + "\");"
      }, AstNull())
      case "exit" => (visit(node) + "return;", AstNull())
      case "stop" => (visit(node) + "stop();", AstNull())
      case "message" => translate(arg(0)) match {
        case (l, _) => (visit(node) + "messages.add(U.toStr(" + l + "));", AstNull())
        case l => error(arg(0), l)
      }
      case "error" => (visit(node) + "U.error(" +
        (if (arg.isEmpty) ""
        else translate(arg(0)) match {
          case (l, AstStr()) => l
          case l => error(arg(0), l)
        }) + ");", AstNull())
      case "len" => ("U.len(" + translateSegs(arg(0).getBranches("result").head.getBranches("path").head.getBranches("segment"))._1 + ")", AstNum())
      case "index" => (translateSegs(arg(0).getBranches("result").head.getBranches("path").head.getBranches("segment"), index = true)._1, AstNum())
      case "subs" =>
        arg.size match {
          case 2 => (translate(arg(0)), translate(arg(1))) match {
            case ((s, AstStr()), (n, AstNum())) => ("U.subs(" + s + ", " + n + ")", AstStr())
            case (l, r) => error(arg(0), l, r)
          }
          case 3 => (translate(arg(0)), translate(arg(1)), translate(arg(2))) match {
            case ((s, AstStr()), (from, AstNum()), (to, AstNum())) => ("U.subs(" + s + ", " + from + ", " + to + ")", AstStr())
            case (l, r, _) => error(arg(0), l, r)
          }
          case _ => error(arg(0), "subs must be in form subs(str, from) or subs(str, from, to)")
        }
      case "indexOf" =>
        arg.size match {
          case 2 => (translate(arg(0)), translate(arg(1))) match {
            case ((s, AstStr()), (subStr, AstStr())) => ("U.indexOf(" + s + ", " + subStr + ")", AstNum())
            case (l, r) => error(arg(0), l, r)
          }
          case 3 => (translate(arg(0)), translate(arg(1)), translate(arg(2))) match {
            case ((s, AstStr()), (subStr, AstStr()), (n, AstNum())) => ("U.indexOf(" + s + ", " + subStr + ", " + n + ")", AstNum())
            case (l, r, _) => error(arg(0), l, r)
          }
          case _ => error(arg(0), "indexOf must be in form indexOf(str, substr) or indexOf(str, substr, from)")
        }
      case "lastIndexOf" =>
        arg.size match {
          case 2 => (translate(arg(0)), translate(arg(1))) match {
            case ((s, AstStr()), (subStr, AstStr())) => ("U.lastIndexOf(" + s + ", " + subStr + ")", AstNum())
            case (l, r) => error(arg(0), l, r)
          }
          case 3 => (translate(arg(0)), translate(arg(1)), translate(arg(2))) match {
            case ((s, AstStr()), (subStr, AstStr()), (n, AstNum())) => ("U.lastIndexOf(" + s + ", " + subStr + ", " + n + ")", AstNum())
            case (l, r, _) => error(arg(0), l, r)
          }
          case _ => error(arg(0), "lastIndexOf must be in form lastIndexOf(str, substr) or lastIndexOf(str, substr, from)")
        }
      case "startsWith" =>
        (translate(arg(0)), translate(arg(1))) match {
          case ((s, AstStr()), (subStr, AstStr())) => ("U.startsWith(" + s + ", " + subStr + ")", AstBool())
          case (l, r) => error(arg(0), l, r)
        }
      case "endsWith" =>
        (translate(arg(0)), translate(arg(1))) match {
          case ((s, AstStr()), (subStr, AstStr())) => ("U.endsWith(" + s + ", " + subStr + ")", AstBool())
          case (l, r) => error(arg(0), l, r)
        }

      case "min" => translateSegs(arg(0).getBranches("result").head.getBranches("path").head.getBranches("segment")) match {
        case (p, AstArr(AstNum())) => ("U.min(" + p + ")", AstNum())
        case (p, AstArr(AstBool())) => ("U.min(" + p + ")", AstBool())
        case (p, AstArr(AstStr())) => ("U.min(" + p + ")", AstStr())
        case (p, AstArr(AstDat())) => ("U.min(" + p + ")", AstDat())
        case _ => error(arg(0), "min must accest array argument")
      }
      case "max" => translateSegs(arg(0).getBranches("result").head.getBranches("path").head.getBranches("segment")) match {
        case (p, AstArr(AstNum())) => ("U.max(" + p + ")", AstNum())
        case (p, AstArr(AstBool())) => ("U.max(" + p + ")", AstBool())
        case (p, AstArr(AstStr())) => ("U.max(" + p + ")", AstStr())
        case (p, AstArr(AstDat())) => ("U.max(" + p + ")", AstDat())
        case _ => error(arg(0), "max must accept array argument")
      }
      case "round" => arg.size match {
        case 1 => translate(arg(0)) match {
          case (num, AstNum()) => ("U.round(" + num + ")", AstNum())
          case l => error(arg(0), l)
        }
        case 2 => (translate(arg(0)), translate(arg(1))) match {
          case ((num0, AstNum()), (num1, AstNum())) => ("U.round(" + num0 + ", " + num1 + ")", AstNum())
          case (l, r) => error(arg(0), l, r)
        }
        case _ => error(arg(0), "round should be in form round(num) or round(num, scale)")
      }
      case "floor" => translate(arg(0)) match {
        case (num, AstNum()) => ("U.floor(" + num + ")", AstNum())
        case l => error(arg(0), l)
      }
      case "power" => (translate(arg(0)), translate(arg(1))) match {
        case ((l, AstNum()), (r, AstNum())) => ("U.power(" + l + "," + r + ")", AstNum())
        case (l, r) => error(arg(0), l, r)
      }
      case "log" => (translate(arg(0)), translate(arg(1))) match {
        case ((base, AstNum()), (num, AstNum())) => ("U.log(" + base + ", " + num + ")", AstNum())
        case (l, r) => error(arg(0), l, r)
      }

      case "true" => ("true", AstBool())
      case "false" => ("false", AstBool())
      case "if" => translate(arg(0)) match {
        case (i, AstBool()) => ("(" + i + " ? " + translate(arg(1))._1 + " : " + translate(arg(2))._1 + ")", AstNull())
        case l => error(arg(0), l)
      }

      case "isEmpty" => translate(arg(0)) match {
        case (l, _) => ("(" + l + ") == null", AstBool())
      }
      case "isDefined" => translate(arg(0)) match {
        case (l, _) => ("(" + l + ") != null", AstBool())
      }

      case "date" => arg.size match {
        case 1 => translate(arg(0)) match {
          case (l, AstStr()) => ("U.dateParse(" + l + ")", AstDat())
          case l => error(arg(0), l)
        }
        case 3 => (translate(arg(0)), translate(arg(1)), translate(arg(2))) match {
          case ((year, AstNum()), (month, AstNum()), (day, AstNum())) => ("U.dateParse(" + year + ", " + month + ", " + day + ")", AstDat())
          case (l, r, _) => error(arg(0), l, r)
        }
      }
      case "day" => arg.size match {
        case 0 => ("Period.ofDays(1)", AstPer())
        case 1 => translate(arg(0)) match {
          case (dat, AstDat()) => ("U.day(" + dat + ")", AstNum())
          case l => error(arg(0), l)
        }
      }
      case "month" => arg.size match {
        case 0 => ("Period.ofMonths(1)", AstPer())
        case 1 => translate(arg(0)) match {
          case (dat, AstDat()) => ("U.month(" + dat + ")", AstNum())
          case l => error(arg(0), l)
        }
      }
      case "year" => arg.size match {
        case 0 => ("Period.ofYears(1)", AstPer())
        case 1 => translate(arg(0)) match {
          case (dat, AstDat()) => ("U.year(" + dat + ")", AstNum())
          case l => error(arg(0), l)
        }
      }
      case "daysBetween" => (translate(arg(0)), translate(arg(1))) match {
        case ((from, AstDat()), (to, AstDat())) => ("U.daysBetween(" + from + ", " + to + ")", AstNum())
        case (l, r) => error(arg(0), l, r)
      }
      case "daysInMonth" => translate(arg(0)) match {
        case (dat, AstDat()) => ("U.daysInMonth(" + dat + ")", AstNum())
        case l => error(arg(0), l)
      }
      case "setDay" => (translate(arg(0)), translate(arg(1))) match {
        case ((dat, AstDat()), (day, AstNum())) => ("U.setDay(" + dat + ", " + day + ")", AstDat())
        case (l, r) => error(arg(0), l, r)
      }
      case "format" => (translate(arg(0)), translate(arg(1))) match {
        case ((dat, AstDat()), (fmt, AstStr())) => ("U.format(" + dat + ", " + fmt + ")", AstStr())
        case (l, r) => error(arg(0), l, r)
      }

      case "today" => ("nativeFunctions.today()", AstDat()) // TODO define nativeFunctions
      case "businessDay" => ("nativeFunctions.businessDay()", AstDat())
      case "generateId" => ("nativeFunctions.generateId()", AstStr())
      case "nextNumber" => ("new BigDecimal(nativeFunctions.nextNumber())", AstNum())
      case "echo" => translate(arg(0)) match {
        case (l, _) => ("nativeFunctions.echo(new Str(" + l + "));", AstNull()) // TODO signature echo(Object)
      }
      //case "echoScope" => nativeFunctions.echoScope(scope) // TODO
      //  None
      case "dictInc" => (translate(arg(0)), translate(arg(1)), translate(arg(2)), translate(arg(3))) match {
        case ((dictCode, AstStr()), (fieldCode, AstStr()), (rowCode, AstStr()), (incrementValue, AstNum())) =>
          ("nativeFunctions.dictInc(" + dictCode + ", " + fieldCode + ", " + rowCode + ", " + incrementValue + ")", AstBool())
        case _ => error(arg(0), "dictInc must be in form dictInc(dictCode, fieldCode, rowCode, increment)")
      }
      case "dictDec" => (translate(arg(0)), translate(arg(1)), translate(arg(2)), translate(arg(3))) match {
        case ((dictCode, AstStr()), (fieldCode, AstStr()), (rowCode, AstStr()), (incrementValue, AstNum())) =>
          ("nativeFunctions.dictInc(" + dictCode + ", " + fieldCode + ", " + rowCode + ", (" + incrementValue + ").negate())", AstBool())
        case _ => error(arg(0), "dictDec must be in form dictDec(dictCode, fieldCode, rowCode, decrement)")
      }
      case "dictValueNumber" => (translate(arg(0)), translate(arg(1)), translate(arg(2))) match {
        case ((dictCode, AstStr()), (fieldCode, AstStr()), (rowCode, AstStr())) =>
          ("nativeFunctions.dictValueNumber(" + dictCode + ", " + fieldCode + ", " + rowCode + ")", AstNum())
        case _ => error(arg(0), "dictValueNumber must be in form dictValueNumber(dictCode, fieldCode, rowCode)")
      }
      case "dictValueText" => (translate(arg(0)), translate(arg(1)), translate(arg(2))) match {
        case ((dictCode, AstStr()), (fieldCode, AstStr()), (rowCode, AstStr())) =>
          ("nativeFunctions.dictValueText(" + dictCode + ", " + fieldCode + ", " + rowCode + ")", AstStr())
        case _ => error(arg(0), "dictValueText must be in form dictValueText(dictCode, fieldCode, rowCode)")
      }

      case "toNum" => translate(arg(0)) match {
        case (str, AstStr()) => ("U.toNum(" + str + ")", AstNum())
        case l => error(arg(0), l)
      }
      case "Object" => ("", AstNull())
      case "Objects" => ("", AstNull())
      case "Num" => ("", AstNull())
      case "Str" => ("", AstNull())
      case "Dat" => ("", AstNull())
      case "Bool" => ("", AstNull())
      case name => error(node, "Unknown function " + name)
    }
  }

  private def translateSegs(segs: List[Node], save: Boolean = false, index: Boolean = false): (String, AstType) = {
    val nLast: Int = segs.size - 1
    val s = segs.zipWithIndex.map(Function.tupled((seg: Node, n: Int) => {
      val name = seg.getValues("name").head
      val isLast = n == nLast
      val indexes = seg.getBranches.getOrElse("index", List())
      if (indexes.isEmpty) name
      else {
        if (indexes(0).getValues.contains("field")) {
          // all indexes should be in form [field1: value1]...[fieldN: valueN]
          val z = indexes.map(index => {
            val field: String = index.getValues("field").head // must contains "field"
            val filter: (String, AstType) = translate(index.getBranches("filter").head)
            (field, filter._1)
          }).unzip
          name + "_" + z._1.mkString("_") + "(" + (if (index) "" else save + ", ") + z._2.mkString(", ") + ")"
        } else translate(indexes(0).getBranches("filter").head) match {
          // just one index [num]
          case (num, AstNum()) => name + "(" + save + ", " + num + ")"
          case l => error(indexes(0), l)
        }
      }
    })).mkString("scope.", ".", "")

    val t: AstType = segs.foldLeft(astScope: AstType) {
      (acc, seg) => {
        val name = seg.getValues("name").head
        val t = acc match {
          case AstArr(AstObj(obj)) => obj.get(name)
          case AstObj(obj) => obj.get(name)
          case _ => error(seg, "Invalid path segment" + name)
        }
        if (!save && t == null) error(seg, "No field " + name)
        t
      }
    }

    (s, t);
  }

}
