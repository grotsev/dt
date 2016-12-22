package kz.greetgo.dt.RepaymentPlanGeneration;


import kz.greetgo.dt.*;
import org.testng.annotations.Test;
import scala.math.BigDecimal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.fest.assertions.api.Assertions.assertThat;


/**
 * Created by ilyas on 18.07.2016.
 */
public class RepaymentPlanGenerationTest {

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

  @Test
  public void testGenerationOfRepaymentPlan() throws IOException, URISyntaxException {
    DtManagerBuilder manager = new DtManagerBuilder(nativeFunctions);
//        addProcedures(manager);
    testProcedures(manager);
    testWholeGeneration(manager);
  }

  private void testDates(DtManager1 manager) throws IOException, URISyntaxException {

  }

  private void testProcedures(DtManagerBuilder manager) throws IOException, URISyntaxException {
    testGetCurrentMonthPrincipalMCEI(manager);
    //testGetFeeByFeeType(manager);
    // testGetPaymentDueDate(manager);
    //testGetTermFeeValues(manager);
    //testGetTermPaymentForMCEI(manager);
  }


  private void testGetPaymentDueDate(DtManager1 manager) {

  }

  private void testGetTermFeeValues(DtManager1 manager) throws IOException, URISyntaxException {

  }

  private void testGetTermPaymentForMCEI(DtManager1 manager) {

  }

  private void testGetCurrentMonthPrincipalMCEI(DtManagerBuilder manager) throws IOException, URISyntaxException {
    //Preparing input data

    SortedMap<String, DtType> scope = scopeWithPreparedData();

    manager.registerTree("main", readFile("getCurrentTermPrincipalForMCEI.txt"));

    printValues("===========================INPUT VALUES===========================", scope);
    manager.newDtManager().executeTree("main", scope);
    printValues("===========================OUTPUT VALUES===========================", scope);

    double termPrincipalAmt = getInt(scope, "product.tmp.termPrincipalAmt");
    assertThat(termPrincipalAmt).isNotNull();
    //assertEquals(termPrincipalAmt, 21d);
    //double termPrincipalFromJava=calcCurrMonthPrinForMCEI();


  }

  private void testGetFeeByFeeType(DtManager1 manager) {

  }

  private void printValues(String header, SortedMap<String, DtType> scope) {
    System.out.println(header);
    for (String key : scope.keySet()) {
      System.out.println(key + " : " + scope.get(key));
    }
  }


  private SortedMap<String, DtType> scopeWithPreparedData() {
    SortedMap<String, DtType> clientScope = new TreeMap<>();
    clientScope.put("in.accountId", new Num(BigDecimal.int2bigDecimal((123))));

    clientScope.put("product.id", new Num(BigDecimal.valueOf(123)));
    clientScope.put("in.LOAN_AMT", new Num(BigDecimal.valueOf(100000)));
    clientScope.put("in.INTEREST_RATE", new Num(BigDecimal.valueOf(0.3)));
    clientScope.put("in.LOAN_TERM", new Num(BigDecimal.valueOf(12)));
    clientScope.put("product.tmp.repaymentTerms", new Num(BigDecimal.valueOf(11)));
    clientScope.put("product.tmp.currentTerm", new Num(BigDecimal.valueOf(2)));
    clientScope.put("product.tmp.loanAmount", new Num(BigDecimal.valueOf(100000)));
    clientScope.put("product.floatRate", new Num(BigDecimal.valueOf(0.2)));

    return clientScope;
  }

  private void testWholeGeneration(DtManagerBuilder manager) throws IOException, URISyntaxException {


//        RepaymentPlanGenerationResponce serviceResponse = new RepaymentPlanGenerationResponce();
//        serviceResponse.loanInitPrin = getBigDecimal(dtResults, "product.loanAmount");
//        serviceResponse.loanTerm = getInt(dtResults, "product.repaymentTerms");
//        serviceResponse.loanFeeSum = java.math.BigDecimal.valueOf(sumNotNoll(getInt(dtResults, "installmentFee"),
//                getInt(dtResults, "product.lifeInsuranceAmt"),
//                getInt(dtResults, "product.loanServiceFee"),
//                getInt(dtResults, "product.loanServiceFee"),
//                getInt(dtResults, "product.insuranceFee"),
//                getInt(dtResults, "product.totalServiceFee"),
//                getInt(dtResults, "product.totPrepayPkgAmt")));
//
//        for (int i = 0; i < serviceResponse.loanTerm; i++) {
//            RepaymentPlanSchedule termSchedule = new RepaymentPlanSchedule();
//            termSchedule.loanTermTotAmt = java.math.BigDecimal.valueOf(sumNotNoll(
//                    getInt(dtResults, "product.repaymentScheduleTermValues." + i + "loanTermPrincipalAmt"),
//                    getInt(dtResults, "product.repaymentScheduleTermValues." + i + "loanTermInsuranceFee"),
//                    getInt(dtResults, "product.repaymentScheduleTermValues." + i + "loanTermInterest"),
//                    getInt(dtResults, "product.repaymentScheduleTermValues." + i + "loanTermServiceFee"),
//                    getInt(dtResults, "product.repaymentScheduleTermValues." + i + "loanTermTotalPrepayPkgFee"),
//                    getInt(dtResults, "product.repaymentScheduleTermValues." + i + "loanTermServiceFee"),
//                    getInt(dtResults, "product.repaymentScheduleTermValues." + i + "loanReplaceServiceFee"),
//                    getInt(dtResults, "product.repaymentScheduleTermValues." + i + "loanLifeInsuranceFee"),
//                    getInt(dtResults, "product.repaymentScheduleTermValues." + i + "loanTermTotalPrepayPkgFee")));
//            serviceResponse.loanIntSum.add(getBigDecimal(dtResults, "product.repaymentScheduleTermValues." + i + "loanTermInterest"));
//            termSchedule.loanTermPrin = getBigDecimal(dtResults, "product.repaymentScheduleTermValues." + i + "loanTermPrincipalAmt");
//            termSchedule.loanTermInstallFee = getBigDecimal(dtResults, "product.repaymentScheduleTermValues." + i + "loanTermPrincipalAmt");
//            termSchedule.loanTermSvcFee = getBigDecimal(dtResults, "product.repaymentScheduleTermValues." + i + "loanTermServiceFee");
//            termSchedule.loanCurrTerm = getInt(dtResults, "product.repaymentScheduleTermValues." + i + "termNumber");
//            termSchedule.loanInsuranceFee = getBigDecimal(dtResults, "product.repaymentScheduleTermValues." + i + "loanTermInsuranceFee");
//            termSchedule.loanLifeInsuFee = getBigDecimal(dtResults, "product.repaymentScheduleTermValues." + i + "loanLifeInsuranceFee");
//            termSchedule.loanPrepayPkgFee = getBigDecimal(dtResults, "product.repaymentScheduleTermValues." + i + "loanTermTotalPrepayPkgFee");
//            termSchedule.loanAgentFee = getBigDecimal(dtResults, "product.repaymentScheduleTermValues." + i + "loanReplaceServiceFee");
//            serviceResponse.subScheduleList.add(termSchedule);
//        }
//        assertEquals(serviceResponse.subScheduleList.size(), 20);
//        for (RepaymentPlanSchedule termValues : serviceResponse.subScheduleList) {
//            assertNotEquals(termValues.loanTermTotAmt, 0);
//        }


  }


  private int sumNotNoll(Integer... fees) {
    Integer sum = 0;
    for (Integer fee : fees) {
      if (fee != null) {
        sum += fee;
      }
    }
    return sum;
  }

  private Integer getInt(SortedMap<String, DtType> clientScope, String key) {
    if (clientScope != null && clientScope.get(key) != null) {
      return ((Num) clientScope.get(key)).num().toInt();
    }
    return 0;
  }

  private java.math.BigDecimal getBigDecimal(SortedMap<String, DtType> clientScope, String key) {
    if (clientScope != null && clientScope.get(key) != null) {
      return java.math.BigDecimal.valueOf(((Num) clientScope.get(key)).num().toDouble());
    }
    return java.math.BigDecimal.ZERO;
  }

  private void fixedAmtSinglePayment(DtManager1 manager) {

  }

  private void fixedAmtMultiplePayments(DtManager1 manager) {

  }


  private void rateAmtSinglePayment(DtManager1 manager) {

  }

  private void rateAmtMultiplePayments(DtManager1 manager) {

  }

  private SortedMap<String, DtType> executeDecisionTree(SortedMap<String, DtType> clientScope, DtManager1 manager) {
    manager.executeTree("main", clientScope);
    return clientScope;
  }

  private SortedMap<String, DtType> createData(Double loanAmt, Integer repaymentTerms, double interest,
                                               boolean isSinglePayment, boolean isFixedAmt, boolean includeAll, double amt) {
    SortedMap<String, DtType> clientScope = new TreeMap<>();
    clientScope.put("in.accountId", new Num(BigDecimal.int2bigDecimal((123))));

    clientScope.put("product.id", new Num(BigDecimal.valueOf(123)));
    clientScope.put("product.loanAmount", new Num(BigDecimal.valueOf(loanAmt)));
    clientScope.put("product.repaymentTerms", new Num(BigDecimal.valueOf(repaymentTerms)));

    clientScope.put("product.interest", new Num(BigDecimal.valueOf(interest)));
    clientScope.put("product.installmentFee.chargeMethod", isSinglePayment ? new Str("FirstTerm") : new Str("Terms"));
    clientScope.put("product.installmentFee.howToCalculate", !isFixedAmt ? new Str("BaseRatio") : new Str("FixedAmount"));
    clientScope.put("product.installmentFee.calculationParameter", new Num(BigDecimal.valueOf(amt)));

    clientScope.put("product.serviceFee.chargeMethod", isSinglePayment ? new Str("FirstTerm") : new Str("Terms"));
    clientScope.put("product.serviceFee.howToCalculate", !isFixedAmt ? new Str("BaseRatio") : new Str("FixedAmount"));
    clientScope.put("product.serviceFee.calculationParameter", new Num(BigDecimal.valueOf(amt)));

    clientScope.put("product.replaceServiceFee.chargeMethod", isSinglePayment ? new Str("FirstTerm") : new Str("Terms"));
    clientScope.put("product.replaceServiceFee.howToCalculate", !isFixedAmt ? new Str("BaseRatio") : new Str("FixedAmount"));
    clientScope.put("product.replaceServiceFee.calculationParameter", new Num(BigDecimal.valueOf(amt)));

    clientScope.put("product.insuranceFee.chargeMethod", isSinglePayment ? new Str("FirstTerm") : new Str("Terms"));
    clientScope.put("product.product.insuranceFee.howToCalculate", !isFixedAmt ? new Str("BaseRatio") : new Str("FixedAmount"));
    clientScope.put("product.insuranceFee.calculationParameter", new Num(BigDecimal.valueOf(amt)));

    if (includeAll) {
      clientScope.put("product.prepayPkgInd", new Str("Y"));
    } else {
      clientScope.put("product.prepayPkgInd", new Str("N"));
    }
    clientScope.put("product.prepayPkgFee.chargeMethod", isSinglePayment ? new Str("FirstTerm") : new Str("Terms"));
    clientScope.put("product.prepayPkgFee.howToCalculate", !isFixedAmt ? new Str("BaseRatio") : new Str("FixedAmount"));
    clientScope.put("product.prepayPkgFee.calculationParameter", new Num(BigDecimal.valueOf(amt)));

    if (includeAll) {
      clientScope.put("product.stampCustomInd", new Str("Y"));
    } else {
      clientScope.put("product.stampCustomInd", new Str("N"));
    }
    clientScope.put("product.stampDutyFee.chargeMethod", isSinglePayment ? new Str("FirstTerm") : new Str("Terms"));
    clientScope.put("product.stampDutyFee.howToCalculate", !isFixedAmt ? new Str("BaseRatio") : new Str("FixedAmount"));
    clientScope.put("product.stampDutyFee.calculationParameter", new Num(BigDecimal.valueOf(amt)));

    if (includeAll) {
      clientScope.put("product.joinLifeInsuInd", new Str("Y"));
    } else {
      clientScope.put("product.joinLifeInsuInd", new Str("N"));
    }
    clientScope.put("product.lifeInsuranceFee.chargeMethod", isSinglePayment ? new Str("FirstTerm") : new Str("Terms"));
    clientScope.put("product.lifeInsuranceFee.howToCalculate", !isFixedAmt ? new Str("BaseRatio") : new Str("FixedAmount"));
    clientScope.put("product.lifeInsuranceFee.calculationParameter", new Num(BigDecimal.valueOf(amt)));

    clientScope.put("product.isMergeBillDay", new Str("Y"));
    clientScope.put("product.floatRate", new Num(BigDecimal.valueOf(0.7)));
    clientScope.put("product.nextStatementDate", new Dat(LocalDate.now()));
    clientScope.put("product.dayOfPayment", new Num(BigDecimal.valueOf(30)));
    clientScope.put("dateOfCurrentBusinessDay", new Dat(LocalDate.now()));
    clientScope.put("product.numberOfGracePeriodDays", new Num(BigDecimal.valueOf(4)));
    return clientScope;
  }


  private String readFile(String fileName) throws IOException, URISyntaxException {
    java.net.URL url = getClass().getResource(fileName);
    java.nio.file.Path resPath = java.nio.file.Paths.get(url.toURI());
    return new String(java.nio.file.Files.readAllBytes(resPath), "UTF8");
  }

  private void addProcedures(DtManager1 manager) throws IOException {
//        try {
//            manager.registerUserProcedure("getFeeByFeeType", readFile("getFeeByFeeType.txt"));
//        } catch (Exception e) {
//            System.out.println("Adding procedure getFeeByFeeType failed " + e.getStackTrace());
//        }
//        try {
//            manager.registerUserProcedure("getCurrentTermPrincipalForMCEI", readFile("getCurrentTermPrincipalForMCEI.txt"));
//        } catch (Exception e) {
//            System.out.println("Adding procedure getCurrentTermPrincipalForMCEI failed " + e.getStackTrace());
//        }
//        try {
//            manager.registerUserProcedure("getPaymentDueDate", readFile("getPaymentDueDate.txt"));
//        } catch (Exception e) {
//            System.out.println("Adding procedure getPaymentDueDate failed " + e.getStackTrace());
//        }
//        try {
//            manager.registerUserProcedure("addCurrentTermRepaymentPlanToList", readFile("addCurrentTermRepaymentPlanToList.txt"));
//        } catch (Exception e) {
//            System.out.println("Adding procedure addCurrentTermRepaymentPlanToList failed " + e.getStackTrace());
//        }
//        try {
//            manager.registerUserProcedure("getTermPaymentForMCEI", readFile("getTermPaymentForMCEI.txt"));
//        } catch (Exception e) {
//            System.out.println("Adding procedure getTermPaymentForMCEI failed " + e.getStackTrace());
//        }
//
//        try {
//            manager.registerUserProcedure("getTermFeesValues", readFile("getTermFeeValues.txt"));
//        } catch (Exception e) {
//            System.out.println("Adding procedure getTermFeeValues failed ");
//            e.printStackTrace();
//        }
  }

  @Test(expectedExceptions = IllegalArgumentException.class) // TODO fix
  public void validityTest() throws IOException, URISyntaxException {
    //Double loanAmt, Integer repaymentTerms, double interest,
    // boolean isSinglePayment, boolean isFixedAmt, boolean includeAll, double amt


    DtManagerBuilder manager = new DtManagerBuilder(nativeFunctions);

    SortedMap<String, DtType> data = createData(new Double(1200), 6, 0.1, true, true, false, new Double(1200));


    manager.registerTree("main", readFile("getCurrentTermPrincipalForMCEI.txt"));

    printValues("===========================INPUT VALUES===========================", data);
    manager.newDtManager().executeTree("getCurrentTermPrincipalForMCEI", data);
    printValues("===========================OUTPUT VALUES===========================", data);

    double termPrincipalAmt = getInt(data, "product.tmp.termPrincipalAmt");
    assertThat(termPrincipalAmt).isNotNull();

  }


}
