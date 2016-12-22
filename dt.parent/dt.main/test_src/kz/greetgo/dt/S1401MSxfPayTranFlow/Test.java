package kz.greetgo.dt.S1401MSxfPayTranFlow;

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

  @org.testng.annotations.Test
  public void startBatch() throws IOException, URISyntaxException {
    DtManagerBuilder m = new DtManagerBuilder(nativeFunctions);

    SortedMap<String, DtType> scope = new TreeMap<>();
    BigBatchTest.fillForPayTransFlow(scope);
    scope.put("in.msDdReturnCode", new Str("0"));
    scope.put("in.txnAmt", new Num(BigDecimal.valueOf(3000)));
    scope.put("in.channelSerial", new Str("channelSerial"));
    scope.put("in.accountIndex", new Num(BigDecimal.valueOf(0)));
    scope.put("in.transactionIndex", new Num(BigDecimal.valueOf(0)));

    String code = resource("S1401MSxfPayTranFlow.txt");
    m.registerTree("main", code);


    m.registerUserProcedure("checkValidity", resource("checkValidity.txt"));
    m.registerUserProcedure("loanReleaseSuccessful", resource("loanReleaseSuccessful.txt"));
    m.registerUserProcedure("addCurrentTermRepaymentPlanToList", resource("addCurrentTermRepaymentPlanToList.txt"));
    m.registerUserProcedure("getFeeByFeeType", resource("getFeeByFeeType.txt"));
    m.registerUserProcedure("getRepaymentPlan", resource("getRepaymentPlan.txt"));
    m.registerUserProcedure("getTermFeesValues", resource("getTermFeesValues.txt"));
    m.registerUserProcedure("getTermPaymentForMCEI", resource("getTermPaymentForMCEI.txt"));
    m.registerUserProcedure("generatePayPlan", resource("generatePayPlan.txt"));
    m.registerUserProcedure("getCurrentTermPrincipalForMCEI", resource("getCurrentTermPrincipalForMCEI.txt"));
    m.registerUserProcedure("getPaymentDueDate", resource("getPaymentDueDate.txt"));

    DtExecuteResult executeResult = m.newDtManager().executeTree("main", scope);

    for (String s : scope.keySet()) {
      System.out.println(s + " = " + scope.get(s));
    }

    System.out.println("getMessages = " + executeResult.messages);
  }

}

