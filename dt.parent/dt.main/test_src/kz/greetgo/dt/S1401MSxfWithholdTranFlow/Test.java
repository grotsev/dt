package kz.greetgo.dt.S1401MSxfWithholdTranFlow;

import kz.greetgo.dt.*;
import kz.greetgo.dt.S0605MSLoanRepayment.BigBatchTest;
import scala.math.BigDecimal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.SortedMap;
import java.util.TreeMap;


public class Test {
  private NativeFunctions nativeFunctions = new NativeFunctions() {
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

  @org.testng.annotations.Test(expectedExceptions = ErrorException.class) // TODO fix
  public void startBatch() throws IOException, URISyntaxException {
    DtManagerBuilder m = new DtManagerBuilder(nativeFunctions);

    SortedMap<String, DtType> scope = new TreeMap<>();
    BigBatchTest.fillScope(scope);


    String code = resource("S1401MSxfWithholdTranFlow.txt");
    m.registerTree("main", code);
    m.registerUserProcedure("withHoldingSuccessful", resource("withHoldingSuccessful.txt"));

    DtExecuteResult executeResult = m.newDtManager().executeTree("main", scope);

    for (String s : scope.keySet()) {
      System.out.println(s + " = " + scope.get(s));
    }

    System.out.println("getMessages = " + executeResult.messages);
  }

}

