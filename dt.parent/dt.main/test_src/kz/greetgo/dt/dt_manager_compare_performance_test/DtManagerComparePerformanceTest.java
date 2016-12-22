package kz.greetgo.dt.dt_manager_compare_performance_test;

import kz.greetgo.dt.AbstractDtManagerTest;
import kz.greetgo.dt.DtManager;
import kz.greetgo.dt.DtManagerBuilder;
import kz.greetgo.dt.DtType;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public class DtManagerComparePerformanceTest {
  public static void main(String[] args) throws Exception {
    new DtManagerComparePerformanceTest().execute();
  }

  private final DtManagerComparePerformanceProcedures def = new DtManagerComparePerformanceProcedures();

  private void execute() throws Exception {
    DtManagerBuilder builder = new DtManagerBuilder();
    builder.setNativeFunctions(AbstractDtManagerTest.nativeFunctions);
    //builder.CLASSPATH = new File("build/DtManagerComparePerformanceTest/dt_src"); // TODO remove

    builder.setStructure(TestData$.MODULE$.structure());

    def.registerProcedures(builder);

    builder.options.genBreakpoint = false;
    builder.options.genSourceMap = false;
    builder.options.genMetrics = true;

    builder.useDtManager3 = false;
    DtManager dtManager1 = builder.newDtManager();
    builder.useDtManager3 = true;
    DtManager dtManager3 = builder.newDtManager();

    setFieldCls(dtManager3, "DtExec006");

    final long count = 100_000;
    System.out.println("        COUNT = " + count);

    Map<String, DtType> scopeResult3 = makeTestWith(dtManager3, "dtManager3", count);
    Map<String, DtType> scopeResult1 = makeTestWith(dtManager1, "dtManager1", count);

    System.out.println();
    System.out.println(" * * * ");
    System.out.println();

    //showScopeResult("dtManager1", scopeResult1);
    //showScopeResult("dtManager3", scopeResult3);
    showScopeResult2("dtManager1 dtManager3", scopeResult1, scopeResult3);
  }

  private void setFieldCls(DtManager dtManager3, String className) throws Exception {
    Field field = dtManager3.getClass().getDeclaredField("cls");
    field.setAccessible(true);
    field.set(dtManager3,
        Class.forName("kz.greetgo.dt.dt_manager_compare_performance_test.tmp." + className)
    );
  }

  private void showScopeResult2(String descr, Map<String, DtType> s1, Map<String, DtType> s2) {
    System.out.println("Last scope results for " + descr);
    Set<String> keys = new HashSet<>();
    keys.addAll(s1.keySet());
    keys.addAll(s2.keySet());
    keys.stream().sorted().forEach(key -> {
      StringBuilder sb = new StringBuilder();
      sb.append("  ").append(key).append(" = ");
      if (s1.containsKey(key)) {
        sb.append(s1.get(key));
      } else {
        sb.append("<  >");
      }
      sb.append("    ");
      if (s2.containsKey(key)) {
        sb.append(s2.get(key));
      } else {
        sb.append("<  >");
      }
      System.out.println(sb);
    });
  }

  private void showScopeResult(String dtManagerName, Map<String, DtType> scopeResult) {
    System.out.println("Last scope result for " + dtManagerName);
    scopeResult.keySet().stream().sorted()
        .forEach(key -> System.out.println("  " + key + "=" + scopeResult.get(key)));
  }

  public static String asStr(double value) {
    return String.format("%4.7f", value);
  }

  private static final double GIG = 1_000_000_000;

  private void showDelay(String delayName, long delay, long count) {
    System.out.println("  " + delayName + " : " + asStr((double) delay / (double) count / GIG * 1000000.0) + " mks");
  }


  private Map<String, DtType> makeTestWith(DtManager dtManager, String dtManagerName, long count) {
    System.out.println("* * * USING " + dtManagerName);

    long time_dtManagerCopy = 0, time_newScope = 0L, time_exec = 0L, time_getScope = 0L;

    Map<String, DtType> scopeResult = null;

    for (long i = 0; i < count; i++) {

      long t1 = System.nanoTime();

      dtManager = dtManager.copy();

      long t2 = System.nanoTime();

      SortedMap<String, DtType> scope = def.newScope();

      long t3 = System.nanoTime();

      dtManager.executeTree("main", scope);

      long t4 = System.nanoTime();

      scopeResult = dtManager.getScope();

      long t5 = System.nanoTime();

      time_dtManagerCopy += t2 - t1;
      time_newScope += t3 - t2;
      time_exec += t4 - t3;
      time_getScope += t5 - t4;
    }


    showDelay("exec          ", time_exec, count);
    {
      Map<String, Long> metrics = dtManager.getMetrics();
      metrics.keySet().stream().sorted().filter(k -> !"count".equals(k)).forEach(k -> {
        showDelay("  " + k, metrics.get(k), count);
      });

    }
    showDelay("dtManagerCopy ", time_dtManagerCopy, count);
    showDelay("newScope      ", time_newScope, count);
    showDelay("getScope      ", time_getScope, count);

    return scopeResult;
  }

}
