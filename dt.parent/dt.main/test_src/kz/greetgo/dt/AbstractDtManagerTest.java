package kz.greetgo.dt;

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
        new HashMap<String, AstType>(){{
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
        new HashMap<String, AstType>(){{
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
        new HashMap<String, AstType>(){{
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
        new HashMap<String, AstType>(){{
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
