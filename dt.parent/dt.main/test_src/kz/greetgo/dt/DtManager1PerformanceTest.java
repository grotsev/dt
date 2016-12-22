package kz.greetgo.dt;

import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.fest.assertions.api.Assertions.assertThat;

public class DtManager1PerformanceTest {

  static class TestNativeFunctions implements NativeFunctions {
    @Override
    public LocalDate today() {
      return LocalDate.now();
    }

    @Override
    public LocalDate businessDay() {
      return LocalDate.now();
    }

    private final AtomicLong nextId = new AtomicLong(10000);

    @Override
    public String generateId() {
      return "GenId" + nextId.getAndIncrement();
    }

    private final AtomicLong nextNumberGenerator = new AtomicLong(1_000_000);

    @Override
    public long nextNumber() {
      return nextNumberGenerator.getAndIncrement();
    }

    @Override
    public void echo(DtType value) {
      System.out.println("echo " + value);
    }

    @Override
    public void echoScope(SortedMap<String, DtType> scope) {
      System.out.println(scope);
    }

    @Override
    public boolean dictInc(String dictCode, String fieldCode, String rowCode, scala.math.BigDecimal incrementValue) {
      return false;
    }


    @Override
    public scala.math.BigDecimal dictValueNumber(String dictCode, String fieldCode, String rowCode) {
      return null;
    }

    @Override
    public String dictValueText(String dictCode, String fieldCode, String rowCode) {
      return null;
    }
  }

  private static Num asNum(String s) {
    return Num.apply(scala.math.BigDecimal.exact(new BigDecimal(s)));
  }

  private static void populate(SortedMap<String, DtType> scope, String asdSum, String foo) {
    scope.put("in.accountIds.0.id", Str.apply("acc_101"));
    scope.put("in.accountIds.1.id", Str.apply("acc_501"));
    scope.put("in.accountIds.2.id", Str.apply("acc_783"));
    scope.put("in.asdSum", asNum(asdSum));
    scope.put("in.foo", asNum(foo));
    scope.put("client.name", Str.apply("Ivan"));
    scope.put("client.account.0.sum", asNum("200123.45"));
    scope.put("client.account.0.id", Str.apply("asd1"));
    scope.put("client.account.1.sum", asNum("200124.45"));
    scope.put("client.account.1.id", Str.apply("acc_101"));
    scope.put("client.account.2.sum", asNum("200125.45"));
    scope.put("client.account.2.id", Str.apply("asd3"));
    scope.put("client.account.3.sum", asNum("200126.45"));
    scope.put("client.account.3.id", Str.apply("acc_501"));
    scope.put("client.account.4.sum", asNum("200127.45"));
    scope.put("client.account.4.id", Str.apply("acc_783"));
    scope.put("client.account.5.sum", asNum("200128.45"));
    scope.put("client.account.5.id", Str.apply("asd6"));
  }

  private static void populateCode(Map<String, String> treeCodes, Map<String, String> procCodes) {
    treeCodes.put("createPayPlanTree", "" +
        "foreach(tmp.i_asd, 0, len(in.accountIds) - 1, group(" +
        "  assign(in.accountId, in.accountIds[tmp.i_asd].id)," +
        "  procedure(createPayPlan)" +
        "))");

    procCodes.put("createPayPlan", "group(\n" +
        "  assign(client.account[id:in.accountId].sum2, client.account[id:in.accountId].sum + in.asdSum),\n" +
        "  foreach(tmp.p_index, 1, 7, group(\n" +
        "    assign(tmp.planId, generateId()),\n" +
        "    assign(client.account[id:in.accountId].payPlan[id:tmp.planId].sum,\n" +
        "               client.account[id:in.accountId].sum2 / 100),\n" +
        "    assign(client.account[id:in.accountId].payPlan[id:tmp.planId].sumAsd1,\n" +
        "                               client.account[id:in.accountId].sum2 / (in.foo + 1)),\n" +
        "    assign(client.account[id:in.accountId].payPlan[id:tmp.planId].sumAsd2,\n" +
        "                               client.account[id:in.accountId].sum2 / (in.foo + 2)),\n" +
        "    assign(client.account[id:in.accountId].payPlan[id:tmp.planId].sumAsd3,\n" +
        "                               client.account[id:in.accountId].sum2 / (in.foo + 3)),\n" +
        "    assign(client.account[id:in.accountId].payPlan[id:tmp.planId].sumAsd4,\n" +
        "                               client.account[id:in.accountId].sum2 / (in.foo + 4)),\n" +
        "    assign(client.account[id:in.accountId].payPlan[id:tmp.planId].sumAsd5,\n" +
        "                               client.account[id:in.accountId].sum2 / (in.foo + 5)),\n" +
        "    assign(client.account[id:in.accountId].payPlan[id:tmp.planId].sumAsd6,\n" +
        "                               client.account[id:in.accountId].sum2 / (in.foo + 6)),\n" +
        "    assign(client.account[id:in.accountId].payPlan[id:tmp.planId].sumAsd7,\n" +
        "                               client.account[id:in.accountId].sum2 / (in.foo + 7)),\n" +
        "    assign(client.account[id:in.accountId].payPlan[id:tmp.planId].sumAsd8,\n" +
        "                               client.account[id:in.accountId].sum2 / (in.foo + 8)),\n" +
        "    assign(client.account[id:in.accountId].payPlan[id:tmp.planId].sumAsd9,\n" +
        "                               client.account[id:in.accountId].sum2 / (in.foo + 9)),\n" +
        "    assign(client.account[id:in.accountId].payPlan[id:tmp.planId].sumAsd10,\n" +
        "                               client.account[id:in.accountId].sum2 / (in.foo + 10)),\n" +
        "    assign(client.account[id:in.accountId].payPlan[id:tmp.planId].sumAsd11,\n" +
        "                               client.account[id:in.accountId].sum2 / (in.foo + 11))\n" +
        "  ))\n" +
        ")");
  }


  private static final double GIG = 1_000_000_000;

  public static String asStr(double value) {
    return String.format("%4.5f s", value);
  }

  @Test
  public void performance() throws Exception {
    TestNativeFunctions nativeFunctions = new TestNativeFunctions();

    Map<String, String> treeCodes = new HashMap<>();
    Map<String, String> procCodes = new HashMap<>();
    populateCode(treeCodes, procCodes);

    DtManager1 dtManager1 = new DtManager1(nativeFunctions);
    treeCodes.entrySet().forEach(e -> dtManager1.registerTree(e.getKey(), e.getValue()));
    procCodes.entrySet().forEach(e -> dtManager1.registerUserProcedure(e.getKey(), e.getValue()));

    DtManager2 dtManager2 = new DtManager2(nativeFunctions);
    treeCodes.entrySet().forEach(e -> dtManager2.registerTree(e.getKey(), e.getValue()));
    procCodes.entrySet().forEach(e -> dtManager2.registerUserProcedure(e.getKey(), e.getValue()));

    long timeOldSum = 0, timeNewSum = 0;
    int count = 0;

    for (int i = 0; i < 400; i++) {

      SortedMap<String, DtType> scope = new TreeMap<>();
      populate(scope, "7000." + i, "100" + i);

      final Obj obj = DtManagerUtil.scopeToObj(scope);

      long timeOld = System.nanoTime();
      dtManager1.executeTree("createPayPlanTree", scope);
      timeOld = System.nanoTime() - timeOld;

      long timeNew = System.nanoTime();
      dtManager2.executeTree("createPayPlanTree", obj);
      timeNew = System.nanoTime() - timeNew;

      System.out.println("timeOld = " + asStr(timeOld / GIG));
      System.out.println("timeNew = " + asStr(timeNew / GIG));
      timeOldSum += timeOld;
      timeNewSum += timeNew;
      count++;

      assertThat(scope.get("client.account.1.payPlan.1.sumAsd9")).isInstanceOf(Num.class);

      //if (i == 0) System.out.println(obj);
    }

    System.out.println("avg old time = " + asStr((double) timeOldSum / (double) count / GIG));
    System.out.println("avg new time = " + asStr((double) timeNewSum / (double) count / GIG));

    //scope.entrySet().forEach(e -> System.out.println(e.getKey() + "=" + e.getValue()));
  }
}
