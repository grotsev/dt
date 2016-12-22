package kz.greetgo.dt.finalTree;

import kz.greetgo.dt.*;
import org.testng.annotations.Test;
import org.testng.util.Strings;
import scala.math.BigDecimal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;


public class FinalTreeTest {
  private NativeFunctions nativeFunctions = new NativeFunctions() {
    @Override
    public LocalDate today() {
      return LocalDate.of(2016, 12, 18);
    }

    @Override
    public LocalDate businessDay() {
      return LocalDate.of(2016, 12, 28);
    }

    @Override
    public String generateId() {
      return Double.toString(Math.random());
    }

    @Override
    public long nextNumber() {
      return 10L;
    }

    @Override
    public void echo(DtType value) {
      System.out.println(value);
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
  };


  private String resource(String name) throws URISyntaxException, IOException {
    java.net.URL url = getClass().getResource(name);
    java.nio.file.Path resPath = java.nio.file.Paths.get(url.toURI());
    return new String(java.nio.file.Files.readAllBytes(resPath), "UTF8");
  }

  /*
  Вот это запускать!
   */

  @Test(enabled = false) // TODO fix
  public void testEndTree() throws IOException, URISyntaxException {
    DtManagerBuilder m = new DtManagerBuilder(nativeFunctions);


    SortedMap<String, DtType> scope = new TreeMap<>();
    fillFromStr(scope);

    scope.put("in.accountIndexes.0.value", new Num(BigDecimal.valueOf(0)));

    String code = resource("finalTree.txt");
    m.registerTree("main", code);

    m.registerUserProcedure("addFine", resource("addFine.txt"));
    m.registerUserProcedure("calculateCurrentDebt", resource("calculateCurrentDebt.txt"));
    m.registerUserProcedure("checkWaitingTransactions", resource("checkWaitingTransactions.txt"));
    m.registerUserProcedure("clearOverdueDate", resource("clearOverdueDate.txt"));
    m.registerUserProcedure("closeAccount", resource("closeAccount.txt"));
    m.registerUserProcedure("createCompensationForCurrentTerm", resource("createCompensationForCurrentTerm.txt"));
    m.registerUserProcedure("createCompulsoryClearingPlan", resource("createCompulsoryClearingPlan.txt"));
    m.registerUserProcedure("debtCheck", resource("debtCheck.txt"));
    m.registerUserProcedure("generateFine", resource("generateFine.txt"));
    m.registerUserProcedure("generateTransaction", resource("generateTransaction.txt"));
    m.registerUserProcedure("moveCurrentTerm", resource("moveCurrentTerm.txt"));
    m.registerUserProcedure("moveNextStmtDate", resource("moveNextStmtDate.txt"));

    DtExecuteResult executeResult = m.newDtManager().executeTree("main", scope);

    for (String s : scope.keySet()) {
      System.out.println(s + " = " + scope.get(s));
    }

    System.out.println("getMessages = " + executeResult.messages);
  }


  @Test
  public void testSum() throws IOException, URISyntaxException {
    DtManagerBuilder m = new DtManagerBuilder(nativeFunctions);
    SortedMap<String, DtType> scope = new TreeMap<>();
//        fillFromStr(scope);

    m.registerTree("main", "" +
        "group(" +
        "echo(\"Round \" ~ round(300.06))" +
        ")");


    DtExecuteResult executeResult = m.newDtManager().executeTree("main", scope);

    for (String s : scope.keySet()) {
      System.out.println(s + " = " + scope.get(s));
    }


    List<Check> sortedList = new ArrayList<>();
    sortedList.add(new Check("10"));
    sortedList.add(new Check("2"));
    sortedList.add(new Check("12"));
    sortedList.add(new Check("41"));
    sortedList.add(new Check("31"));

    Collections.sort(sortedList, (o1, o2) -> {
      java.math.BigDecimal txnAmt1;
      try {
        txnAmt1 = new java.math.BigDecimal(o1.txnAmt);
      } catch (NumberFormatException e) {
        txnAmt1 = java.math.BigDecimal.ZERO;
      }

      java.math.BigDecimal txnAmt2;
      try {
        txnAmt2 = new java.math.BigDecimal(o2.txnAmt);
      } catch (NumberFormatException e) {
        txnAmt2 = java.math.BigDecimal.ZERO;
      }


      return txnAmt1.compareTo(txnAmt2);
    });


    System.out.println("getMessages = " + executeResult.messages);

    System.out.println(sortedList);

  }

  private String toDDMMYYYY(String ss) {
    if (Strings.isNullOrEmpty(ss)) return null;

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    try {
      Date date = format.parse(ss);
      DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
      return df.format(date);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }


  public void fillFromStr(SortedMap<String, DtType> scope) throws IOException, URISyntaxException {
    String dbData = resource("10000001.txt");

    String[] split = dbData.split("\\n");
    for (String line : split) {
      String[] vars = line.split(" ~ ");
      if (vars.length != 3) continue;
      String key = "client." + vars[0];

      String type = vars[1];
      String value = vars[2];

      if (type.equals("STR")) {
        scope.put(key, new Str(value));
      }
      if (type.equals("NUM")) {
        scope.put(key, new Num(BigDecimal.exact(value)));
      }
      if (type.equals("DATE")) {//2016-09-05T00:00:00.000
        scope.put(key, new Dat(LocalDate.parse(value.replace("T00:00:00.000", ""))));
      }
      if (type.equals("BOOL")) {
        if (value.equals("1")) scope.put(key, new Bool(true));
        if (value.equals("0")) scope.put(key, new Bool(false));
      }
    }
  }

  private class Check {
    public String txnAmt;

    public Check(String x) {
      txnAmt = x;
    }

    @Override
    public String toString() {
      return txnAmt;
    }
  }
}

