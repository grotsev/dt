package kz.greetgo.dt;

import kz.greetgo.dt.dt_manager_compare_performance_test.TestData;
import kz.greetgo.dt.dt_manager_compare_performance_test.TestData$;
import kz.greetgo.dt.gen.*;
import org.fest.assertions.api.Assertions;
import org.fest.assertions.data.Offset;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import scala.math.BigDecimal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.fest.assertions.api.Assertions.assertThat;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public abstract class AbstractDtManagerTest {

  public static class TestNativeFunctions implements NativeFunctions {
    @Override
    public LocalDate today() {
      return LocalDate.now();
    }

    @Override
    public LocalDate businessDay() {
      return LocalDate.now();
    }

    @Override
    public String generateId() {
      return Double.toString(Math.random());
    }

    @Override
    public long nextNumber() {
      return 42L;
    }

    public final List<DtType> echoList = new ArrayList<>();

    @Override
    public void echo(DtType value) {
      System.out.println(value);
      echoList.add(value);
    }

    @Override
    public void echoScope(SortedMap<String, DtType> scope) {
      System.out.println(scope);
    }

    @Override
    public boolean dictInc(String dictCode, String fieldCode, String rowCode, BigDecimal incrementValue) {
      return false;
    }

    @Override
    public BigDecimal dictValueNumber(String dictCode, String fieldCode, String rowCode) {
      return null;
    }

    @Override
    public String dictValueText(String dictCode, String fieldCode, String rowCode) {
      return null;
    }
  }

  public static final TestNativeFunctions nativeFunctions = new TestNativeFunctions();

  @Test
  public void callNativeProcedure() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("s", new AstStr());
        }}
    ));

    final boolean called[] = new boolean[]{false};

    m.registerNativeProcedure("nativeLog", scope -> called[0] = true);

    m.registerUserProcedure("userLog", "condition(case(s = \"abc\", procedure(nativeLog)))");

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("s", new Str("abc"));

    m.registerTree("main", "procedure(userLog)");

    m.compile();
    m.executeTree("main", scope);

    System.out.println(m.getScope());
    assertThat(called[0]).isTrue();
  }

  @Test
  public void newVariableIntoAnotherProcedure() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("num1", new AstNum());
        }}
    ));

    m.registerUserProcedure("addHello3", "assign(hello314, hello314 + 3)");
    m.registerUserProcedure("saveHello", "assign(num1, hello314)");

    m.registerTree("main", "group(" +
        "  assign(hello314, num1)," +
        "  procedure(addHello3)," +
        "  procedure(saveHello)" +
        ")");

    m.compile();

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("num1", new Num(BigDecimal.valueOf(11)));
    m.executeTree("main", scope);

    System.out.println(scope);

    assertThat(m.getScope().get("num1").toString()).isEqualTo("Num(14)");
  }

  @Test
  public void echo() throws Exception {

    nativeFunctions.echoList.clear();

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("left", new AstNum());
        }}
    ));

    m.registerTree("main", "group(" +
        "  assign(hello314, 2345)," +
        "  echo(\"hello314 = \" ~ hello314)" +
        ")");

    m.compile();

    SortedMap<String, DtType> scope = new TreeMap<>();
    m.executeTree("main", scope);

    System.out.println(nativeFunctions.echoList);

    assertThat(nativeFunctions.echoList.get(0).toString()).isEqualTo("Str(hello314 = 2345)");

  }

  @Test
  public void nativeProcedureChangeData() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("hello", new AstStr());
        }}
    ));

    m.registerNativeProcedure("do1", scope -> scope.set("hello", Str.apply("world")));

    SortedMap<String, DtType> scope = new TreeMap<>();
    //scope.put("x", new Str("abc"));

    m.registerTree("main", "group(procedure(do1), assign( hello1, hello ))");

    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("hello1")).isEqualTo(Str.apply("world"));
  }

  protected abstract DtManagerWrapper createDtManager(NativeFunctions nativeFunctions);


  @Test
  public void callUserProcedure() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));

    m.registerUserProcedure("p1", "assign(called, called ~ \"p1\")");
    m.registerUserProcedure("p2", "assign(called, called ~ \"p2\")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.registerTree("main", "group( assign(called, \"main \"), procedure(p1), procedure(p2) )");

    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("called").toString()).isEqualTo("Str(main p1p2)");
  }

  private final Consumer<ScopeAccess> logger = System.out::println;

  @SuppressWarnings("SameParameterValue")
  private String resource(String name) throws URISyntaxException, IOException {
    java.net.URL url = getClass().getResource(name);
    java.nio.file.Path resPath = java.nio.file.Paths.get(url.toURI());
    return new String(java.nio.file.Files.readAllBytes(resPath), "UTF8");
  }

  @Test(expectedExceptions = ErrorException.class, expectedExceptionsMessageRegExp = ".*Validity of the contract contraExpireDate")
  public void createAccount() throws IOException, URISyntaxException {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(TestData$.MODULE$.client());
    m.registerNativeProcedure("payResultProc", logger);
    m.registerNativeProcedure("sendSMS", logger);
    //m.registerUserProcedure("userLog", "condition(x=1, procedure(nativeLog))");

    SortedMap<String, DtType> scope = new TreeMap<>();
    //scope.put("client.account.123.in.interestRate", new Num(BigDecimal.valueOf(-1)));
    scope.put("client.account.123.id", new Str("123"));
    scope.put("in.accountId", new Str("123"));

    String code = resource("createAccount.txt");
    m.registerTree("main", code);
    m.compile();
    DtExecuteResult executeResult = m.executeTree("main", scope);

    System.out.println(scope);
    System.out.println(executeResult.messages);
  }

  private void see(SortedMap<String, DtType> scope) {
    for (Map.Entry<String, DtType> e : scope.entrySet()) {
      //noinspection unused
      String s = e.getKey() + "=" + e.getValue();
//      System.out.println(s);
    }
  }

  @Test
  public void dates_less_good_good_1() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("dat1", new AstDat());
          put("dat2", new AstDat());
        }}
    ));

    DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("dat1", new Dat(LocalDate.parse("2010-01-01", f)));
    scope.put("dat2", new Dat(LocalDate.parse("2011-01-01", f)));

    m.registerTree("main", "group (assign(x, 1), condition(" +
        "  case ( dat1 < dat2 , assign(x, 2)) " +
        "))");
    m.compile();
    m.executeTree("main", scope);

    System.out.println(m.getScope().get("x").toString());
    assertThat(m.getScope().get("x").toString()).isEqualTo("Num(2)");

  }

  @Test
  public void dates_less_good_good_2() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("dat1", new AstDat());
          put("dat2", new AstDat());
        }}
    ));

    DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("dat1", new Dat(LocalDate.parse("2011-01-01", f)));
    scope.put("dat2", new Dat(LocalDate.parse("2010-01-01", f)));

    m.registerTree("main", "group (assign(x, 1), condition(" +
        "  case ( dat1 < dat2 , assign(x, 2)) " +
        "))");
    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("x").toString()).isEqualTo("Num(1)");

  }

  @Test
  public void dates_less_null_good() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("dat2", new AstDat());
          put("datNull", new AstDat());
        }}
    ));

    DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("dat2", new Dat(LocalDate.parse("2010-01-01", f)));

    m.registerTree("main", "group (assign(x, 1), condition(" +
        "  case( datNull < dat2 , assign(x, 2)) " +
        "))");
    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("x").toString()).isEqualTo("Num(2)");

  }

  @Test
  public void dates_less_good_null() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("dat1", new AstDat());
          put("datNull", new AstDat());
        }}
    ));

    DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("dat1", new Dat(LocalDate.parse("2010-01-01", f)));

    m.registerTree("main", "group (assign(x, 1), condition(" +
        "  case( dat1 < datNull , assign(x, 2)) " +
        "))");
    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("x").toString()).isEqualTo("Num(1)");

  }

  @Test
  public void dates_less_null_null() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("datNull1", new AstDat());
          put("datNull2", new AstDat());
        }}
    ));

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.registerTree("main", "group (assign(x, 1), condition(" +
        "  case( datNull1 < datNull2 , assign(x, 2)) " +
        "))");
    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("x").toString()).isEqualTo("Num(1)");

  }


  @Test
  public void dates_lessOrEqual_good_good_1() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("dat1", new AstDat());
          put("dat2", new AstDat());
        }}
    ));

    DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("dat1", new Dat(LocalDate.parse("2010-01-01", f)));
    scope.put("dat2", new Dat(LocalDate.parse("2011-01-01", f)));

    m.registerTree("main", "group (assign(x, 1), condition(" +
        "  case ( dat1 <= dat2 , assign(x, 2)) " +
        "))");
    m.compile();
    m.executeTree("main", scope);

    System.out.println(m.getScope().get("x").toString());
    assertThat(m.getScope().get("x").toString()).isEqualTo("Num(2)");
  }

  @Test
  public void dates_lessOrEqual_good_good_2() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("dat1", new AstDat());
          put("dat2", new AstDat());
        }}
    ));

    DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("dat1", new Dat(LocalDate.parse("2011-01-01", f)));
    scope.put("dat2", new Dat(LocalDate.parse("2010-01-01", f)));

    m.registerTree("main", "group (assign(x, 1), condition(" +
        "  case ( dat1 <= dat2 , assign(x, 2)) " +
        "))");
    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("x").toString()).isEqualTo("Num(1)");
  }

  @Test
  public void caseSecondIsNulll() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("dat1", new AstDat());
          put("str1", new AstStr());
        }}
    ));

    DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("dat1", new Dat(LocalDate.parse("2011-01-01", f)));

    m.registerTree("main", "group (assign(x, 1), condition(" +
        "  case ( isDefined(dat1) & str1=\"A\" , assign(x, 2)) " +
        "))");
    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("x").toString()).isEqualTo("Num(1)");
  }

  @Test
  public void dates_lessOrEqual_good_good_3() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("dat1", new AstDat());
          put("dat2", new AstDat());
        }}
    ));

    DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("dat1", new Dat(LocalDate.parse("2010-01-01", f)));
    scope.put("dat2", new Dat(LocalDate.parse("2010-01-01", f)));

    m.registerTree("main", "group (assign(x, 1), condition(" +
        "  case ( dat1 <= dat2 , assign(x, 2)) " +
        "))");
    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("x").toString()).isEqualTo("Num(2)");
  }

  @Test
  public void dates_lessOrEqual_null_good() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("datNull", new AstDat());
          put("dat2", new AstDat());
        }}
    ));

    DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("dat2", new Dat(LocalDate.parse("2010-01-01", f)));

    m.registerTree("main", "group (assign(x, 1), condition(" +
        "  case( datNull <= dat2 , assign(x, 2)) " +
        "))");
    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("x").toString()).isEqualTo("Num(2)");
  }

  @Test
  public void dates_lessOrEqual_good_null() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("datNull", new AstDat());
          put("dat1", new AstDat());
        }}
    ));

    DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("dat1", new Dat(LocalDate.parse("2010-01-01", f)));

    m.registerTree("main", "group (assign(x, 1), condition(" +
        "  case( dat1 <= datNull , assign(x, 2)) " +
        "))");
    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("x").toString()).isEqualTo("Num(1)");
  }

  @Test
  public void dates_lessOrEqual_null_null() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("datNull1", new AstDat());
          put("datNull2", new AstDat());
        }}
    ));

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.registerTree("main", "group (assign(x, 1), condition(" +
        "  case( datNull1 <= datNull2 , assign(x, 2)) " +
        "))");
    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("x").toString()).isEqualTo("Num(2)");
  }

  @Test
  public void daysBetween() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("dat1", new AstDat());
          put("dat2", new AstDat());
        }}
    ));

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("dat1", new Dat(LocalDate.now()));
    scope.put("dat2", new Dat(LocalDate.now().minusDays(2)));

    m.registerTree("main", "group (assign(x, daysBetween(dat1,dat2) ) )");
    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("x").toString()).isEqualTo("Num(-2)");
  }

  @Test
  public void num() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.registerTree("main", "group(assign(x, toNum(\"123.45\") ), assign(y, toNum(\"z123.45\") ))");
    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("x").toString()).isEqualTo("Num(123.45)");
    assertThat(m.getScope().get("y")).isNull();
  }

  @Test
  public void len() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("a", new AstArr(new AstObj(
              new HashMap<String, AstType>() {{
                put("s", new AstArr(new AstObj(
                    new HashMap<String, AstType>() {{
                      put("z", new AstDat());
                    }}
                )));
              }}
          )));
        }}
    ));

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("a.0.s.0.z", new Dat(LocalDate.now()));
    scope.put("a.0.s.1.z", new Dat(LocalDate.now()));
    scope.put("num", new Num(BigDecimal.valueOf(0)));

    m.registerTree("main", "group ( assign(num,len(a[0].s)))");
    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("num").toString()).isEqualTo("Num(2)");
  }

  @Test
  public void testArrayLenOnNotFilledArray() throws Exception {

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("asd", new AstObj(
              new HashMap<String, AstType>() {{
                put("arrLen", new AstNum());
                put("arr", new AstArr(new AstObj(
                    new HashMap<String, AstType>() {{
                      put("field", new AstStr());
                    }}
                )));
              }}
          ));
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(asd.arr[10].field, \"HI\")," +
        "  assign(asd.arrLen, len(asd.arr))" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    Num num = (Num) outScope.get("asd.arrLen");
    assertThat(num.num().intValue()).isEqualTo(11);
  }

  @Test
  public void emptyFunction() throws Exception {
    if (getClass().getName().endsWith("3Test")) throw new SkipException("empty() has not type");

    DtManagerWrapper m = createDtManager(nativeFunctions);

    m.registerTree("main", "group(" +
        "  assign(asd.asd, empty())" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope).hasSize(1);
    assertThat(outScope).containsKey("asd.asd");
    assertThat(outScope.get("asd.asd")).isNull();
  }

  @Test
  public void emptyIsZero_zeroOnRightSide() throws Exception {

    DtManagerWrapper m = createDtManager(nativeFunctions);

    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("asd", new AstObj(
              new HashMap<String, AstType>() {{
                put("asd", new AstNum());
              }}
          ));
        }}
    ));

    m.registerTree("main", "group(" +
        "  assign(asd.asd, empty())," +
        "  condition(" +
        "    case(asd.asd = 0, assign(strRes, \"empty is zero\"))," +
        "    case(1=1, assign(strRes, \"empty is not zero\"))" +
        "  )" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(getStr("strRes", outScope)).describedAs("Empty value in arithmetic operations must be converted to zero")
        .isEqualTo("empty is zero");
  }

  @Test
  public void emptyIsZero_zeroOnLeftSide() throws Exception {

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("asd", new AstObj(
              new HashMap<String, AstType>() {{
                put("asd", new AstNum());
              }}
          ));
        }}
    ));

    m.registerTree("main", "group(" +
        "  assign(asd.asd, empty())," +
        "  condition(" +
        "    case(0 = asd.asd, assign(strRes, \"empty is zero\"))," +
        "    case(1=1, assign(strRes, \"empty is not zero\"))" +
        "  )" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(getStr("strRes", outScope)).describedAs("Empty value in arithmetic operations must be converted to zero")
        .isEqualTo("empty is zero");
  }

  @Test
  public void equalOperation_emptyIsNotEqualToVoidStr() throws Exception {

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyStr", new AstStr());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(emptyStr, empty())," +
        "  assign(voidStr, \"\")," +
        "  assign(num, 0)," +
        "  condition(" +
        "    case(emptyStr = voidStr, assign(numRes, 1))," +
        "    case(1=1, assign(numRes, 2))" +
        "  )" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(getInt("numRes", outScope))
        .describedAs("emptyValue = voidStr ---> must be false")
        .isEqualTo(2);
  }


  @Test
  public void notEqualOperation_emptyIsNotEqualToVoidStr() throws Exception {

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyStr", new AstStr());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(emptyStr, empty())," +
        "  assign(voidStr, \"\")," +
        "  assign(numRes, 0)," +
        "  condition(" +
        "    case(emptyStr != voidStr, assign(numRes, 1))," +
        "    case(1=1, assign(numRes, 2))" +
        "  )" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(getInt("numRes", outScope))
        .describedAs("emptyStr != voidStr ---> must be true")
        .isEqualTo(1);
  }

  private String getStr(String key, SortedMap<String, DtType> scope) {
    Str str = (Str) scope.get(key);
    return str.str();
  }

  private int getInt(String key, SortedMap<String, DtType> scope) {
    Num str = (Num) scope.get(key);
    return str.num().intValue();
  }

  @Test
  public void lessOperation_minusOneMustBeLessThenEmpty() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyValue", new AstNum());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(emptyValue, empty())," +
        "  assign(numRes, 0)," +
        "  condition(" +
        "    case(-1 < emptyValue, assign(numRes, 1))," +
        "    case(1=1, assign(numRes, 2))" +
        "  )" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(getInt("numRes", outScope))
        .describedAs("-1 < emptyValue  ---> must be true")
        .isEqualTo(1);
  }

  @Test
  public void lessOperation_oneMustNotBeLessThenEmpty() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyValue", new AstNum());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(emptyValue, empty())," +
        "  assign(numRes, 0)," +
        "  condition(" +
        "    case(1 < emptyValue, assign(numRes, 1))," +
        "    case(1=1, assign(numRes, 2))" +
        "  )" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(getInt("numRes", outScope))
        .describedAs("1 < emptyValue  ---> must be false")
        .isEqualTo(2);
  }

  @Test
  public void trueCaseMustWorkOnlyOne() throws Exception {

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));
    m.registerTree("main", "group(" +
        "  assign(res0, 0)," +
        "  assign(res1, 0)," +
        "  assign(res2, 0)," +
        "  condition(" +
        "    case(1=0, assign(res0, 1))," +
        "    case(2=2, assign(res1, 1))," +
        "    case(1=1, assign(res2, 1))" +
        "  )" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(getInt("res0", outScope)).describedAs("False case must not work").isEqualTo(0);
    assertThat(getInt("res1", outScope)).describedAs("First true case work ok").isEqualTo(1);
    assertThat(getInt("res2", outScope)).describedAs("Second true case must not work!").isEqualTo(0);
  }

  @Test
  public void emptyMustBeEqualToEmpty() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyValue1", new AstNum());
          put("emptyValue2", new AstNum());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(emptyValue1, empty())," +
        "  assign(emptyValue2, empty())," +
        "  assign(numRes, 0)," +
        "  condition(" +
        "    case(emptyValue1 = emptyValue2, assign(numRes, 1))," +
        "    case(1=1,  assign(numRes, 2))" +
        "  )" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(getInt("numRes", outScope))
        .describedAs("emptyValue1 = emptyValue2  ---> must be true")
        .isEqualTo(1);
  }

  @Test
  public void emptyMustNotBeNotEqualToEmpty() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyValue1", new AstNum());
          put("emptyValue2", new AstNum());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(emptyValue1, empty())," +
        "  assign(emptyValue1, empty())," +
        "  assign(numRes, 0)," +
        "  condition(" +
        "    case(emptyValue1 != emptyValue2, assign(numRes, 1))," +
        "    case(1=1, assign(numRes, 2))" +
        "  )" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(getInt("numRes", outScope))
        .describedAs("emptyValue1 != emptyValue2  ---> must be false")
        .isEqualTo(2);
  }

  @DataProvider
  public Object[][] operationDataProvider() {
    return new Object[][]{

        new Object[]{"emptyValue >  one      ", "false"},
        new Object[]{"emptyValue >  minusOne ", "true"},
        new Object[]{"emptyValue >  zero     ", "false"},
        new Object[]{"emptyValue >= one      ", "false"},
        new Object[]{"emptyValue >= minusOne ", "true"},
        new Object[]{"emptyValue >= zero     ", "true"},
        new Object[]{"emptyValue <  one      ", "true"},
        new Object[]{"emptyValue <  minusOne ", "false"},
        new Object[]{"emptyValue <  zero     ", "false"},
        new Object[]{"emptyValue <= one      ", "true"},
        new Object[]{"emptyValue <= minusOne ", "false"},
        new Object[]{"emptyValue <= zero     ", "true"},
        new Object[]{"emptyValue =  one      ", "false"},
        new Object[]{"emptyValue =  minusOne ", "false"},
        new Object[]{"emptyValue =  zero     ", "true"},
        new Object[]{"emptyValue != one      ", "true"},
        new Object[]{"emptyValue != minusOne ", "true"},
        new Object[]{"emptyValue != zero     ", "false"},

        new Object[]{"     one >  emptyValue ", "true"},
        new Object[]{"minusOne >  emptyValue ", "false"},
        new Object[]{"    zero >  emptyValue ", "false"},
        new Object[]{"     one >= emptyValue ", "true"},
        new Object[]{"minusOne >= emptyValue ", "false"},
        new Object[]{"    zero >= emptyValue ", "true"},
        new Object[]{"     one <  emptyValue ", "false"},
        new Object[]{"minusOne <  emptyValue ", "true"},
        new Object[]{"    zero <  emptyValue ", "false"},
        new Object[]{"     one <= emptyValue ", "false"},
        new Object[]{"minusOne <= emptyValue ", "true"},
        new Object[]{"    zero <= emptyValue ", "true"},
        new Object[]{"     one =  emptyValue ", "false"},
        new Object[]{"minusOne =  emptyValue ", "false"},
        new Object[]{"    zero =  emptyValue ", "true"},
        new Object[]{"     one != emptyValue ", "true"},
        new Object[]{"minusOne != emptyValue ", "true"},
        new Object[]{"    zero != emptyValue ", "false"},

        new Object[]{"emptyValue = emptyValue ", "true"},
        new Object[]{"emptyValue != emptyValue ", "false"},

        new Object[]{"emptyValue * one = 0 ", "true"},

        new Object[]{" trueValue  & emptyBool ", "false"},
        new Object[]{" falseValue & emptyBool ", "false"},
        new Object[]{" trueValue  | emptyBool ", "true"},
        new Object[]{" falseValue | emptyBool ", "false"},

        new Object[]{" emptyBool & trueValue  ", "false"},
        new Object[]{" emptyBool & falseValue ", "false"},
        new Object[]{" emptyBool | trueValue  ", "true"},
        new Object[]{" emptyBool | falseValue ", "false"},

        new Object[]{" ! emptyBool ", "true"},
    };
  }

  @Test(dataProvider = "operationDataProvider")
  public void operationOnEmptyValue(String condition, String result) throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyValue", new AstNum());
          put("emptyBool", new AstBool());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(emptyValue, empty())," +
        "  assign(minusOne, -1)," +
        "  assign(trueValue, true())," +
        "  assign(falseValue, false())," +
        "  assign(one, 1)," +
        "  assign(zero, 0)," +
        "  assign(strRes, \"no result\")," +
        "  condition(" +
        "    case(" + condition + ", assign(strRes, \"true\"))," +
        "    case(1=1, assign(strRes, \"false\"))" +
        "  )" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(getStr("strRes", outScope))
        .describedAs(condition + "  ---> must be " + result)
        .isEqualTo(result);
  }

  @Test(expectedExceptions = {ExprException.class, ArithmeticException.class}, expectedExceptionsMessageRegExp = ".*Division by zero")
  public void oneDivideZero() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));
    m.registerTree("main", "group(" +
        "  assign(one, 1)," +
        "  assign(zero, 0)," +
        "  assign(numRes, one/zero)" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();

    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);
  }

  @Test(expectedExceptions = {ExprException.class, ArithmeticException.class}, expectedExceptionsMessageRegExp = ".*Division by zero")
  public void oneDivideEmpty() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyValue", new AstNum());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(one, 1)," +
        "  assign(emptyValue, empty())," +
        "  assign(numRes, one/emptyValue)" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);
  }

  @Test
  public void onePlusEmptyValue() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyValue", new AstNum());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(one, 1)," +
        "  assign(emptyValue, empty())," +
        "  assign(numRes, one + emptyValue)" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    String d = "one + empty must be one";

    assertThat(outScope.get("numRes")).describedAs(d).isNotNull();
    assertThat(getInt("numRes", outScope)).describedAs(d).isEqualTo(1);
  }

  @Test
  public void emptyValuePlusOne() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyValue", new AstNum());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(one, 1)," +
        "  assign(emptyValue, empty())," +
        "  assign(numRes, emptyValue + one)" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    String d = "empty + one must be one";
    assertThat(outScope.get("numRes")).describedAs(d).isNotNull();
    assertThat(getInt("numRes", outScope)).describedAs(d).isEqualTo(1);
  }

  @Test
  public void oneMinusEmptyValue() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyValue", new AstNum());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(one, 1)," +
        "  assign(emptyValue, empty())," +
        "  assign(numRes, one - emptyValue)" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    String d = "one - empty must be one";

    assertThat(outScope.get("numRes")).describedAs(d).isNotNull();
    assertThat(getInt("numRes", outScope)).describedAs(d).isEqualTo(1);
  }

  @Test
  public void emptyValueMinusOne() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyValue", new AstNum());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(one, 1)," +
        "  assign(emptyValue, empty())," +
        "  assign(numRes, emptyValue - one)" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    String d = "empty - one must be minus one";

    assertThat(outScope.get("numRes")).describedAs(d).isNotNull();
    assertThat(getInt("numRes", outScope)).describedAs(d).isEqualTo(-1);
  }

  @Test
  public void oneMultiplyEmptyValue() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyValue", new AstNum());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(one, 1)," +
        "  assign(emptyValue, empty())," +
        "  assign(numRes, one * emptyValue)" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    String d = "one * empty must be zero";
    assertThat(outScope.get("numRes")).describedAs(d).isNotNull();

    assertThat(getInt("numRes", outScope)).describedAs(d).isEqualTo(0);
  }

  @Test
  public void emptyValueMultiplyOne() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyValue", new AstNum());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(one, 1)," +
        "  assign(emptyValue, empty())," +
        "  assign(numRes, emptyValue * one)" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    String d = "empty * one must be zero";

    assertThat(outScope.get("numRes")).describedAs(d).isNotNull();
    assertThat(getInt("numRes", outScope)).describedAs(d).isEqualTo(0);
  }

  @Test
  public void power() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));
    m.registerTree("main", "group(" +
        "  assign(numRes, power(3, 4))" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);
    assertThat(outScope.get("numRes")).isInstanceOf(Num.class);
  }

  @Test
  public void powerOfEmpty() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyValue", new AstNum());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(emptyValue, empty())," +
        "  assign(numRes, power(3, emptyValue))" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    String d = "3 in power of empty must be one";

    assertThat(outScope.get("numRes")).describedAs(d).isInstanceOf(Num.class);
    assertThat(getInt("numRes", outScope)).describedAs(d).isEqualTo(1);
  }

  @Test
  public void concat() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));
    m.registerTree("main", "group(" +
        "  assign(str1, \"str1\")," +
        "  assign(str2, \"str2\")," +
        "  assign(strRes, str1 ~ str2)" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    String d = "'str1' ~ 'str2' must be 'str1str2'";

    assertThat(outScope.get("strRes")).describedAs(d).isInstanceOf(Str.class);
    assertThat(getStr("strRes", outScope)).describedAs(d).isEqualTo("str1str2");
  }

  @Test
  public void concatEmptyStr() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyStr", new AstStr());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(str, \"str\")," +
        "  assign(emptyStr, empty())," +
        "  assign(strRes, emptyStr ~ str)" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    String d = "empty ~ 'str' must be 'str'";

    assertThat(outScope.get("strRes")).describedAs(d).isInstanceOf(Str.class);
    assertThat(getStr("strRes", outScope)).describedAs(d).isEqualTo("str");

  }

  @Test
  public void concatStrEmpty() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyStr", new AstStr());
        }}
    ));
    m.registerTree("main", "group(" +
        "  assign(str, \"str\")," +
        "  assign(emptyStr, empty())," +
        "  assign(strRes, str ~ emptyStr)" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    String d = "'str' ~ empty must be 'str'";

    assertThat(outScope.get("strRes")).describedAs(d).isInstanceOf(Str.class);
    assertThat(getStr("strRes", outScope)).describedAs(d).isEqualTo("str");
  }

  @Test
  public void concatStrNum() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));
    m.registerTree("main", "group(" +
        "  assign(str, \"str\")," +
        "  assign(num, 123)," +
        "  assign(strRes, str ~ num)" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    String d = "'str' ~ 123 must be 'str123'";

    assertThat(outScope.get("strRes")).describedAs(d).isInstanceOf(Str.class);
    assertThat(getStr("strRes", outScope)).describedAs(d).isEqualTo("str123");
  }

  @Test
  public void concatNumNum() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));
    m.registerTree("main", "group(" +
        "  assign(num1, 12)," +
        "  assign(num2, 34)," +
        "  assign(strRes, num1 ~ num2)" +
        ")");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    String d = "12 ~ 34 must be '1234'";

    assertThat(outScope.get("strRes")).describedAs(d).isInstanceOf(Str.class);
    assertThat(getStr("strRes", outScope)).describedAs(d).isEqualTo("1234");
  }

  @Test
  public void lenOnArrayInArray() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("account.1.asd", new Str("asd value"));
    scope.put("account.1.transaction.3.value", new Str("hello"));

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("account", new AstArr(new AstObj(
              new HashMap<String, AstType>() {{
                put("asd", new AstStr());
                put("transaction", new AstArr(new AstObj(
                    new HashMap<String, AstType>() {{
                      put("value", new AstStr());
                    }}
                )));
              }}
          )));
        }}
    ));

    m.registerTree("main", "group(" +
        "  assign(numRes, len(account[1].transaction))" +
        ")");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("numRes")).isInstanceOf(Num.class);
    assertThat(getInt("numRes", outScope)).isEqualTo(4);
  }

  @Test
  public void lenOnArrayInArray2() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("account.1.asd", new Str("asd value"));
    scope.put("account.1.transaction.3.value", new Str("hello"));

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("account", new AstArr(new AstObj(
              new HashMap<String, AstType>() {{
                put("asd", new AstStr());
                put("transaction", new AstArr(new AstObj(
                    new HashMap<String, AstType>() {{
                      put("value", new AstStr());
                    }}
                )));
              }}
          )));
        }}
    ));

    m.registerTree("main", "group(" +
        "  assign(numRes, len(account[asd:\"asd value\"].transaction))" +
        ")");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("numRes")).isInstanceOf(Num.class);
    assertThat(getInt("numRes", outScope)).isEqualTo(4);
  }

  @Test
  public void indexOfArrayElement() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("account.1.asd", new Str("asd A"));
    scope.put("account.2.asd", new Str("asd B"));
    scope.put("account.3.asd", new Str("asd C"));
    scope.put("account.4.asd", new Str("asd D"));

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("account", new AstArr(new AstObj(
              new HashMap<String, AstType>() {{
                put("asd", new AstStr());
                put("transaction", new AstArr(new AstObj(
                    new HashMap<String, AstType>() {{
                      put("value", new AstStr());
                    }}
                )));
              }}
          )));
        }}
    ));

    m.registerTree("main", "group(" +
        "  assign(numRes, index(account[asd:\"asd C\"]))" +
        ")");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("numRes")).isInstanceOf(Num.class);
    assertThat(getInt("numRes", outScope)).isEqualTo(3);
  }

  @Test
  public void readFromEnd() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("account.0.asd", new Str("asd O"));
    scope.put("account.1.asd", new Str("asd A"));
    scope.put("account.2.asd", new Str("asd B"));
    scope.put("account.3.asd", new Str("asd C"));
    scope.put("account.4.asd", new Str("asd D"));

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("account", new AstArr(new AstObj(
              new HashMap<String, AstType>() {{
                put("asd", new AstStr());
                put("transaction", new AstArr(new AstObj(
                    new HashMap<String, AstType>() {{
                      put("value", new AstStr());
                    }}
                )));
              }}
          )));
        }}
    ));

    m.registerTree("main", "group(" +
        "   assign(lastElement,       account[-1].asd)" +
        "  ,assign(beforeLastElement, account[-2].asd)" +
        "  ,assign(someFromEnd,       account[-4].asd)" +
        "  ,assign(emptyStr,        account[-1000].asd)" +
        ")");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    String d = "[-1] is last, [-2] is before last, ...";

    assertThat(outScope.get("lastElement")).describedAs(d).isInstanceOf(Str.class);
    assertThat(getStr("lastElement", outScope)).describedAs(d).isEqualTo("asd D");

    assertThat(outScope.get("beforeLastElement")).describedAs(d).isInstanceOf(Str.class);
    assertThat(getStr("beforeLastElement", outScope)).describedAs(d).isEqualTo("asd C");

    assertThat(outScope.get("someFromEnd")).describedAs(d).isInstanceOf(Str.class);
    assertThat(getStr("someFromEnd", outScope)).describedAs(d).isEqualTo("asd A");

    assertThat(outScope.get("emptyStr")).isNull();
  }

  @Test
  public void writeFromEnd() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("account.0.asd", new Str("asd O"));
    scope.put("account.1.asd", new Str("asd A"));
    scope.put("account.2.asd", new Str("asd B"));
    scope.put("account.3.asd", new Str("asd C"));
    scope.put("account.4.asd", new Str("asd D"));

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("account", new AstArr(new AstObj(
              new HashMap<String, AstType>() {{
                put("asd", new AstStr());
                put("transaction", new AstArr(new AstObj(
                    new HashMap<String, AstType>() {{
                      put("value", new AstStr());
                    }}
                )));
              }}
          )));
        }}
    ));

    m.registerTree("main", "group(" +
        "   assign(account[-1].asd, \"last\")" +
        "  ,assign(account[-2].asd, \"before last\")" +
        "  ,assign(account[-4].asd, \"Hello world\")" +
        ")");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    String d = "[-1] is last, [-2] is before last, ...";

    assertThat(outScope.get("account.1.asd").toString()).describedAs(d).isEqualTo("Str(Hello world)");
    assertThat(outScope.get("account.3.asd").toString()).describedAs(d).isEqualTo("Str(before last)");
    assertThat(outScope.get("account.4.asd").toString()).describedAs(d).isEqualTo("Str(last)");
  }

  @Test(expectedExceptions = {ExprException.class, ArrayIndexOutOfBoundsException.class}, expectedExceptionsMessageRegExp = ".*-4000.*")
  public void writeFromEndOutOfDiapason() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("account.0.asd", new Str("asd O"));
    scope.put("account.1.asd", new Str("asd A"));
    scope.put("account.2.asd", new Str("asd B"));
    scope.put("account.3.asd", new Str("asd C"));
    scope.put("account.4.asd", new Str("asd D"));

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("account", new AstArr(new AstObj(
              new HashMap<String, AstType>() {{
                put("asd", new AstStr());
                put("transaction", new AstArr(new AstObj(
                    new HashMap<String, AstType>() {{
                      put("value", new AstStr());
                    }}
                )));
              }}
          )));
        }}
    ));
    m.registerUserProcedure("proc", "group(  assign(account[-4000].asd, \"Hello world\"))");

    m.registerTree("main", "procedure(proc)");

    m.compile();
    m.executeTree("main", scope);
  }

  @DataProvider
  public Object[][] dataProvider_function_toNum() {
    return new Object[][]{
        new Object[]{"12.3", new Num(BigDecimal.decimal(12.3))},
        new Object[]{"12,3", new Num(BigDecimal.decimal(12.3))},
        new Object[]{"12 000,3", new Num(BigDecimal.decimal(12000.3))},
        new Object[]{"12_000,3", new Num(BigDecimal.decimal(12000.3))},

        new Object[]{"-12.3", new Num(BigDecimal.decimal(-12.3))},
        new Object[]{"-12,3", new Num(BigDecimal.decimal(-12.3))},
        new Object[]{" -  1 2,  3 ", new Num(BigDecimal.decimal(-12.3))},
        new Object[]{" -  1 2, _3 ", new Num(BigDecimal.decimal(-12.3))},
        new Object[]{"-12,3e-17", new Num(BigDecimal.decimal(-12.3e-17))},
        new Object[]{"+ 1 2 ._3 E+17", new Num(BigDecimal.decimal(+12.3e+17))},
        new Object[]{"12.3E17", new Num(BigDecimal.decimal(+12.3e+17))},
    };
  }

  @Test(dataProvider = "dataProvider_function_toNum")
  public void function_toNum(String strValue, DtType expectedValue) throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("strValue", new Str(strValue));
    scope.put("numValue", new Num(BigDecimal.decimal(0)));

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("strValue", new AstStr());
        }}
    ));

    m.registerTree("main", "assign(numValue, toNum(strValue))");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("numValue")).isEqualTo(expectedValue);
  }

  @DataProvider
  public Object[][] dataProvider_function_indexOf() {
    return new Object[][]{
        new Object[]{"asd", "asd", 0, "indexOf(str, subStr)", 0},
        new Object[]{"asd", "dsa", 0, "indexOf(str, subStr)", -1},
        new Object[]{"asd", "asd", -10, "indexOf(str, subStr, anIndex)", 0},
        new Object[]{" asd", "asd", -10, "indexOf(str, subStr, anIndex)", 1},
        new Object[]{"hello asd world", "asd", 0, "indexOf(str, subStr)", "hello asd world".indexOf("asd")},
        new Object[]{"asd hello asd world", "asd", 0, "indexOf(str, subStr, anIndex)", 0},
        new Object[]{"asd hello asd world", "asd", 1, "indexOf(str, subStr, anIndex)", 10},
        new Object[]{"asd hello asd world", "asd", 11, "indexOf(str, subStr, anIndex)", -1},
        new Object[]{"asd hello asd world", "abra", 0, "indexOf(str, subStr, anIndex)", -1},
        new Object[]{"asd hello asd world", "abra", 0, "indexOf(str, subStr)", -1},
        new Object[]{"Hei, asd hello asd world", "asd", 6, "indexOf(str, subStr, anIndex)", 15},

        new Object[]{" asd hello asd world", "asd", 11, "lastIndexOf(str, subStr)", 11},
        new Object[]{" asd hello asd world", "asd", 11, "lastIndexOf(str, subStr, anIndex)", 11},
        new Object[]{"asd", "dsa", 11, "lastIndexOf(str, subStr, anIndex)", -1},
        new Object[]{"asd", "dsa", 11, "lastIndexOf(str, subStr)", -1},
    };
  }

  @Test(dataProvider = "dataProvider_function_indexOf")
  public void function_indexOf(String str, String subStr, int anIndex, String expr, int result) throws Exception {

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("str", new Str(str));
    scope.put("subStr", new Str(subStr));
    scope.put("anIndex", new Num(BigDecimal.decimal(anIndex)));

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("str", new AstStr());
          put("subStr", new AstStr());
          put("anIndex", new AstNum());
        }}
    ));

    m.registerTree("main", "assign(numRes, " + expr + ")");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("numRes")).isEqualTo(new Num(BigDecimal.valueOf(result)));

  }

  @DataProvider
  public Object[][] dataProvider_function_subs() {
    return new Object[][]{
        new Object[]{"абвгдеёжз", 0, 3, "subs(str, indexFrom, indexTo)", "абв"},
        new Object[]{"абвгдеёжз", 2, 7, "subs(str, indexFrom, indexTo)", "вгдеё"},
        new Object[]{"абвгдеёжз", 2, 7, "subs(str, indexFrom)", "вгдеёжз"},
        new Object[]{"абвгдеёжз", 2, 7000, "subs(str, indexFrom, indexTo)", "вгдеёжз"},
        new Object[]{"абвгдеёжз", 2000, 7000, "subs(str, indexFrom, indexTo)", ""},
        new Object[]{"абвгдеёжз", 0, 7000, "subs(str, indexFrom, indexTo)", "абвгдеёжз"},
        new Object[]{"абвгдеёжз", 2000, 7000, "subs(str, indexFrom)", ""},
        new Object[]{"абвгдеёжз", -2000, 7000, "subs(str, indexFrom)", "абвгдеёжз"},
    };
  }

  @Test(dataProvider = "dataProvider_function_subs")
  public void function_subs(String str, int indexFrom, int indexTo, String expr, String expectedStr) throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("str", new Str(str));
    scope.put("indexFrom", new Num(BigDecimal.decimal(indexFrom)));
    scope.put("indexTo", new Num(BigDecimal.decimal(indexTo)));

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("str", new AstStr());
          put("indexFrom", new AstNum());
          put("indexTo", new AstNum());
        }}
    ));

    m.registerTree("main", "assign(strRes, " + expr + ")");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("strRes")).isEqualTo(new Str(expectedStr));
  }

  @DataProvider
  public Object[][] dataProvider_functions_startsWith_endsWith() {
    return new Object[][]{
        new Object[]{"asd hi", "asd", "startsWith(str, sub)", true},
        new Object[]{"asd hi", "dsa", "startsWith(str, sub)", false},
        new Object[]{"hi asd", "asd", "endsWith(str, sub)", true},
        new Object[]{"hi asd", "dsa", "endsWith(str, sub)", false},
    };
  }

  @Test(dataProvider = "dataProvider_functions_startsWith_endsWith")
  public void functions_startsWith_endsWith(String str, String sub, String expr, boolean result) throws Exception {

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("str", new Str(str));
    scope.put("sub", new Str(sub));

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("str", new AstStr());
          put("sub", new AstStr());
        }}
    ));

    m.registerTree("main", "assign(flag, " + expr + ")");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("flag")).isEqualTo(new Bool(result));
  }

  @DataProvider
  public Object[][] dataProvider_some_expr() {
    return new Object[][]{
        new Object[]{"12 + 0.3", new Num(BigDecimal.decimal(12 + 0.3))},
        new Object[]{"12 - 0.3", new Num(BigDecimal.decimal(12 - 0.3))},
        new Object[]{"12 / 0.3", new Num(BigDecimal.decimal(12 / 0.3))},
        new Object[]{"12 * 0.3", new Num(BigDecimal.decimal(3.6))},
        new Object[]{"4 + 12 * 0.3", new Num(BigDecimal.decimal(4 + 3.6))},
        new Object[]{"4 + 12 / 0.3", new Num(BigDecimal.decimal(44))},
        new Object[]{"(4 + 12) * 0.3", new Num(BigDecimal.decimal(4.8))},
        new Object[]{"2 + emptyValue", new Num(BigDecimal.decimal(2))},
    };
  }

  @Test(dataProvider = "dataProvider_some_expr")
  public void some_expr(String expr, DtType expectedResult) throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("emptyValue", new AstNum());
        }}
    ));

    m.registerTree("main", "assign(numRes, " + expr + ")");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("numRes")).isEqualTo(expectedResult);
  }

  @Test
  public void function_if_true() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));

    m.registerTree("main", "group(\n" +
        "  assign(boolValue, true()), \n" +
        "  assign(num, 1), \n" +
        "  assign(num, if(boolValue, 2+3, 3+7))\n" +
        ")");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("num")).isEqualTo(new Num(BigDecimal.decimal(5)));
  }

  @Test
  public void function_if_false() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));

    m.registerTree("main", "group(\n" +
        "  assign(boolValue, false()),\n" +
        "  assign(num, 1),\n" +
        "  assign(num, if(boolValue, 2+3, 3+7))\n" +
        ")");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("num")).isEqualTo(new Num(BigDecimal.decimal(10)));
  }

  @Test
  public void roundUp2Digits() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));

    m.registerTree("main", "group(assign(toRound, 156.673), assign(numRes, round(toRound, 2)))");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("numRes")).isEqualTo(new Num(BigDecimal.decimal(156.67)));

  }

  @Test
  public void roundUp1Digit() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));

    m.registerTree("main", "group(assign(toRound, 166.67), assign(numRes, round(toRound, 1)))");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("numRes")).isEqualTo(new Num(BigDecimal.decimal(166.7)));

  }

  @Test
  public void floor() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));

    m.registerTree("main", "assign(numRes, floor(12.34))");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();

    assertThat(outScope.get("numRes")).isEqualTo(new Num(BigDecimal.decimal(12)));

  }

  @Test(expectedExceptions = ErrorException.class, expectedExceptionsMessageRegExp = ".*No procedure asd.*")
  public void noProcedure() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));

    m.registerTree("main", "procedure(asd)");

    m.compile();
    m.executeTree("main", scope);
  }

  @Test
  public void len2() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("client.account.0.cpdBeginDate", new Dat(LocalDate.now().minusDays(10)));
    scope.put("client.account.0.transaction.0.loanUsage", new Str("N"));
    scope.put("client.account.0.paidPrincipalAmt", new Num(BigDecimal.valueOf(10)));
    scope.put("client.account.0.paidInterest", new Num(BigDecimal.valueOf(10)));

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(TestData$.MODULE$.client());

    m.registerTree("main", "group(assign(strRes, \"len \"~len(client.account)))");

    m.compile();
    DtExecuteResult result = m.executeTree("main", scope);

    System.out.println(result.messages);
    SortedMap<String, DtType> outScope = m.getScope();
    assertThat(outScope.get("strRes")).isEqualTo(new Str("len 1"));
  }

  @Test
  public void lenEmpty() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("client.account.0.cpdBeginDate", new Dat(LocalDate.now().minusDays(10)));
    scope.put("client.account.1.cpdBeginDate", new Dat(LocalDate.now().minusDays(11)));
    scope.put("client.account.2.cpdBeginDate", new Dat(LocalDate.now().minusDays(12)));

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(TestData$.MODULE$.client());

    m.registerTree("main", "group(\n" +
        "  message(\"len(client.account) = \" ~ len(client.account)),\n" +
        "  message(\"Hello world\")\n" +
        ")");

    m.compile();
    DtExecuteResult result = m.executeTree("main", scope);

    assertThat(result.messages).isNotNull();
    assertThat(result.messages).hasSize(2);
    assertThat(result.messages.get(0)).isEqualTo("len(client.account) = 3");
    assertThat(result.messages.get(1)).isEqualTo("Hello world");
  }

  @Test
  public void log() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));

    m.registerTree("main", "group(assign(l10, log(10, 100)), assign(l2, log(2, 50)))");

    m.compile();
    m.executeTree("main", scope);
    SortedMap<String, DtType> outScope = m.getScope();
    assertThat(outScope.get("l10")).isEqualTo(new Num(BigDecimal.decimal(2)));
    assertThat(((Num) outScope.get("l2")).num().toDouble()).isGreaterThan(5).isLessThan(6);
  }

  @Test
  public void complexObject() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("in.accountIndex", new Num(BigDecimal.int2bigDecimal(1)));
    scope.put("in.overdueAmt", new Num(BigDecimal.int2bigDecimal(10)));

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(TestData$.MODULE$.client1());

    m.registerTree("main", "group (\n" +
        "    Object(in, Num(idx), Num(accountIndex), Num(overdueAmt), Str(accountId)),\n" +
        "    assign (overdueId, generateId()),\n" +
        "    assign(client1.account[in.accountIndex].overdueFines[id:overdueId].overdueFine_amount, in.overdueAmt),\n" +
        "    assign(client1.account[in.accountIndex].overdueFines[id:overdueId].overdueFine_date, businessDay()),\n" +
        "    assign(client1.account[in.accountIndex].overdueFines[id:overdueId].overdueFine_status, \"U\")\n" +
        ")");

    m.compile();
    m.executeTree("main", scope);
    SortedMap<String, DtType> outScope = m.getScope();
    assertThat(outScope.get("client1.account.1.overdueFines.0.id")).isEqualTo(outScope.get("overdueId")).isNotNull();
    assertThat(outScope.get("client1.account.1.overdueFines.0.overdueFine_amount")).isEqualTo(new Num(BigDecimal.valueOf(10)));
    assertThat(outScope.get("client1.account.1.overdueFines.0.overdueFine_date")).isNotNull();
    assertThat(outScope.get("client1.account.1.overdueFines.0.overdueFine_status")).isEqualTo(new Str("U"));
  }

  @Test
  public void dateCondition() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("payPlanDueDate", new Dat(LocalDate.now().plusMonths(1))); // 0, -1

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("payPlanDueDate", new AstDat());
        }}
    ));

    m.registerTree("main", "condition(\n" +
        "    case(payPlanDueDate>businessDay(), assign(flag, true()))\n" +
        ")");

    m.compile();
    m.executeTree("main", scope);
    SortedMap<String, DtType> outScope = m.getScope();
    assertThat(outScope.get("flag")).isEqualTo(new Bool(true));
  }

  @Test
  public void createNewArrayElementOfUnsuccessfulSearch1() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(TestData$.MODULE$.person());

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("person.address.0.street", Str.apply("Baker"));
    scope.put("person.address.1.street", Str.apply("Newton"));

    m.registerTree("main", "assign(person.address[street:\"Boston\"].title, \"Hello\")");
    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("person.address.2.title").toString()).isEqualTo("Str(Hello)");
    assertThat(m.getScope().get("person.address.2.street").toString()).isEqualTo("Str(Boston)");
  }

  @Test
  public void createNewArrayElementOfUnsuccessfulSearch2() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(TestData$.MODULE$.client2());

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("client.account.0.street", Str.apply("Baker"));
    scope.put("client.account.0.transaction.0.type", Str.apply("More money"));

    m.registerTree("main", "assign(\n" +
        "  client.account[street:\"Baker\"].transaction[type:\"Left\"].message, \"Hello World\"\n" +
        ")");
    m.compile();
    m.executeTree("main", scope);

    see(scope);

    assertThat(m.getScope().get("client.account.0.transaction.1.type").toString()).isEqualTo("Str(Left)");
    assertThat(m.getScope().get("client.account.0.transaction.1.message").toString()).isEqualTo("Str(Hello World)");
  }

  @Test(expectedExceptions = kz.greetgo.dt.ErrorException.class)
  public void clientAccountCannotBeCreated() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(TestData$.MODULE$.client3());

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("client.account.0.asd", Str.apply("asd value"));

    m.registerTree("main", "assign(client.account[asd:\"left value\"].title, \"Hello\")");
    m.compile();
    m.executeTree("main", scope);
  }

  @Test
  public void testWithoutDash() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(TestData$.MODULE$.client4());

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("client.account.0.product.ChargeAdvanceRepayment.Value_AddedServiceFee", new Str("CurrentTerm"));
    scope.put("in.idx", new Num(BigDecimal.valueOf(0)));

    m.registerTree("main", "group(\n" +
        "  Object(in, Num(idx), Num(accountIndex), Num(overdueAmt), Str(accountId)),\n" +
        "  assign(valueInTree, client.account[in.idx].product.ChargeAdvanceRepayment.Value_AddedServiceFee)\n" +
        ")");
    m.compile();
    m.executeTree("main", scope);

    assertThat(m.getScope().get("valueInTree").toString()).isEqualTo("Str(CurrentTerm)");
  }

  @Test
  public void divide0() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.registerTree("div", "group(\n" +
        "  assign(z, 1), \n" +
        "  assign(z, 2), \n" +
        "  assign(z, 1/0)\n" +
        ")");
    m.registerTree("main", "procedure(div)");

    try {
      m.compile();
      m.executeTree("main", scope);
    } catch (ExprException e) {
      assertThat(e.getMessage()).contains("3:"); // Just statement resolution
      assertThat(e.getMessage()).contains("Division by zero");
      assertThat(e.getMessage()).contains("(div)");
      return;
    } catch (ArithmeticException e) { // For DtTranslator
      assertThat(e.getMessage()).contains("Division by zero");
      return;
    }

    Assertions.fail("Must be error");
  }

  @Test
  public void complexCase() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.registerTree("main", "condition(case(1=1, assign(x,1), assign(y,2) ))");
    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    assertThat(outScope.get("x")).isEqualTo(new Num(BigDecimal.valueOf(1)));
    assertThat(outScope.get("y")).isEqualTo(new Num(BigDecimal.valueOf(2)));
  }

  @Test
  public void zeroTest() throws Exception {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.registerTree("main", "group(\n" +
        "  assign(n, 0),\n" +
        "  condition(\n" +
        "    case(n=0, assign(x, 1))\n" +
        "  ),\n" +
        "  condition(\n" +
        "    case(n=0.00, assign(y, 2))\n" +
        "  )\n" +
        ")");
    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    assertThat(outScope.get("x")).isEqualTo(new Num(BigDecimal.valueOf(1)));
    assertThat(outScope.get("y")).isEqualTo(new Num(BigDecimal.valueOf(2)));
  }

  @Test
  public void format() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));

    m.registerTree("main", "assign(strRes, format(date(\"2001-02-03\"), \"dd/MM/yyyy\"))");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("strRes")).isEqualTo(new Str("03/02/2001"));

  }

  @Test
  public void setDay() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));

    m.registerTree("main", "group(\n" +
        "  assign(currDate, date(\"2016-01-02\")),\n" +
        "  assign(currDateResult, setDay(currDate, 20))\n" +
        ")");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("currDateResult")).isEqualTo(new Dat(LocalDate.of(2016, 1, 20)));
  }

  @Test
  public void divide() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));

    m.registerTree("main", "group(\n" +
        "  assign(feeAmt, 311.23),\n" +
        "  assign(txnAmt, 1),\n" +
        "  assign(date, date(\"2010-01-01\")),\n" +
        "  assign(dayNumber, day(date)),\n" +
        "  assign(numberOfDaysInMonth, daysInMonth(date)),\n" +
        "  assign(txnAmt, txnAmt + feeAmt * dayNumber/numberOfDaysInMonth)\n" +
        ")\n");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    Num txnAmt = (Num) outScope.get("txnAmt");

    assertThat(txnAmt.num().doubleValue()).isEqualTo(11.03967, Offset.offset(0.001));
  }

  @Test
  public void foreach() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("num", new AstNum());
        }}
    ));

    m.registerTree("main", "foreach(i, 1, 10, assign(num, num+2))");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("num")).isEqualTo(new Num(BigDecimal.valueOf(20)));
  }

  @Test
  public void foreach_continue() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("num", new AstNum());
        }}
    ));

    m.registerTree("main", "foreach(i, 1, 10, group(\n" +
        "  condition(\n" +
        "    case(i=3, continue())\n" +
        "  ),\n" +
        "  assign(num, num+2))\n" +
        ")");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("num")).isEqualTo(new Num(BigDecimal.valueOf(18)));
  }

  @Test
  public void foreach_break() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("num", new AstNum());
        }}
    ));

    m.registerTree("main", "foreach(i, 1, 10, group(\n" +
        "  condition(\n" +
        "    case(i=3, break())\n" +
        "  ), \n" +
        "  assign(num, num+2))\n" +
        ")");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);

    assertThat(outScope.get("num")).isEqualTo(new Num(BigDecimal.valueOf(4)));
  }

  private static DtType dtValue(String type, String value) {
    if (value == null) return null;
    switch (type) {
      case "NUM":
        return new Num(BigDecimal.javaBigDecimal2bigDecimal(new java.math.BigDecimal(value.replace(',', '.'))));
      case "BOOL":
        return new Bool("1".equals(value));
      case "STR":
        return new Str(value);
      case "DATE":
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
        return new Dat(LocalDate.parse(value, f));
      default: throw new IllegalArgumentException();
    }
  }

  private static AstType astType(String type) {
    switch (type) {
      case "NUM":
        return new AstNum();
      case "BOOL":
        return new AstBool();
      case "STR":
        return new AstStr();
      case "DATE":
        return new AstDat();
      default: throw new IllegalArgumentException();
    }
  }

  private static void structure(AstObj root, List<String> path, AstType type) {
    HashMap<String, AstType> map = root.obj();
    String head = path.get(0);

    if (path.size() > 1) { // recirsive
      if (path.get(1).matches("\\d+")) { // arr
        AstArr arr = (AstArr)map.get(head);
        if (arr == null) {
          arr = new AstArr(new AstObj(new HashMap<>()));
          map.put(head, arr);
        }
        structure((AstObj)arr.elem(), path.subList(2, path.size()), type);
      } else { // obj
        AstObj obj = (AstObj)map.get(head);
        if (obj == null) {
          obj = new AstObj(new HashMap<>());
          map.put(head, obj);
        }
        structure(obj, path.subList(1, path.size()), type);
      }
    } else { // end
      map.put(head, type);
    }
  }

  @Test
  public void foreach_break_complex() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();
    AstObj root = new AstObj(new HashMap<>());
    String data = resource("foreach_break_complex.txt");
    for (String line : Arrays.asList(data.split("\n"))) {
      String[] fields = line.split("\t");
      String name = fields[0];
      String type = fields[1];
      String value = fields.length < 3 ? null : fields[2];
      DtType dtValue = dtValue(type, value);
      structure(root, Arrays.asList(name.split("\\.")), astType(type));
    }

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("client", root);
          put("in", TestData$.MODULE$.in());
        }}
    ));

    m.registerUserProcedure("calculateCurrentDebt",
        "group(\n" +
            "    assign(debtAmt, 0),\n" +
            "    assign(currentTermDebt, 0),\n" +
            "\n" +
            "    foreach (j, 0, len(client.account[in.accountIndex].payPlan)-1,\n" +
            "        group(\n" +
            "            condition(\n" +
            "                case(client.account[in.accountIndex].payPlan[j].hasPaid = true() |\n" +
            "                      client.account[in.accountIndex].payPlan[j].payPlanDueDate > businessDay() |\n" +
            "                      client.account[in.accountIndex].payPlan[j].actual = false() |\n" +
            "                      client.account[in.accountIndex].payPlan[j].planStatus=\"C\",\n" +
            "                      group(\n" +
            "                        continue()\n" +
            "                      )\n" +
            "                )\n" +
            "            ), //We need only previous and non paid\n" +
            "\n" +
            "            assign(termDebt,0),\n" +
            "\n" +
            "            message( client.account[in.accountIndex].payPlan[j].payPlanDueDate ~ \" : \" ~ businessDay()),\n" +
            "\n" +
            "            group( //Principal\n" +
            "                assign(a, client.account[in.accountIndex].payPlan[j].remainingPrincipal),\n" +
            "                assign(termDebt, termDebt+a)\n" +
            "            ),\n" +
            "\n" +
            "\n" +
            "            group( //Interest\n" +
            "                assign(a, client.account[in.accountIndex].payPlan[j].remainingInterest),\n" +
            "                assign(termDebt, termDebt+a)\n" +
            "            ),\n" +
            "\n" +
            "\n" +
            "            group( //Life Insurance Amt\n" +
            "                assign(a, client.account[in.accountIndex].payPlan[j].remainingLifeInsurance),\n" +
            "                assign(termDebt, termDebt+a)\n" +
            "            ),\n" +
            "\n" +
            "            group( //Service Fee Amt\n" +
            "                assign(a, client.account[in.accountIndex].payPlan[j].remainingServiceFee),\n" +
            "                assign(termDebt, termDebt+a)\n" +
            "            ),\n" +
            "\n" +
            "            group( //Agent Fee Amt\n" +
            "                assign(a, client.account[in.accountIndex].payPlan[j].remainingAgentFee),\n" +
            "                assign(termDebt, termDebt+a)\n" +
            "            ),\n" +
            "\n" +
            "            group( //Replace Svc Fee\n" +
            "                assign(a, client.account[in.accountIndex].payPlan[j].remainingReplaceSvcFee),\n" +
            "                assign(termDebt, termDebt+a)\n" +
            "            ),\n" +
            "\n" +
            "            group( //Prepay Pkg Fee\n" +
            "                assign(a, client.account[in.accountIndex].payPlan[j].remainingPrepayPkgFee),\n" +
            "                assign(termDebt, termDebt+a)\n" +
            "            ),\n" +
            "\n" +
            "            condition(\n" +
            "                case(\n" +
            "                    client.account[in.accountIndex].payPlan[j].termNumber = client.account[in.accountIndex].termToPay,\n" +
            "\n" +
            "                    assign(currentTermDebt, termDebt)\n" +
            "                )\n" +
            "            ),\n" +
            "\n" +
            "            assign(debtAmt, debtAmt +termDebt)\n" +
            "\n" +
            "        )\n" +
            "    ),\n" +
            "\n" +
            "     //Calculate overdueFines\n" +
            "\n" +
            "     assign(fineAmt, 0),\n" +
            "     assign(afterTransactionFine, 0),\n" +
            "\n" +
            "     foreach(j, 0, len(client.account[in.accountIndex].overdueFine)-1,\n" +
            "         condition(\n" +
            "             case(client.account[in.accountIndex].overdueFine[j].status = \"U\" &\n" +
            "                  client.account[in.accountIndex].payPlan[termNumber:client.account[in.accountIndex].overdueFine[j].term].planStatus = \"C\",\n" +
            "                group(\n" +
            "                  continue()\n" +
            "                )\n" +
            "             ),\n" +
            "             case(\n" +
            "                 client.account[in.accountIndex].overdueFine[j].status = \"U\",\n" +
            "                     group(\n" +
            "                         assign(a, client.account[in.accountIndex].overdueFine[j].remainingAmount),\n" +
            "                         assign(fineAmt, fineAmt+a),\n" +
            "\n" +
            "                         condition(\n" +
            "                            case(\n" +
            "                                isDefined(dateFrom)&client.account[in.accountIndex].overdueFine[j].date>=dateFrom,\n" +
            "                                assign(afterTransactionFine, afterTransactionFine+a)\n" +
            "                            )\n" +
            "                         )\n" +
            "                     )\n" +
            "             )\n" +
            "         )\n" +
            "     ),\n" +
            "\n" +
            "      group(\n" +
            "          assign(a, fineAmt),\n" +
            "          assign(debtAmt, debtAmt+a)\n" +
            "      )\n" +
            ")\n");

    m.registerUserProcedure("closeAccount",
        "group(\n" +
            "    assign(client.account[in.accountIndex].closedDate, businessDay()),\n" +
            "    assign(client.account[in.accountIndex].nextStmtDate, empty()),\n" +
            "    assign(client.account[in.accountIndex].batchDate, empty()),\n" +
            "    assign(client.account[in.accountIndex].remainingTerms,0),\n" +
            "\n" +
            "    condition(\n" +
            "        case(client.account[in.accountIndex].sumOverpaid>0,\n" +
            "            assign(client.account[in.accountIndex].returnOverpayment,  true()),\n" +
            "            assign(client.account[in.accountIndex].batchDate, businessDay())\n" +
            "        )\n" +
            "    )\n" +
            ")\n");

    m.registerUserProcedure("moveNextStmtDate",
        "group(\n" +
            "    condition(\n" +
            "        case(client.account[in.accountIndex].remainingTerms=0, //finished\n" +
            "          group (\n" +
            "            procedure(closeAccount),\n" +
            "            assign (client.account[in.accountIndex].contractStatus, \"F\")\n" +
            "          )\n" +
            "        ),\n" +
            "        case(client.account[in.accountIndex].remainingTerms>0\n" +
            "            & client.account[in.accountIndex].hasAdvancedClearing!=true()\n" +
            "            & client.account[in.accountIndex].returnOverpayment!=true(),\n" +
            "            group(\n" +
            "                assign(client.account[in.accountIndex].nextStmtDate, client.account[in.accountIndex].payPlan[client.account[in.accountIndex].termToPay].payPlanDueDate) ,\n" +
            "                assign(client.account[in.accountIndex].pmtDueDate, client.account[in.accountIndex].nextStmtDate) , //For MS Loan\n" +
            "                assign(client.account[in.accountIndex].batchDate, client.account[in.accountIndex].nextStmtDate),\n" +
            "\n" +
            "                assign(client.account[in.accountIndex].graceDate, client.account[in.accountIndex].pmtDueDate + day() * client.account[in.accountIndex].product.BusinessParameters.GracePeriod)\n" +
            "            )\n" +
            "        )\n" +
            "    )\n" +
            ")\n");

    m.registerUserProcedure("addFine",
        "group(\n" +
            "    condition(\n" +
            "        case( //grace period is finished\n" +
            "            businessDay() >= client.account[in.accountIndex].graceDate & client.account[in.accountIndex].hasCompulsoryCleaning = false(),\n" +
            "            group(\n" +
            "                assign(cpd, client.account[in.accountIndex].cpdDays),\n" +
            "                assign(dpd, client.account[in.accountIndex].dpdDays),\n" +
            "\n" +
            "                condition(\n" +
            "                    case(client.account[in.accountIndex].product.FineCalculationType = \"CPD\",\n" +
            "                        assign(overdueDays, cpd)\n" +
            "                    ),\n" +
            "                     case(client.account[in.accountIndex].product.FineCalculationType = \"DPD\",\n" +
            "                        assign(overdueDays, dpd)\n" +
            "                    )\n" +
            "                ),\n" +
            "\n" +
            "                group(//Fines\n" +
            "                    assign(length, len(client.account[in.accountIndex].product.Fine)),\n" +
            "\n" +
            "                    foreach (i, 0, length - 1, //TODO Run throw all fines (if one day batch is down fine will not be added)\n" +
            "                        group(\n" +
            "                            condition(\n" +
            "                                case(\n" +
            "                                    (dpd>0\n" +
            "                                    &\n" +
            "                                    overdueDays >= client.account[in.accountIndex].product.Fine[i].Min\n" +
            "                                    &\n" +
            "                                    overdueDays <= client.account[in.accountIndex].product.Fine[i].Max)\n" +
            "                                    &\n" +
            "                                    (client.account[in.accountIndex].lastMaxFine != client.account[in.accountIndex].product.Fine[i].Max\n" +
            "                                    |\n" +
            "                                    isEmpty(client.account[in.accountIndex].lastMaxFine)),\n" +
            "\n" +
            "                                        group(\n" +
            "                                            assign(client.account[in.accountIndex].lastMaxFine, client.account[in.accountIndex].product.Fine[i].Max),\n" +
            "\n" +
            "                                            assign(fineAmount, client.account[in.accountIndex].product.Fine[i].parameter),\n" +
            "\n" +
            "                                            assign(message, \"Overdue for overdueDays = \" ~ overdueDays),\n" +
            "                                            assign(overdueFineType,\"fine\"),\n" +
            "                                            procedure(generateFine)\n" +
            "                                        )\n" +
            "                                )\n" +
            "                            )\n" +
            "                        )\n" +
            "                    )\n" +
            "                ),\n" +
            "\n" +
            "                group(//Agent fines\n" +
            "                    assign(length, len(client.account[in.accountIndex].product.agentFine)),\n" +
            "\n" +
            "                    foreach (i, 0, length - 1, //TODO Run throw all fines (if one day batch is down fine will not be added)\n" +
            "                        group(\n" +
            "                            condition(\n" +
            "                                case(\n" +
            "                                    (dpd>0\n" +
            "                                    &\n" +
            "                                    overdueDays >= client.account[in.accountIndex].product.agentFine[i].Min\n" +
            "                                    &\n" +
            "                                    overdueDays <= client.account[in.accountIndex].product.agentFine[i].Max)\n" +
            "                                    &\n" +
            "                                    (client.account[in.accountIndex].lastMaxAgentFine != client.account[in.accountIndex].product.agentFine[i].Max\n" +
            "                                    |\n" +
            "                                    isEmpty(client.account[in.accountIndex].lastMaxAgentFine)),\n" +
            "\n" +
            "                                        group(\n" +
            "                                            assign(client.account[in.accountIndex].lastMaxAgentFine, client.account[in.accountIndex].product.agentFine[i].Max),\n" +
            "\n" +
            "                                            assign(fineAmount, client.account[in.accountIndex].product.agentFine[i].parameter),\n" +
            "\n" +
            "                                            assign(message, \"Overdue for overdueDays = \" ~ overdueDays),\n" +
            "\n" +
            "                                            assign(overdueFineType,\"agentFine\"),\n" +
            "                                            procedure(generateFine)\n" +
            "                                        )\n" +
            "                                )\n" +
            "                            )\n" +
            "                        )\n" +
            "                    )\n" +
            "                ),\n" +
            "\n" +
            "                group( //Penalties\n" +
            "                    assign(length, len(client.account[in.accountIndex].product.Penalty)),\n" +
            "                    assign(overdueLength,len(client.account[in.accountIndex].overdueFine)-1),\n" +
            "               assign(roundTwoTotal, 0 ),\n" +
            "               assign(roundSixTotal, 0 ),\n" +
            "\n" +
            "                    foreach (i, 0, length - 1, //TODO Run throw all fines (if one day batch is down fine will not be added)\n" +
            "                        group(\n" +
            "                            condition(\n" +
            "                                case(client.account[in.accountIndex].product.Penalty[i].CalculationRule = \"BalanceOfPrincipal\",\n" +
            "                                    assign(amt, client.account[in.accountIndex].loanAmt - client.account[in.accountIndex].totalPaidPrincipalAmt)\n" +
            "                                ),\n" +
            "                                case(client.account[in.accountIndex].product.Penalty[i].CalculationRule = \"LoanPrincipal\",\n" +
            "                                    assign(amt, client.account[in.accountIndex].loanAmt)\n" +
            "                                ),\n" +
            "                                case(client.account[in.accountIndex].product.Penalty[i].CalculationRule = \"OverdueLoanPrincipal\",\n" +
            "                                    //Total overdue principal\n" +
            "                                    assign(amt, 0),\n" +
            "                                    foreach (j, client.account[in.accountIndex].paidTerms, client.account[in.accountIndex].termToPay-1,\n" +
            "                                        assign(amt, amt+client.account[in.accountIndex].payPlan[j].remainingPrincipal)\n" +
            "                                    )\n" +
            "                                ),\n" +
            "                                case(client.account[in.accountIndex].product.Penalty[i].CalculationRule = \"TotalOverdueAmount\",\n" +
            "                                    //Total overdue amount\n" +
            "                                    assign(amt, 0),\n" +
            "                                    foreach (j, client.account[in.accountIndex].paidTerms, client.account[in.accountIndex].termToPay-1,\n" +
            "                                        assign(amt, amt+client.account[in.accountIndex].payPlan[j].remainingTotal)\n" +
            "                                    ),\n" +
            "\n" +
            "                                    foreach(j, 0, overdueLength,\n" +
            "                                        condition(\n" +
            "                                            case(\n" +
            "                                                client.account[in.accountIndex].overdueFine[j].status = \"U\",\n" +
            "                                                    group( //Prepay Pkg Fee\n" +
            "                                                        assign(a, client.account[in.accountIndex].overdueFine[j].remainingAmount),\n" +
            "                                                        assign(amt, amt+a)\n" +
            "                                                    )\n" +
            "                                            )\n" +
            "                                        )\n" +
            "                                    )\n" +
            "                                )\n" +
            "                            ),\n" +
            "\n" +
            "                            condition(\n" +
            "                                case(isDefined(client.account[in.accountIndex].product.Penalty[i].divider)=false(), assign(client.account[in.accountIndex].product.Penalty[i].divider,1))\n" +
            "                            ),\n" +
            "\n" +
            "                            condition(\n" +
            "                                case(client.account[in.accountIndex].product.Penalty[i].HowToCalculate = \"FixedAmount\",\n" +
            "                                    assign(fineAmount, client.account[in.accountIndex].product.Penalty[i].CalculationParameter)),\n" +
            "                                case(client.account[in.accountIndex].product.Penalty[i].HowToCalculate = \"BaseRatio\",\n" +
            "                                   group (\n" +
            "                                       assign( fineAmount, round((client.account[in.accountIndex].product.Penalty[i].CalculationParameter / client.account[in.accountIndex].product.Penalty[i].divider) * amt, 2))\n" +
            "                                    )\n" +
            "                               )\n" +
            "                            ),\n" +
            "\n" +
            "                            condition(\n" +
            "                                case(client.account[in.accountIndex].product.Penalty[i].Accumulative = true() & fineAmount>0,\n" +
            "                                  group (\n" +
            "                                    assign(overdueFineType, client.account[in.accountIndex].product.Penalty[i].type),\n" +
            "                                    assign(roundSixTotal, roundSixTotal+round((client.account[in.accountIndex].product.Penalty[i].CalculationParameter / client.account[in.accountIndex].product.Penalty[i].divider) * amt, 6)),\n" +
            "                                    assign(roundTwoTotal, roundTwoTotal+round((client.account[in.accountIndex].product.Penalty[i].CalculationParameter / client.account[in.accountIndex].product.Penalty[i].divider) * amt, 2)),\n" +
            "                                    procedure(generateFine),\n" +
            "                                    condition(\n" +
            "                                        case(dictValueText(\"PenaltyType\",\"agentCode\", client.account[in.accountIndex].product.Penalty[i].type)=\"notAgent\",\n" +
            "                                          assign(myOverdueId, overdueFineId)\n" +
            "                                        )\n" +
            "                                    )\n" +
            "                                  )\n" +
            "                                ),\n" +
            "                                case(1=1,\n" +
            "                                    group (\n" +
            "                                        condition(\n" +
            "                                            case((!isDefined(client.account[in.accountIndex].product.Penalty[i].penaltyUsedTerm) | client.account[in.accountIndex].product.Penalty[i].penaltyUsedTerm!=client.account[in.accountIndex].termToPay) & fineAmount>0,\n" +
            "                                                group(\n" +
            "                                                    assign(client.account[in.accountIndex].product.Penalty[i].penaltyUsedTerm, client.account[in.accountIndex].termToPay),\n" +
            "                                                    assign(overdueFineType, client.account[in.accountIndex].product.Penalty[i].type),\n" +
            "                                                assign(roundSixTotal, roundSixTotal+round((client.account[in.accountIndex].product.Penalty[i].CalculationParameter / client.account[in.accountIndex].product.Penalty[i].divider) * amt, 6)),\n" +
            "                                                assign(roundTwoTotal, roundTwoTotal+round((client.account[in.accountIndex].product.Penalty[i].CalculationParameter / client.account[in.accountIndex].product.Penalty[i].divider) * amt, 2)),\n" +
            "                                                    procedure(generateFine),\n" +
            "                                                    condition(\n" +
            "                                                        case(dictValueText(\"PenaltyType\",\"agentCode\", client.account[in.accountIndex].product.Penalty[i].type)=\"notAgent\",\n" +
            "                                                          assign(myOverdueId, overdueFineId)\n" +
            "                                                        )\n" +
            "                                                    )\n" +
            "                                                )\n" +
            "                                            )\n" +
            "                                        )\n" +
            "                                    )\n" +
            "                                )\n" +
            "                            )\n" +
            "                        )\n" +
            "                    ),\n" +
            "                    condition(\n" +
            "                       case(roundSixTotal>roundTwoTotal & isDefined(myOverdueId),\n" +
            "                           assign(client.account[in.accountIndex].overdueFine[id:myOverdueId].remainingAmount,client.account[in.accountIndex].overdueFine[id:myOverdueId].remainingAmount+round(roundSixTotal-roundTwoTotal+0.004,2))\n" +
            "                        )\n" +
            "                    )\n" +
            "                )\n" +
            "            )\n" +
            "        )\n" +
            "    )\n" +
            ")\n");

    m.registerUserProcedure("generateFine",
        "group(\n" +
            "   //create new overdueFine\n" +
            "\n" +
            "   assign(overdueFineId, generateId()),\n" +
            "    assign(client.account[in.accountIndex].overdueFine[id:overdueFineId].date, businessDay()),\n" +
            "    assign(client.account[in.accountIndex].overdueFine[id:overdueFineId].remainingAmount, fineAmount),\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].overdueFine[id:overdueFineId].paidAmount, 0),\n" +
            "    assign(client.account[in.accountIndex].overdueFine[id:overdueFineId].status, \"U\"), //status = unPaid\n" +
            "    assign(client.account[in.accountIndex].overdueFine[id:overdueFineId].term, client.account[in.accountIndex].termToPay),\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].totalRemainingAmt,\n" +
            "       client.account[in.accountIndex].totalRemainingAmt +\n" +
            "       client.account[in.accountIndex].overdueFine[id:overdueFineId].remainingAmount),\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].overdueFine[id:overdueFineId].message, message),\n" +
            "    assign(client.account[in.accountIndex].overdueFine[id:overdueFineId].actual, true()),\n" +
            "    assign(client.account[in.accountIndex].overdueFine[id:overdueFineId].overdueFineType, overdueFineType)\n" +
            ")\n");

    m.registerUserProcedure("createCompensationForCurrentTerm",
        "group(\n" +
            "    condition(\n" +
            "        case(client.account[in.accountIndex].lastCompensationTerm!=client.account[in.accountIndex].currentTerm,\n" +
            "           group (\n" +
            "               assign(loanUsage, \"U\"),\n" +
            "               assign(orderStatus, \"N\"),\n" +
            "\n" +
            "\n" +
            "               assign(termTotalAmount, client.account[in.accountIndex].payPlan[termNumber:client.account[in.accountIndex].currentTerm].remainingTotal),\n" +
            "\n" +
            "               group(\n" +
            "                   assign(length, len(client.account[in.accountIndex].overdueFine)),\n" +
            "                   assign(totalPenaltySum, 0),\n" +
            "\n" +
            "                   foreach (i, 0, length - 1,\n" +
            "                       group(\n" +
            "                           condition(\n" +
            "                               case(\"fine\" = client.account[in.accountIndex].overdueFine[i].overdueFineType & waitingTransactionIsClaim=false(),\n" +
            "                                   group(\n" +
            "                                       assign(totalPenaltySum, totalPenaltySum+client.account[in.accountIndex].overdueFine[i].remainingAmount)\n" +
            "                                   )\n" +
            "                               ),\n" +
            "                               case(dictValueText(\"PenaltyType\",\"agentCode\", client.account[in.accountIndex].overdueFine[i].overdueFineType)=\"notAgent\" & waitingTransactionIsClaim=false(),\n" +
            "                                   group(\n" +
            "                                       assign(totalPenaltySum, totalPenaltySum+client.account[in.accountIndex].overdueFine[i].remainingAmount)\n" +
            "                                   )\n" +
            "                               )\n" +
            "                           )\n" +
            "                       )\n" +
            "                   )\n" +
            "               ),\n" +
            "\n" +
            "                assign (agentServiceFee, client.account[in.accountIndex].payPlan[termNumber:client.account[in.accountIndex].currentTerm].remainingAgentFee),\n" +
            "               //Agent service fee should not be included\n" +
            "               assign(generateTransaction_transactionAmount, termTotalAmount+totalPenaltySum-agentServiceFee), // calculate here\n" +
            "               //After payment is received, then add agent service fee before common payment\n" +
            "\n" +
            "\n" +
            "               assign(client.account[in.accountIndex].payPlan[termNumber:client.account[in.accountIndex].termToPay].planStatus, \"C\"),\n" +
            "               assign(term, client.account[in.accountIndex].termToPay),\n" +
            "               assign(settleFeeType, \"CompensateAmt\"),\n" +
            "               assign(txnDirection, \"-\"),\n" +
            "\n" +
            "               assign(client.account[in.accountIndex].lastCompensationTerm, client.account[in.accountIndex].currentTerm),\n" +
            "\n" +
            "                group(\n" +
            "                    assign(settleId,generateId()),\n" +
            "                    assign(client.account[in.accountIndex].settlePlatform[id:settleId].settleFeeType,\"CompensateAmt\"),\n" +
            "                    assign(client.account[in.accountIndex].settlePlatform[id:settleId].settleAmt, generateTransaction_transactionAmount),\n" +
            "                    assign(client.account[in.accountIndex].settlePlatform[id:settleId].postDate, businessDay()),\n" +
            "                    assign(client.account[in.accountIndex].settlePlatform[id:settleId].settleTerm, client.account[in.accountIndex].termToPay),\n" +
            "                    assign(client.account[in.accountIndex].settlePlatform[id:settleId].settleSend, true()),\n" +
            "                    assign(client.account[in.accountIndex].settlePlatform[id:settleId].settleTxnDirection, \"-\")\n" +
            "                ),\n" +
            "                \n" +
            "               procedure(generateTransaction)\n" +
            "            )\n" +
            "        )\n" +
            "    )\n" +
            ")\n");

    m.registerUserProcedure("clearOverdueDate",
        "group(\n" +
            "    condition(\n" +
            "        case(debtAmt<=client.account[in.accountIndex].product.ToleranceValue.CPDOverdueToleranceValue,\n" +
            "            assign(client.account[in.accountIndex].cpdBeginDate, empty())\n" +
            "        )\n" +
            "    ),\n" +
            "\n" +
            "    condition(\n" +
            "        case(debtAmt<=client.account[in.accountIndex].product.ToleranceValue.DPDOverdueToleranceValue,\n" +
            "            assign(client.account[in.accountIndex].overdueDate, empty())\n" +
            "        )\n" +
            "    ),\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].cpdDays, 0),\n" +
            "    assign(client.account[in.accountIndex].dpdDays, 0),\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].lastMaxFine, empty()),\n" +
            "    assign(client.account[in.accountIndex].lastMaxAgentFine, empty())\n" +
            "\n" +
            ")\n");

    m.registerUserProcedure("checkWaitingTransactions",
        "group(\n" +
            "    foreach(transIndex, 0, len(client.account[in.accountIndex].transaction) - 1,\n" +
            "        condition(\n" +
            "            case (client.account[in.accountIndex].transaction[transIndex].orderStatus = \"W\"\n" +
            "            &\n" +
            "            client.account[in.accountIndex].transaction[transIndex].loanUsage != \"U\"\n" +
            "            &\n" +
            "            daysBetween(businessDay(), client.account[in.accountIndex].transaction[transIndex].createTime)>=5,\n" +
            "                group(\n" +
            "                    assign(client.account[in.accountIndex].transaction[transIndex].orderStatus, \"E\"),\n" +
            "                    assign(client.account[in.accountIndex].transaction[transIndex].responseMessage, \"Out of 5 days\"),\n" +
            "                    assign(client.account[in.accountIndex].transaction[transIndex].failureAmt, client.account[in.accountIndex].transaction[transIndex].txnAmt),\n" +
            "\n" +
            "                    condition(\n" +
            "                        case(\n" +
            "                        client.account[in.accountIndex].hasAdvancedClearing = true(),\n" +
            "                            group(\n" +
            "                                assign(client.account[in.accountIndex].hasAdvancedClearing, false()),\n" +
            "                                assign(client.account[in.accountIndex].batchDate, client.account[in.accountIndex].nextStmtDate)\n" +
            "                            )\n" +
            "                        )\n" +
            "                    )\n" +
            "                )\n" +
            "            )\n" +
            "        )\n" +
            "    )\n" +
            ")\n");

    m.registerUserProcedure("createCompulsoryClearingPlan",
        "group(\n" +
            "    assign(termToPay, client.account[in.accountIndex].currentTerm),\n" +
            "\n" +
            "    condition(\n" +
            "        case(client.account[in.accountIndex].hasManualCompulsoryClearing=true(),\n" +
            "            group(\n" +
            "                assign(termToPay, client.account[in.accountIndex].manualComClearingTerm),\n" +
            "                assign(client.account[in.accountIndex].manualCompulsoryClearingPlanChanged, true()),\n" +
            "                condition(\n" +
            "                    case(client.account[in.accountIndex].nextStmtDate>client.account[in.accountIndex].manualComClearingAppliedDate,\n" +
            "                      assign(client.account[in.accountIndex].nextStmtDate, client.account[in.accountIndex].manualComClearingAppliedDate)\n" +
            "                    )\n" +
            "                )\n" +
            "            )\n" +
            "        )\n" +
            "    ),\n" +
            "\n" +
            "    assign(sameAsDueDay,false()),\n" +
            "    condition(\n" +
            "      case(client.account[in.accountIndex].termToPay>0,\n" +
            "        condition(\n" +
            "          case(client.account[in.accountIndex].payPlan[termNumber:client.account[in.accountIndex].termToPay].payPlanDueDate=businessDay(),\n" +
            "            group(\n" +
            "              assign(sameAsDueDay,true())\n" +
            "            )\n" +
            "          )\n" +
            "        )\n" +
            "      )\n" +
            "    ),\n" +
            "\n" +
            "    condition(\n" +
            "      case(client.account[in.accountIndex].hasManualCompulsoryClearing=true(),\n" +
            "        condition(\n" +
            "          case(client.account[in.accountIndex].manualComClearingTerm>1,\n" +
            "            condition(\n" +
            "              case(client.account[in.accountIndex].payPlan[termNumber:client.account[in.accountIndex].manualComClearingTerm-1].payPlanDueDate=client.account[in.accountIndex].manualComClearingAppliedDate,\n" +
            "                group(\n" +
            "                  assign(sameAsDueDay,true())\n" +
            "                )\n" +
            "              )\n" +
            "            )\n" +
            "          ),\n" +
            "          case(daysBetween(client.account[in.accountIndex].payPlan[0].payPlanDueDate-month(), client.account[in.accountIndex].manualComClearingAppliedDate)<=client.account[in.accountIndex].product.Hesitation.value,\n" +
            "            assign(sameAsDueDay,true())\n" +
            "          )\n" +
            "        )\n" +
            "      )\n" +
            "    ),\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].totalRemainingAmt, client.account[in.accountIndex].totalRemainingAmt - client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal),\n" +
            "    assign(client.account[in.accountIndex].totalRemInterest, client.account[in.accountIndex].totalRemInterest - client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingInterest),\n" +
            "\n" +
            "    condition(\n" +
            "        case(\n" +
            "            client.account[in.accountIndex].payPlan[termNumber:termToPay].payPlanDueDate>businessDay(), //Change date and interest of payPlan\n" +
            "            group(\n" +
            "                condition(\n" +
            "                    case(termToPay-1>0,\n" +
            "                        group(\n" +
            "                            assign(x, daysBetween(client.account[in.accountIndex].payPlan[termNumber:termToPay-1].payPlanDueDate, businessDay())),\n" +
            "                            condition(\n" +
            "                                case(client.account[in.accountIndex].hasManualCompulsoryClearing=true(),\n" +
            "                                    group(\n" +
            "                                      assign(x, daysBetween(client.account[in.accountIndex].payPlan[termNumber:termToPay-1].payPlanDueDate, client.account[in.accountIndex].manualComClearingAppliedDate))\n" +
            "                                    )\n" +
            "                                )\n" +
            "                            ),\n" +
            "                            assign(y, daysBetween(client.account[in.accountIndex].payPlan[termNumber:termToPay-1].payPlanDueDate, client.account[in.accountIndex].payPlan[termNumber:termToPay].payPlanDueDate))\n" +
            "                        )\n" +
            "                    ),\n" +
            "                    case(1=1,\n" +
            "                        group(\n" +
            "                            assign(x, daysBetween(client.account[in.accountIndex].payPlan[0].payPlanDueDate - month() - day(), businessDay())),\n" +
            "                            condition(\n" +
            "                                case(client.account[in.accountIndex].hasManualCompulsoryClearing=true(),\n" +
            "                                    group(\n" +
            "                                      assign(x, daysBetween(client.account[in.accountIndex].payPlan[0].payPlanDueDate - month() - day(), client.account[in.accountIndex].manualComClearingAppliedDate))\n" +
            "                                    )\n" +
            "                                )\n" +
            "                            ),\n" +
            "                            assign(y, daysBetween(client.account[in.accountIndex].payPlan[0].payPlanDueDate-month(), client.account[in.accountIndex].payPlan[0].payPlanDueDate))\n" +
            "                        )\n" +
            "                    )\n" +
            "                ),\n" +
            "\n" +
            "                condition(\n" +
            "                    case(isEmpty(x)&isEmpty(y),\n" +
            "                        group(\n" +
            "                            assign(x, 1),\n" +
            "                            assign(y, 1)\n" +
            "                        )\n" +
            "                    )\n" +
            "                ),\n" +
            "\n" +
            "                assign(remainingInterest, client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingInterest),\n" +
            "\n" +
            "                assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal, client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal-remainingInterest),\n" +
            "\n" +
            "                assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingInterest, round(remainingInterest*x/y,2)),\n" +
            "\n" +
            "                assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal, client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal+client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingInterest),\n" +
            "\n" +
            "                assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].payPlanDueDate, businessDay())\n" +
            "            )\n" +
            "        )\n" +
            "    ),\n" +
            "\n" +
            "    //New interest Amount\n" +
            "    assign(client.account[in.accountIndex].totalRemInterest, client.account[in.accountIndex].totalRemInterest + client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingInterest),\n" +
            "\n" +
            "    foreach(i, termToPay+1, len(client.account[in.accountIndex].payPlan), //Sum remaining principal\n" +
            "        group(\n" +
            "            assign(a, client.account[in.accountIndex].payPlan[termNumber:i].remainingPrincipal),\n" +
            "\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingPrincipal, client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingPrincipal+a),\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal, client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal+a),\n" +
            "\n" +
            "\n" +
            "            //Remaining total recalculate\n" +
            "            assign(client.account[in.accountIndex].totalRemainingAmt, client.account[in.accountIndex].totalRemainingAmt - client.account[in.accountIndex].payPlan[termNumber:i].remainingTotal),\n" +
            "            assign(client.account[in.accountIndex].totalRemInstallmentFee, client.account[in.accountIndex].totalRemInstallmentFee - client.account[in.accountIndex].payPlan[termNumber:i].remainingInstallmentFee),\n" +
            "            assign(client.account[in.accountIndex].totalRemLifeInsuranceFee, client.account[in.accountIndex].totalRemLifeInsuranceFee - client.account[in.accountIndex].payPlan[termNumber:i].remainingLifeInsurance),\n" +
            "            assign(client.account[in.accountIndex].totalRemReplaceServiceFee, client.account[in.accountIndex].totalRemReplaceServiceFee - client.account[in.accountIndex].payPlan[termNumber:i].remainingReplaceSvcFee),\n" +
            "            assign(client.account[in.accountIndex].totalRemPrepayPkgFee, client.account[in.accountIndex].totalRemPrepayPkgFee - client.account[in.accountIndex].payPlan[termNumber:i].remainingPrepayPkgFee),\n" +
            "            assign(client.account[in.accountIndex].totalRemServiceFee, client.account[in.accountIndex].totalRemServiceFee - client.account[in.accountIndex].payPlan[termNumber:i].remainingServiceFee),\n" +
            "            assign(client.account[in.accountIndex].totalRemInterest, client.account[in.accountIndex].totalRemInterest - client.account[in.accountIndex].payPlan[termNumber:i].remainingInterest),\n" +
            "            assign(client.account[in.accountIndex].totalRemAgentFee, client.account[in.accountIndex].totalRemAgentFee - client.account[in.accountIndex].payPlan[termNumber:i].remainingAgentFee),\n" +
            "\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:i].actual, false())\n" +
            "        )\n" +
            "    ),\n" +
            "\n" +
            "    condition(\n" +
            "      case(sameAsDueDay,\n" +
            "        group(\n" +
            "            //Remaining total recalculate\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal, client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal-client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingInstallmentFee),\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal, client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal-client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingLifeInsurance),\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal, client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal-client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingReplaceSvcFee),\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal, client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal-client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingPrepayPkgFee),\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal, client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal-client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingServiceFee),\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal, client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal-client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingInterest),\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal, client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal-client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingAgentFee),\n" +
            "\n" +
            "            assign(client.account[in.accountIndex].totalRemInstallmentFee, client.account[in.accountIndex].totalRemInstallmentFee - client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingInstallmentFee),\n" +
            "            assign(client.account[in.accountIndex].totalRemLifeInsuranceFee, client.account[in.accountIndex].totalRemLifeInsuranceFee - client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingLifeInsurance),\n" +
            "            assign(client.account[in.accountIndex].totalRemReplaceServiceFee, client.account[in.accountIndex].totalRemReplaceServiceFee - client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingReplaceSvcFee),\n" +
            "            assign(client.account[in.accountIndex].totalRemPrepayPkgFee, client.account[in.accountIndex].totalRemPrepayPkgFee - client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingPrepayPkgFee),\n" +
            "            assign(client.account[in.accountIndex].totalRemServiceFee, client.account[in.accountIndex].totalRemServiceFee - client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingServiceFee),\n" +
            "            assign(client.account[in.accountIndex].totalRemInterest, client.account[in.accountIndex].totalRemInterest - client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingInterest),\n" +
            "            assign(client.account[in.accountIndex].totalRemAgentFee, client.account[in.accountIndex].totalRemAgentFee - client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingAgentFee),\n" +
            "\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingInstallmentFee, 0),\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingLifeInsurance, 0),\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingReplaceSvcFee, 0),\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingPrepayPkgFee, 0),\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingServiceFee, 0),\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingInterest, 0),\n" +
            "            assign(client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingAgentFee, 0)\n" +
            "        )\n" +
            "      )\n" +
            "    ),\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].totalRemainingAmt, client.account[in.accountIndex].totalRemainingAmt + client.account[in.accountIndex].payPlan[termNumber:termToPay].remainingTotal),\n" +
            "\n" +
            "    condition(\n" +
            "        case(client.account[in.accountIndex].batchDate>businessDay(),\n" +
            "            assign(client.account[in.accountIndex].batchDate,businessDay())\n" +
            "        )\n" +
            "    ),\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].hasCompulsoryCleaning, true()),\n" +
            "    assign(client.account[in.accountIndex].manualCompulsoryClearingPlanChanged, true()),\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].remainingTerms, 1),\n" +
            "    assign(client.account[in.accountIndex].termToPay, termToPay),\n" +
            "\n" +
            "    condition(\n" +
            "        case(client.account[in.accountIndex].product.IsPartnerProduct = true(),\n" +
            "            group(\n" +
            "                assign(totalDebtWithoutAgent,0),\n" +
            "                group(\n" +
            "                    foreach(p,1,client.account[in.accountIndex].termToPay,\n" +
            "                        group(\n" +
            "                            condition(\n" +
            "                                case(client.account[in.accountIndex].payPlan[termNumber:p].hasPaid=false() & client.account[in.accountIndex].payPlan[termNumber:p].planStatus!=\"C\",\n" +
            "                                    group (\n" +
            "                                        assign(client.account[in.accountIndex].payPlan[termNumber:p].planStatus,\"X\"),\n" +
            "\n" +
            "                                        assign(client.account[in.accountIndex].payPlan[termNumber:p].remainingTotal, round(client.account[in.accountIndex].payPlan[termNumber:p].remainingTotal\n" +
            "                                            - client.account[in.accountIndex].payPlan[termNumber:p].remainingAgentFee, 2)),\n" +
            "\n" +
            "                                        assign(client.account[in.accountIndex].payPlan[termNumber:p].paidAgentFee,0.00),\n" +
            "\n" +
            "                                        group (\n" +
            "                                            assign(client.account[in.accountIndex].totalRemAgentFee, round(client.account[in.accountIndex].totalRemAgentFee - client.account[in.accountIndex].payPlan[termNumber:p].remainingAgentFee, 2)),\n" +
            "\n" +
            "                                            assign(client.account[in.accountIndex].totalRemainingAmt, round(client.account[in.accountIndex].totalRemainingAmt - client.account[in.accountIndex].payPlan[termNumber:p].remainingAgentFee, 2))\n" +
            "                                        ),\n" +
            "\n" +
            "                                        assign(client.account[in.accountIndex].payPlan[termNumber:p].remainingAgentFee, 0.00),\n" +
            "                                        assign(totalDebtWithoutAgent,totalDebtWithoutAgent + client.account[in.accountIndex].payPlan[termNumber:p].remainingTotal)\n" +
            "                                   )\n" +
            "                                )\n" +
            "                            )\n" +
            "                        )\n" +
            "                    )\n" +
            "                ),\n" +
            "\n" +
            "\n" +
            "               group(\n" +
            "                   foreach (i, 0, len(client.account[in.accountIndex].overdueFine) - 1,\n" +
            "                       group(\n" +
            "                           condition(\n" +
            "                               case(\"fine\" = client.account[in.accountIndex].overdueFine[i].overdueFineType & client.account[in.accountIndex].payPlan[termNumber:client.account[in.accountIndex].overdueFine[i].term].planStatus!=\"C\",\n" +
            "                                   group(\n" +
            "                                       assign(totalDebtWithoutAgent, totalDebtWithoutAgent+client.account[in.accountIndex].overdueFine[i].remainingAmount)\n" +
            "                                   )\n" +
            "                               ),\n" +
            "                               case(dictValueText(\"PenaltyType\",\"agentCode\", client.account[in.accountIndex].overdueFine[i].overdueFineType)=\"notAgent\" &  client.account[in.accountIndex].payPlan[termNumber:client.account[in.accountIndex].overdueFine[i].term].planStatus!=\"C\",\n" +
            "                                   group(\n" +
            "                                       assign(totalDebtWithoutAgent, totalDebtWithoutAgent+client.account[in.accountIndex].overdueFine[i].remainingAmount)\n" +
            "                                   )\n" +
            "                               )\n" +
            "                           )\n" +
            "                       )\n" +
            "                   )\n" +
            "               ),\n" +
            "\n" +
            "                assign(loanUsage, \"C\"),\n" +
            "                assign(orderStatus, \"N\"),\n" +
            "\n" +
            "                assign(settleFeeType, \"ClaimAmt\"),\n" +
            "                assign(term, client.account[in.accountIndex].termToPay),\n" +
            "                assign(txnDirection, \"-\"),\n" +
            "\n" +
            "                assign(generateTransaction_transactionAmount, totalDebtWithoutAgent), // calculate here\n" +
            "\n" +
            "                group(\n" +
            "                    assign(settleId,generateId()),\n" +
            "                    assign(client.account[in.accountIndex].settlePlatform[id:settleId].settleFeeType,\"ClaimAmt\"),\n" +
            "                    assign(client.account[in.accountIndex].settlePlatform[id:settleId].settleAmt, generateTransaction_transactionAmount),\n" +
            "                    assign(client.account[in.accountIndex].settlePlatform[id:settleId].postDate, businessDay()),\n" +
            "                    assign(client.account[in.accountIndex].settlePlatform[id:settleId].settleTerm, client.account[in.accountIndex].termToPay),\n" +
            "                    assign(client.account[in.accountIndex].settlePlatform[id:settleId].settleSend, true()),\n" +
            "                    assign(client.account[in.accountIndex].settlePlatform[id:settleId].settleTxnDirection, \"-\")\n" +
            "                ),\n" +
            "\n" +
            "                procedure(generateTransaction),\n" +
            "\n" +
            "                procedure(closeAccount)\n" +
            "            )\n" +
            "        )\n" +
            "    )\n" +
            ")\n");

    m.registerUserProcedure("moveCurrentTerm",
        "group(\n" +
            "    foreach(k,0,len(client.account[in.accountIndex].payPlan)-1,\n" +
            "        condition(\n" +
            "            case(businessDay() >= client.account[in.accountIndex].payPlan[k].payPlanDueDate\n" +
            "                &\n" +
            "                client.account[in.accountIndex].hasCompulsoryCleaning = false()\n" +
            "                &\n" +
            "                client.account[in.accountIndex].hasAdvancedClearing = false()\n" +
            "                &\n" +
            "                client.account[in.accountIndex].manualCompulsoryClearingPlanChanged != true(), //If claim exists do not change currentTerm.\n" +
            "                    group(\n" +
            "                        assign(client.account[in.accountIndex].currentTerm, client.account[in.accountIndex].payPlan[k].termNumber+1),\n" +
            "                        assign(client.account[in.accountIndex].termToPay, k+1),\n" +
            "\n" +
            "                        condition(\n" +
            "                            case(client.account[in.accountIndex].currentTerm>client.account[in.accountIndex].loanTerm,\n" +
            "                                assign(client.account[in.accountIndex].currentTerm, client.account[in.accountIndex].loanTerm)\n" +
            "                            )\n" +
            "                        )\n" +
            "                    )\n" +
            "            )\n" +
            "        )\n" +
            "    )\n" +
            ")\n");

    m.registerUserProcedure("debtCheck",
        "group(\n" +
            "    procedure(calculateCurrentDebt),\n" +
            "\n" +
            "    assign(paidTerms,client.account[in.accountIndex].paidTerms),\n" +
            "\n" +
            "    condition(\n" +
            "        case(debtAmt > 0 , //If there is overdue\n" +
            "            group(\n" +
            "                condition(\n" +
            "                    case (isEmpty(client.account[in.accountIndex].cpdBeginDate),\n" +
            "                        assign(client.account[in.accountIndex].cpdBeginDate, client.account[in.accountIndex].nextStmtDate)\n" +
            "                    ),\n" +
            "                    case(client.account[in.accountIndex].payPlan[client.account[in.accountIndex].termToPay-1].payPlanDueDate=businessDay()\n" +
            "                        & currentTermDebt=debtAmt,\n" +
            "                        group(\n" +
            "                            assign(client.account[in.accountIndex].nextStmtDate, client.account[in.accountIndex].payPlan[client.account[in.accountIndex].termToPay-1].payPlanDueDate) ,\n" +
            "                            assign(client.account[in.accountIndex].pmtDueDate, client.account[in.accountIndex].nextStmtDate) , //For MS Loan\n" +
            "                            assign(client.account[in.accountIndex].batchDate, client.account[in.accountIndex].nextStmtDate),\n" +
            "                            assign(client.account[in.accountIndex].graceDate, client.account[in.accountIndex].pmtDueDate + day() * client.account[in.accountIndex].product.BusinessParameters.GracePeriod),\n" +
            "\n" +
            "                            assign(client.account[in.accountIndex].cpdBeginDate, client.account[in.accountIndex].nextStmtDate)\n" +
            "                        )\n" +
            "                    )\n" +
            "                ),\n" +
            "\n" +
            "                assign(client.account[in.accountIndex].cpdDays, daysBetween(client.account[in.accountIndex].cpdBeginDate, businessDay())),\n" +
            "\n" +
            "                condition(\n" +
            "                            case(debtAmt<=client.account[in.accountIndex].product.ToleranceValue.CPDOverdueToleranceValue\n" +
            "                            &\n" +
            "                            client.account[in.accountIndex].termToPay!=client.account[in.accountIndex].loanTerm,\n" +
            "                                group(\n" +
            "                                    assign(client.account[in.accountIndex].cpdBeginDate, empty()),\n" +
            "                                    assign(client.account[in.accountIndex].cpdDays, 0),\n" +
            "                                    assign(client.account[in.accountIndex].lastMaxFine, empty()),\n" +
            "                                    assign(client.account[in.accountIndex].lastMaxAgentFine, empty())\n" +
            "                                )\n" +
            "                            )\n" +
            "                ),\n" +
            "                group(\n" +
            "                    condition(\n" +
            "                        case (isEmpty(client.account[in.accountIndex].overdueDate), //To switch dpd according to payments\n" +
            "                            group(\n" +
            "                                 assign(client.account[in.accountIndex].overdueDate, client.account[in.accountIndex].nextStmtDate)\n" +
            "                            )\n" +
            "                        ),\n" +
            "                        case(client.account[in.accountIndex].payPlan[client.account[in.accountIndex].termToPay-1].payPlanDueDate=businessDay()\n" +
            "                            & currentTermDebt=debtAmt,\n" +
            "                            group(\n" +
            "                                assign(client.account[in.accountIndex].overdueDate, client.account[in.accountIndex].nextStmtDate)\n" +
            "                            )\n" +
            "                        )\n" +
            "                    ),\n" +
            "\n" +
            "                    condition(\n" +
            "                        case(isDefined(client.account[in.accountIndex].overdueDate),\n" +
            "                            assign(client.account[in.accountIndex].dpdDays, daysBetween(client.account[in.accountIndex].overdueDate, businessDay()))\n" +
            "                        )\n" +
            "                    ),\n" +
            "\n" +
            "                    condition(//Does it works for last term???\n" +
            "                        case(debtAmt<=client.account[in.accountIndex].product.ToleranceValue.DPDOverdueToleranceValue\n" +
            "                            &\n" +
            "                            client.account[in.accountIndex].termToPay!=client.account[in.accountIndex].loanTerm,\n" +
            "                            group(\n" +
            "                                assign(client.account[in.accountIndex].overdueDate, empty()),\n" +
            "                                assign(client.account[in.accountIndex].dpdDays, 0),\n" +
            "\n" +
            "                                procedure(moveNextStmtDate)\n" +
            "                            )\n" +
            "                        ),\n" +
            "                        case(waitingTransactionIsClaim=false() & client.account[in.accountIndex].hasManualCompulsoryClearing!=true(),\n" +
            "                            group(\n" +
            "                                procedure(addFine)\n" +
            "                            )\n" +
            "                        )\n" +
            "                    )\n" +
            "                )\n" +
            "            )\n" +
            "        ),\n" +
            "        case(1 = 1 ,\n" +
            "            //If there is no overdue\n" +
            "            group(\n" +
            "                procedure(clearOverdueDate),\n" +
            "                condition(\n" +
            "                    case((client.account[in.accountIndex].hasAdvancedClearing & isDefined(client.account[in.accountIndex].advanceClearingTerm))\n" +
            "                    | client.account[in.accountIndex].hasCompulsoryCleaning = true()\n" +
            "                    | (client.account[in.accountIndex].hasManualCompulsoryClearing = true() & client.account[in.accountIndex].manualCompulsoryClearingPlanChanged=true()),\n" +
            "                        group(\n" +
            "                            procedure(closeAccount)\n" +
            "                        )\n" +
            "                    ),\n" +
            "\n" +
            "                    case(1=1,\n" +
            "                        procedure(moveNextStmtDate)\n" +
            "                    )\n" +
            "                )\n" +
            "            )\n" +
            "        )\n" +
            "    )\n" +
            ")\n");

    m.registerUserProcedure("generateTransaction",
        "group(\n" +
            "\n" +
            "    assign(transId, generateId()),\n" +
            "\n" +
            "    //Account info\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].org, \"000000000001\"),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].acctNbr, client.account[in.accountIndex].acctNbr),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].acctType, client.account[in.accountIndex].acctType), //\"E|人民币独立小额贷款账户\"\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].currency, \"156\"), //Currency code for MS Loan\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].openBankId, client.account[in.accountIndex].ddBankBranch),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].openBankName, client.account[in.accountIndex].ddBankName),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].subBank, client.account[in.accountIndex].owningBranch),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].cardNo, client.account[in.accountIndex].ddBankAcctNbr),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].usrName, client.account[in.accountIndex].ddBankAcctName),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].state, client.account[in.accountIndex].ddBankProvince),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].city, client.account[in.accountIndex].ddBankCity),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].mobileNumber, client.account[in.accountIndex].mobileNo),\n" +
            "\n" +
            "    //Client Info\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].certId, client.idNo),\n" +
            "\n" +
            "    group(\n" +
            "                condition(\n" +
            "                    case(client.idType=\"P\",\n" +
            "                        assign(client.account[in.accountIndex].transaction[id:transId].certType, client.idType)\n" +
            "                    ),\n" +
            "\n" +
            "                    case(client.tmp.idType=\"R\",\n" +
            "                        assign(client.account[in.accountIndex].transaction[id:transId].certType, \"04\")\n" +
            "                    ),\n" +
            "\n" +
            "                    case(client.tmp.idType=\"H\",\n" +
            "                        assign(client.account[in.accountIndex].transaction[id:transId].certType, \"05\")\n" +
            "                    ),\n" +
            "\n" +
            "                    case(1=1,\n" +
            "                        assign(client.account[in.accountIndex].transaction[id:transId].certType, \"06\")\n" +
            "                    )\n" +
            "                ),\n" +
            "\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].certId, client.idNo)\n" +
            "    ),\n" +
            "\n" +
            "    //Transaction info\n" +
            "    //Incoming variables\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].txnAmt, generateTransaction_transactionAmount),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].orderStatus, orderStatus),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].loanUsage, loanUsage),\n" +
            "\n" +
            "    condition(\n" +
            "        case(isDefined(duebillNo), //When create transaction manualy!\n" +
            "            assign(client.account[in.accountIndex].transaction[id:transId].dueBillNo, duebillNo)\n" +
            "        )\n" +
            "    ),\n" +
            "\n" +
            "    condition(\n" +
            "        case(isDefined(client.tmp.refNbr),\n" +
            "            assign(client.account[in.accountIndex].transaction[id:transId].refNbr, client.tmp.refNbr)\n" +
            "        )\n" +
            "    ),\n" +
            "\n" +
            "    condition(\n" +
            "        case(isDefined(client.tmp.inputSource),\n" +
            "            assign(client.account[in.accountIndex].transaction[id:transId].inputSource, client.tmp.inputSource)\n" +
            "        )\n" +
            "    ),\n" +
            "\n" +
            "    condition(\n" +
            "        case(isDefined(client.tmp.serviceSn),\n" +
            "            assign(client.account[in.accountIndex].transaction[id:transId].serviceSn, client.tmp.serviceSn)\n" +
            "        )\n" +
            "    ),\n" +
            "\n" +
            "    condition(\n" +
            "        case(isDefined(client.tmp.serviceId),\n" +
            "            assign(client.account[in.accountIndex].transaction[id:transId].serviceId, client.tmp.serviceId)\n" +
            "        )\n" +
            "    ),\n" +
            "\n" +
            "    condition(\n" +
            "        case(isDefined(client.tmp.requestTime),\n" +
            "            assign(client.account[in.accountIndex].transaction[id:transId].requestTime, client.tmp.requestTime)\n" +
            "        )\n" +
            "    ),\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].acqId, client.account[in.accountIndex].acqId),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].flag, \"00\"),\n" +
            "\n" +
            "    condition(\n" +
            "        case(loanUsage=\"L\", //Loan release\n" +
            "            group(\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].txnType, \"AgentDebit\"),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].commandType, \"SPA\"),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].purpose, \"放款申请\"),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].payBizCode, \"1\"), //Credit -0, Debit - 1\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].matchInd, \"Y\") //Need to check in batch?\n" +
            "            )\n" +
            "        ),\n" +
            "        case(loanUsage=\"N\", //Normal deduction\n" +
            "            group(\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].txnType, \"AgentCredit\"),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].commandType, \"SDB\"),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].purpose, \"正常扣款\"),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].payBizCode, \"0\"), //Credit -0, Debit - 1\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].matchInd, \"Y\") //Need to check in batch?\n" +
            "            )\n" +
            "        ),\n" +
            "        case(loanUsage=\"O\", //Overdue deduction\n" +
            "            group(\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].txnType, \"AgentCredit\"),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].commandType, \"SDB\"),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].purpose, \"逾期扣款\"),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].payBizCode, \"0\"), //Credit -0, Debit - 1\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].matchInd, \"Y\") //Need to check in batch?\n" +
            "            )\n" +
            "        ),\n" +
            "        case(loanUsage=\"D\", //Overpayment return\n" +
            "            group(\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].txnType, \"AgentDebit\"),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].purpose, \"溢缴款转出\"),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].payBizCode, \"1\"), //Credit -0, Debit - 1\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].matchInd, \"Y\") //Need to check in batch?\n" +
            "            )\n" +
            "        ),\n" +
            "        case(loanUsage=\"M\", //Advanced repayment\n" +
            "            group(\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].purpose, \"提前结清\"),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].payBizCode, \"0\"), //Credit -0, Debit - 1\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].txnType, \"AgentCredit\"),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].matchInd, \"Y\") //Need to check in batch?\n" +
            "            )\n" +
            "        ),\n" +
            "        case(loanUsage=\"C\" | loanUsage=\"U\", //Claim and compensation\n" +
            "            group(\n" +
            "                assign(partnerCode, client.account[in.accountIndex].product.Partner.Partners),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].txnType, \"AgentCredit\"),\n" +
            "\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].flag, \"01\"),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].usrName, client.customername),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].cardNo, client.account[in.accountIndex].ddBankAcctNbr),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].certType, \"06\"),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].certId, client.idNo),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].openBankId, client.account[in.accountIndex].ddBankBranch),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].openBankName, client.account[in.accountIndex].ddBankName),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].subBank, client.account[in.accountIndex].owningBranch),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].state, client.account[in.accountIndex].ddBankProvince),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].city, client.account[in.accountIndex].ddBankCity),\n" +
            "\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].cooperationId, client.account[in.accountIndex].acqId),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].applicationNo, client.account[in.accountIndex].applicationNo),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].productCd, client.account[in.accountIndex].productCd),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].settleFeeType, settleFeeType),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].term, term),\n" +
            "                assign(client.account[in.accountIndex].transaction[id:transId].txnDirection, txnDirection)\n" +
            "            )\n" +
            "        )\n" +
            "    ),\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].comparedInd, false()), assign(client.account[in.accountIndex].transaction[id:transId].term, client.account[in.accountIndex].currentTerm),\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].channelId, client.account[in.accountIndex].custSource),\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].contrNbr, client.account[in.accountIndex].contrNbr),\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].successAmt, 0),\n" +
            "    condition(\n" +
            "        case(orderStatus=\"S\",\n" +
            "            assign(client.account[in.accountIndex].transaction[id:transId].successAmt, generateTransaction_transactionAmount)\n" +
            "        )\n" +
            "    ),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].failureAmt, 0),\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].priv1, \"T110E5\"), //PrivateField\n" +
            "\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].businessDate, businessDay()),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].sendTime, businessDay()),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].createTime, businessDay()),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].setupDate, businessDay()),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].orderTime, businessDay()),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].optDatetime, businessDay()),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].cardType, \"0\"),\n" +
            "    assign(client.account[in.accountIndex].transaction[id:transId].payChannelId, \"1\")\n" +
            ")\n");

    m.registerTree("main", "group(\n" +
        "    assign(generateTransaction_transactionAmount,0),\n" +
        "    assign(debtAmt,0),\n" +
        "    assign(currentTermDebt,0),\n" +
        "    assign(termDebt,0),\n" +
        "    assign(fineAmt,0),\n" +
        "    assign(afterTransactionFine,0),\n" +
        "    assign(termToPay,0),\n" +
        "    assign(sameAsDueDay,true()),\n" +
        "    assign(totalDebtWithoutAgent,0),\n" +
        "    assign(loanUsage,\"\"),\n" +
        "    assign(orderStatus,\"\"),\n" +
        "    assign(settleFeeType,\"\"),\n" +
        "    assign(term,0),\n" +
        "    assign(txnDirection,\"\"),\n" +
        "    assign(settleId,\"\"),\n" +
        "    assign(transId,\"\"),\n" +
        "    assign(fineAmount,0),\n" +
        "    assign(overdueFineType,\"\"),\n" +
        "    assign(cpd,0),\n" +
        "    assign(dpd,0),\n" +
        "    assign(overdueDays,0),\n" +
        "    assign(amt,0),\n" +
        "    assign(overdueFineId,\"\"),\n" +
        "    assign(termTotalAmount,0),\n" +
        "    assign(totalPenaltySum,0),\n" +
        "    assign(agentServiceFee,0),\n" +
        "    assign(duebillNo,\"\"),\n" +
        "    assign(dateFrom,businessDay()),\n" +
        "    assign(removeDate,businessDay()),\n" +
        "    assign(sum,0),\n" +
        "    assign(checkCode,\"\"),\n" +
        "    assign(message,\"\"),\n" +
        "\n" +
        "    assign(generateTransaction_transactionAmount,empty()),\n" +
        "    assign(debtAmt,empty()),\n" +
        "    assign(currentTermDebt,empty()),\n" +
        "    assign(termDebt,empty()),\n" +
        "    assign(fineAmt,empty()),\n" +
        "    assign(afterTransactionFine,empty()),\n" +
        "    assign(termToPay,empty()),\n" +
        "    assign(sameAsDueDay,empty()),\n" +
        "    assign(totalDebtWithoutAgent,empty()),\n" +
        "    assign(loanUsage,empty()),\n" +
        "    assign(orderStatus,empty()),\n" +
        "    assign(settleFeeType,empty()),\n" +
        "    assign(term,empty()),\n" +
        "    assign(txnDirection,empty()),\n" +
        "    assign(settleId,empty()),\n" +
        "    assign(transId,empty()),\n" +
        "    assign(fineAmount,empty()),\n" +
        "    assign(overdueFineType,empty()),\n" +
        "    assign(cpd,empty()),\n" +
        "    assign(dpd,empty()),\n" +
        "    assign(overdueDays,empty()),\n" +
        "    assign(amt,empty()),\n" +
        "    assign(overdueFineId,empty()),\n" +
        "    assign(termTotalAmount,empty()),\n" +
        "    assign(totalPenaltySum,empty()),\n" +
        "    assign(agentServiceFee,empty()),\n" +
        "    assign(duebillNo, empty()),\n" +
        "    assign(dateFrom,empty()),\n" +
        "    assign(removeDate,empty()),\n" +
        "    assign(sum,empty()),\n" +
        "    assign(checkCode,empty()),\n" +
        "    assign(message,empty()),\n" +
        "\n" +
        "    foreach(i, 0, len(in.accountIndexes) - 1,\n" +
        "        group (\n" +
        "            assign(in.accountIndex, in.accountIndexes[i].value),\n" +
        "\n" +
        "            condition(\n" +
        "              case(isDefined(client.account[in.accountIndex].closedDate) & isEmpty(client.account[in.accountIndex].batchDate),\n" +
        "                  break()\n" +
        "\n" +
        "              )\n" +
        "            ),\n" +
        "            \n" +
        "            assign(waitingTransactionExists, false()),\n" +
        "            assign(waitingTransactionIsClaim, false()),\n" +
        "\n" +
        "            foreach(transIndex, 0, len(client.account[in.accountIndex].transaction) - 1,\n" +
        "                condition(\n" +
        "                    case (client.account[in.accountIndex].transaction[transIndex].orderStatus = \"W\",\n" +
        "                      assign(waitingTransactionExists, true()),\n" +
        "\n" +
        "                      condition(case(\n" +
        "                        client.account[in.accountIndex].transaction[transIndex].loanUsage = \"C\" | client.account[in.accountIndex].transaction[transIndex].loanUsage = \"U\",\n" +
        "                        assign(waitingTransactionIsClaim, true())\n" +
        "                      ))\n" +
        "                    )\n" +
        "                )\n" +
        "            ),\n" +
        "\n" +
        "            procedure(debtCheck),\n" +
        "\n" +
        "            assign(compulsoryCondition,\n" +
        "                client.account[in.accountIndex].dpdDays>=client.account[in.accountIndex].product.compulsorySettlement.CPD |\n" +
        "                (client.account[in.accountIndex].product.IsPartnerProduct = true()\n" +
        "                    & client.account[in.accountIndex].dpdDays>=client.account[in.accountIndex].product.Partner.ClaimDays) |\n" +
        "                (client.account[in.accountIndex].manualCompulsoryClearingPlanChanged != true() & client.account[in.accountIndex].hasManualCompulsoryClearing = true())\n" +
        "            ),\n" +
        "\n" +
        "            condition(//Set claim if CPD >=91\n" +
        "                case(\n" +
        "                    compulsoryCondition\n" +
        "                    &\n" +
        "                    client.account[in.accountIndex].hasCompulsoryCleaning = false()\n" +
        "                    &\n" +
        "                    (client.account[in.accountIndex].termToPay < client.account[in.accountIndex].loanTerm | client.account[in.accountIndex].product.IsPartnerProduct = true())\n" +
        "                    &\n" +
        "                    (waitingTransactionExists=false() | client.account[in.accountIndex].product.IsPartnerProduct = true() |\n" +
        "                      (client.account[in.accountIndex].manualCompulsoryClearingPlanChanged != true() & client.account[in.accountIndex].hasManualCompulsoryClearing = true())\n" +
        "                    ),\n" +
        "\n" +
        "                    group(\n" +
        "                        procedure(createCompulsoryClearingPlan)\n" +
        "                    )\n" +
        "                ),\n" +
        "                case(\n" +
        "                    compulsoryCondition\n" +
        "                    &\n" +
        "                    client.account[in.accountIndex].hasCompulsoryCleaning = false()\n" +
        "                    &\n" +
        "                    (client.account[in.accountIndex].termToPay = client.account[in.accountIndex].loanTerm)\n" +
        "                    &\n" +
        "                    (waitingTransactionExists=false() | client.account[in.accountIndex].product.IsPartnerProduct = true() |\n" +
        "                      (client.account[in.accountIndex].manualCompulsoryClearingPlanChanged != true() & client.account[in.accountIndex].hasManualCompulsoryClearing = true())\n" +
        "                    ),\n" +
        "                    group(\n" +
        "                        assign(client.account[in.accountIndex].hasCompulsoryCleaning, true())\n" +
        "                    )\n" +
        "                )\n" +
        "            ),\n" +
        "\n" +
        "            condition( //Create compensation for this term\n" +
        "                case(\n" +
        "                    client.account[in.accountIndex].product.IsPartnerProduct = true()\n" +
        "                    &\n" +
        "                    debtAmt>0\n" +
        "                    &\n" +
        "                    (daysBetween(client.account[in.accountIndex].payPlan[termNumber:client.account[in.accountIndex].termToPay].payPlanDueDate, businessDay())\n" +
        "                      >= client.account[in.accountIndex].product.Partner.CompensationDays |\n" +
        "                      waitingTransactionIsClaim = true()\n" +
        "                    )\n" +
        "                    &\n" +
        "                    client.account[in.accountIndex].product.Partner.CreateCompensation = true()\n" +
        "                    &\n" +
        "                    client.account[in.accountIndex].hasCompulsoryCleaning = false(),\n" +
        "\n" +
        "                    group(\n" +
        "                        procedure(createCompensationForCurrentTerm)\n" +
        "                    )\n" +
        "                )\n" +
        "\n" +
        "            ),\n" +
        "\n" +
        "            procedure(moveCurrentTerm),\n" +
        "\n" +
        "            procedure(checkWaitingTransactions)\n" +
        "        )\n" +
        "    )\n" +
        ")");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);
  }

  @Test
  public void type_inference() throws Exception {
    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("overdueDay", new Dat(LocalDate.now()));

    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>() {{
          put("overdueDay", new AstDat());
        }}
    ));

    m.registerTree("main", "group(\n" +
        "           assign(message,\"asdasdasd\" ~ overdueDay),\n" +
        "           assign(x,message)\n" +
        ")");

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);
  }

  @Test
  public void emptyGroup() {
    DtManagerWrapper m = createDtManager(nativeFunctions);
    m.setStructure(new AstObj(
        new HashMap<String, AstType>()
    ));

    m.registerTree("main", "group(empty())");

    SortedMap<String, DtType> scope = new TreeMap<>();

    m.compile();
    m.executeTree("main", scope);

    SortedMap<String, DtType> outScope = m.getScope();
    see(outScope);
  }

}
