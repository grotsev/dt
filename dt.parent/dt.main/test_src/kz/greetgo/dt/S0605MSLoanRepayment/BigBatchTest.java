package kz.greetgo.dt.S0605MSLoanRepayment;

import kz.greetgo.dt.*;
import org.testng.annotations.Test;
import scala.math.BigDecimal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.SortedMap;
import java.util.TreeMap;


public class BigBatchTest {
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

  @Test(expectedExceptions = ErrorException.class)
  public void startBatch() throws IOException, URISyntaxException {
    DtManagerBuilder m = new DtManagerBuilder(nativeFunctions);


    SortedMap<String, DtType> scope = new TreeMap<>();
    fillScopeForBigBatch(scope);
    fillBigBatchInScope(scope);

    m.registerUserProcedure("paymentSuccessfulAdvance", resource("paymentSuccessfulAdvance.txt"));
    m.registerUserProcedure("paymentSuccessfulNormal", resource("paymentSuccessfulNormal.txt"));
    m.registerUserProcedure("paymentSuccessfulOverdue", resource("paymentSuccessfulOverdue.txt"));

//        m.registerUserProcedure("moveNextStmtDate", resource("moveNextStmtDate.txt"));
    m.registerUserProcedure("prepareForAdvanceClearing", resource("prepareForAdvanceClearing.txt"));
    m.registerUserProcedure("calculateCurrentDebt", resource("calculateCurrentDebt.txt"));

    m.registerUserProcedure("removeUnnecessaryFines", resource("removeUnnecessaryFines.txt"));
    m.registerUserProcedure("sumProcedure", resource("sumProcedure.txt"));
    m.registerUserProcedure("commonPayment", "group()");

    m.registerUserProcedure("checkValidity", resource("checkValidity.txt"));

    String code = resource("S0605MSLoanRepayment.txt");
    m.registerTree("main", code);
    DtExecuteResult executeResult = m.newDtManager().executeTree("main", scope);

    for (String s : scope.keySet()) {
      System.out.println(s + " = " + scope.get(s));
    }

    System.out.println("getMessages = " + executeResult.messages);
  }

  private void fillBigBatchInScope(SortedMap<String, DtType> scope) {
    scope.put("in.msDdReturnCode", new Str("0"));
    scope.put("in.accountIndex", new Num(BigDecimal.valueOf(100)));
    scope.put("in.transactionIndex", new Num(BigDecimal.valueOf(100)));

  }

  @Test(enabled = false) // TODO fix test
  public void testRemoveFines() throws IOException, URISyntaxException {
    DtManagerBuilder m = new DtManagerBuilder(nativeFunctions);

    SortedMap<String, DtType> scope = new TreeMap<>();

    fillScopeForBigBatch(scope);
    fillPayPlanScope(scope);

    //Transaction time minusDays 3
    scope.put("client.account.0.overdueFine.0.amount", new Num(BigDecimal.valueOf(100)));
    scope.put("client.account.0.overdueFine.0.date", new Dat(LocalDate.now()));
    scope.put("client.account.0.overdueFine.0.status", new Str("U"));

    scope.put("client.account.0.overdueFine.1.amount", new Num(BigDecimal.valueOf(100)));
    scope.put("client.account.0.overdueFine.1.date", new Dat(LocalDate.now().minusDays(4)));
    scope.put("client.account.0.overdueFine.1.status", new Str("U"));

    scope.put("client.account.0.overdueFine.2.amount", new Num(BigDecimal.valueOf(100)));
    scope.put("client.account.0.overdueFine.2.date", new Dat(LocalDate.now().minusDays(1)));
    scope.put("client.account.0.overdueFine.2.status", new Str("U"));


    m.registerUserProcedure("removeUnnecessaryFines", resource("removeUnnecessaryFines.txt"));

    String code = "group(" +
        "assign(removeDate, client.account[in.accountIndex].transaction[in.transactionIndex].optDatetime)," +
        "procedure(removeUnnecessaryFines)" +
        ")";
    m.registerTree("main", code);

    DtExecuteResult executeResult = m.newDtManager().executeTree("main", scope);

    for (String s : scope.keySet()) {
      System.out.println(s + " = " + scope.get(s));
    }

    System.out.println("getMessages = " + executeResult.messages);

  }

  @Test
  public void calculateCurrentDebt() throws IOException, URISyntaxException {
    DtManagerBuilder m = new DtManagerBuilder(nativeFunctions);


    SortedMap<String, DtType> scope = new TreeMap<>();

    fillScope(scope);
    fillPayPlanScope(scope);

    m.registerUserProcedure("calculateCurrentDebt", resource("calculateCurrentDebt.txt"));
    m.registerUserProcedure("sumProcedure", resource("sumProcedure.txt"));

    String code = "group(procedure(calculateCurrentDebt))";
    m.registerTree("main", code);

    DtExecuteResult executeResult = m.newDtManager().executeTree("main", scope);

    for (String s : scope.keySet()) {
      System.out.println(s + " = " + scope.get(s));
    }

    System.out.println("getMessages = " + executeResult.messages);
  }

  @Test(expectedExceptions = ErrorException.class, expectedExceptionsMessageRegExp = "len 1")
  public void len2() {
    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("client.account.0.cpdBeginDate", new Dat(LocalDate.now().minusDays(10)));
    scope.put("client.account.0.transaction.0.loanUsage", new Str("N"));
    scope.put("client.account.0.paidPrincipalAmt", new Num(BigDecimal.valueOf(10)));
    scope.put("client.account.0.paidInterest", new Num(BigDecimal.valueOf(10)));

    DtManagerBuilder m = new DtManagerBuilder(nativeFunctions);

    m.registerTree("main", "group(error(\"len \"~len(client.account)))");

    DtExecuteResult result = m.newDtManager().executeTree("main", scope);

    System.out.println(result.messages);
  }

  @Test
  public void empty() {
    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("test", new Dat(LocalDate.now().minusDays(10)));

    DtManagerBuilder m = new DtManagerBuilder(nativeFunctions);

    m.registerTree("main", "group(message(\"test\"~test), assign(test,empty()))");

    DtExecuteResult result = m.newDtManager().executeTree("main", scope);

    for (String s : scope.keySet()) {
      System.out.println(s + " = " + scope.get(s));
    }

    System.out.println("getMessages = " + result.messages);
  }

  public static void fillScopeForBigBatch(SortedMap<String, DtType> scope) {

    scope.put("client.account.0.acctNbr", new Str("10000000"));
    scope.put("client.account.0.acctType", new Str("E"));
    scope.put("client.account.0.acqId", new Str("10000000"));
    scope.put("client.account.0.agreementRateExpireDate", new Dat(LocalDate.parse("2020-09-09")));
    scope.put("client.account.0.applicationNo", new Str("8997882"));
    scope.put("client.account.0.applyDate", new Dat(LocalDate.parse("2015-09-30")));
    scope.put("client.account.0.batchDate", new Dat(LocalDate.parse("2016-09-01")));
    scope.put("client.account.0.compoundRate", new Num(BigDecimal.valueOf(0.15)));
    scope.put("client.account.0.contrNbr", new Str("checkContrNbr"));
    scope.put("client.account.0.corpName", new Str("长亮科技长亮科技"));
    scope.put("client.account.0.createTime", new Dat(LocalDate.parse("2016-08-01")));
    scope.put("client.account.0.currentTerm", new Num(BigDecimal.valueOf(1)));
    scope.put("client.account.0.cycleDay", new Num(BigDecimal.valueOf(1)));
    scope.put("client.account.0.ddBankAcctName", new Str("Beam"));
    scope.put("client.account.0.ddBankAcctNbr", new Str("6225882121996634"));
    scope.put("client.account.0.ddBankBranch", new Str("0100"));
    scope.put("client.account.0.ddBankCity", new Str("1"));
    scope.put("client.account.0.ddBankCityCode", new Str("110101"));
    scope.put("client.account.0.ddBankName", new Str("1"));
    scope.put("client.account.0.ddBankProvince", new Str("1"));
    scope.put("client.account.0.ddBankProvinceCode", new Str("110000"));
    scope.put("client.account.0.feeRate", new Num(BigDecimal.valueOf(0.90)));
    scope.put("client.account.0.gender", new Str("M"));
    scope.put("client.account.0.graceDate", new Dat(LocalDate.parse("2016-09-01")));
    scope.put("client.account.0.hasAdvancedClearing", new Bool(false));
    scope.put("client.account.0.id", new Str("RWVj0qmzdsVg2pvRg"));
    scope.put("client.account.0.installmentFeeAmt", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.interestRate", new Num(BigDecimal.valueOf(0.15)));
    scope.put("client.account.0.isAbnormal", new Bool(false));
    scope.put("client.account.0.isFreezed", new Bool(false));
    scope.put("client.account.0.joinLifeInsuInd", new Bool(true));
    scope.put("client.account.0.lifeInsuFeeRate", new Num(BigDecimal.valueOf(0.90)));
    scope.put("client.account.0.loanAmt", new Num(BigDecimal.valueOf(3000)));
    scope.put("client.account.0.loanFeeDefId", new Str("11010601"));
    scope.put("client.account.0.loanTerm", new Num(BigDecimal.valueOf(6)));
    scope.put("client.account.0.name", new Str("silver"));
    scope.put("client.account.0.nextStmtDate", new Dat(LocalDate.parse("2016-10-01")));
    scope.put("client.account.0.owningBranch", new Str("000000001"));
    scope.put("client.account.0.paidSumOfDebtTillBizDate", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.paidTerms", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.0.actual", new Bool(true));
    scope.put("client.account.0.payPlan.0.hasPaid", new Bool(false));
    scope.put("client.account.0.payPlan.0.paidInstallmentFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.0.paidInterest", new Num(BigDecimal.valueOf(37.50)));
    scope.put("client.account.0.payPlan.0.paidLifeInsurance", new Num(BigDecimal.valueOf(450.00)));
    scope.put("client.account.0.payPlan.0.paidPrepayPkgFee", new Num(BigDecimal.valueOf(0.0)));
    scope.put("client.account.0.payPlan.0.paidPrincipal", new Num(BigDecimal.valueOf(484.60)));
    scope.put("client.account.0.payPlan.0.paidReplaceSvcFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.0.paidServiceFee", new Num(BigDecimal.valueOf(450.00)));
    scope.put("client.account.0.payPlan.0.paidStampDutyFee", new Num(BigDecimal.valueOf(0.15)));
    scope.put("client.account.0.payPlan.0.paidTotal", new Num(BigDecimal.valueOf(1422.25)));
    scope.put("client.account.0.payPlan.0.payPlanDueDate", new Dat(LocalDate.parse("2016-09-01")));
    scope.put("client.account.0.payPlan.0.remainingInstallmentFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.0.remainingInterest", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.0.remainingLifeInsurance", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.0.remainingPrepayPkgFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.0.remainingPrincipal", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.0.remainingReplaceSvcFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.0.remainingServiceFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.0.remainingStampDutyFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.0.remainingTotal", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.0.termNumber", new Num(BigDecimal.valueOf(1)));
    scope.put("client.account.0.payPlan.1.actual", new Bool(true));
    scope.put("client.account.0.payPlan.1.hasPaid", new Bool(false));
    scope.put("client.account.0.payPlan.1.paidInstallmentFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.1.paidInterest", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.1.paidLifeInsurance", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.1.paidPrepayPkgFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.1.paidPrincipal", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.1.paidReplaceSvcFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.1.paidServiceFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.1.paidStampDutyFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.1.paidTotal", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.1.payPlanDueDate", new Dat(LocalDate.parse("2016-10-01")));
    scope.put("client.account.0.payPlan.1.remainingInstallmentFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.1.remainingInterest", new Num(BigDecimal.valueOf(31.44)));
    scope.put("client.account.0.payPlan.1.remainingLifeInsurance", new Num(BigDecimal.valueOf(450.00)));
    scope.put("client.account.0.payPlan.1.remainingPrepayPkgFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.1.remainingPrincipal", new Num(BigDecimal.valueOf(490.66)));
    scope.put("client.account.0.payPlan.1.remainingReplaceSvcFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.1.remainingServiceFee", new Num(BigDecimal.valueOf(450.00)));
    scope.put("client.account.0.payPlan.1.remainingStampDutyFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.1.remainingTotal", new Num(BigDecimal.valueOf(1422.10)));
    scope.put("client.account.0.payPlan.1.termNumber", new Num(BigDecimal.valueOf(2)));
    scope.put("client.account.0.payPlan.2.actual", new Bool(true));
    scope.put("client.account.0.payPlan.2.hasPaid", new Bool(false));
    scope.put("client.account.0.payPlan.2.paidInstallmentFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.2.paidInterest", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.2.paidLifeInsurance", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.2.paidPrepayPkgFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.2.paidPrincipal", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.2.paidReplaceSvcFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.2.paidServiceFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.2.paidStampDutyFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.2.paidTotal", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.2.payPlanDueDate", new Dat(LocalDate.parse("2016-11-01")));
    scope.put("client.account.0.payPlan.2.remainingInstallmentFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.2.remainingInterest", new Num(BigDecimal.valueOf(25.31)));
    scope.put("client.account.0.payPlan.2.remainingLifeInsurance", new Num(BigDecimal.valueOf(450.00)));
    scope.put("client.account.0.payPlan.2.remainingPrepayPkgFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.2.remainingPrincipal", new Num(BigDecimal.valueOf(496.79)));
    scope.put("client.account.0.payPlan.2.remainingReplaceSvcFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.2.remainingServiceFee", new Num(BigDecimal.valueOf(450.00)));
    scope.put("client.account.0.payPlan.2.remainingStampDutyFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.2.remainingTotal", new Num(BigDecimal.valueOf(1422.10)));
    scope.put("client.account.0.payPlan.2.termNumber", new Num(BigDecimal.valueOf(3)));
    scope.put("client.account.0.payPlan.3.actual", new Bool(true));
    scope.put("client.account.0.payPlan.3.hasPaid", new Bool(false));
    scope.put("client.account.0.payPlan.3.paidInstallmentFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.3.paidInterest", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.3.paidLifeInsurance", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.3.paidPrepayPkgFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.3.paidPrincipal", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.3.paidReplaceSvcFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.3.paidServiceFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.3.paidStampDutyFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.3.paidTotal", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.3.payPlanDueDate", new Dat(LocalDate.parse("2016-12-01")));
    scope.put("client.account.0.payPlan.3.remainingInstallmentFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.3.remainingInterest", new Num(BigDecimal.valueOf(19.10)));
    scope.put("client.account.0.payPlan.3.remainingLifeInsurance", new Num(BigDecimal.valueOf(450.00)));
    scope.put("client.account.0.payPlan.3.remainingPrepayPkgFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.3.remainingPrincipal", new Num(BigDecimal.valueOf(503.00)));
    scope.put("client.account.0.payPlan.3.remainingReplaceSvcFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.3.remainingServiceFee", new Num(BigDecimal.valueOf(450.00)));
    scope.put("client.account.0.payPlan.3.remainingStampDutyFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.3.remainingTotal", new Num(BigDecimal.valueOf(1422.10)));
    scope.put("client.account.0.payPlan.3.termNumber", new Num(BigDecimal.valueOf(4)));
    scope.put("client.account.0.payPlan.4.actual", new Bool(true));
    scope.put("client.account.0.payPlan.4.hasPaid", new Bool(false));
    scope.put("client.account.0.payPlan.4.paidInstallmentFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.4.paidInterest", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.4.paidLifeInsurance", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.4.paidPrepayPkgFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.4.paidPrincipal", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.4.paidReplaceSvcFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.4.paidServiceFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.4.paidStampDutyFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.4.paidTotal", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.4.payPlanDueDate", new Dat(LocalDate.parse("2017-01-01")));
    scope.put("client.account.0.payPlan.4.remainingInstallmentFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.4.remainingInterest", new Num(BigDecimal.valueOf(12.81)));
    scope.put("client.account.0.payPlan.4.remainingLifeInsurance", new Num(BigDecimal.valueOf(450.00)));
    scope.put("client.account.0.payPlan.4.remainingPrepayPkgFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.4.remainingPrincipal", new Num(BigDecimal.valueOf(509.29)));
    scope.put("client.account.0.payPlan.4.remainingReplaceSvcFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.4.remainingServiceFee", new Num(BigDecimal.valueOf(450.00)));
    scope.put("client.account.0.payPlan.4.remainingStampDutyFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.4.remainingTotal", new Num(BigDecimal.valueOf(1422.10)));
    scope.put("client.account.0.payPlan.4.termNumber", new Num(BigDecimal.valueOf(5)));
    scope.put("client.account.0.payPlan.5.actual", new Bool(true));
    scope.put("client.account.0.payPlan.5.hasPaid", new Bool(false));
    scope.put("client.account.0.payPlan.5.paidInstallmentFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.5.paidInterest", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.5.paidLifeInsurance", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.5.paidPrepayPkgFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.5.paidPrincipal", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.5.paidReplaceSvcFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.5.paidServiceFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.5.paidStampDutyFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.5.paidTotal", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.5.payPlanDueDate", new Dat(LocalDate.parse("2017-02-01")));
    scope.put("client.account.0.payPlan.5.remainingInstallmentFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.5.remainingInterest", new Num(BigDecimal.valueOf(6.45)));
    scope.put("client.account.0.payPlan.5.remainingLifeInsurance", new Num(BigDecimal.valueOf(450.00)));
    scope.put("client.account.0.payPlan.5.remainingPrepayPkgFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.5.remainingPrincipal", new Num(BigDecimal.valueOf(515.66)));
    scope.put("client.account.0.payPlan.5.remainingReplaceSvcFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.5.remainingServiceFee", new Num(BigDecimal.valueOf(450.00)));
    scope.put("client.account.0.payPlan.5.remainingStampDutyFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.payPlan.5.remainingTotal", new Num(BigDecimal.valueOf(1422.11)));
    scope.put("client.account.0.payPlan.5.termNumber", new Num(BigDecimal.valueOf(6)));
    scope.put("client.account.0.penaltyRate", new Num(BigDecimal.valueOf(0.15)));
    scope.put("client.account.0.pmtDueDate", new Dat(LocalDate.parse("2016-09-01")));
    scope.put("client.account.0.product.advanceRepaymentApply.applyBefore", new Num(BigDecimal.valueOf(5)));
    scope.put("client.account.0.product.advanceRepaymentApply.canToApply", new Bool(true));
    scope.put("client.account.0.product.advanceRepaymentApply.deductBefore", new Num(BigDecimal.valueOf(3)));
    scope.put("client.account.0.product.AdvanceRepaymentServiceFee.0.BaseType", new Str("RemainingPrincipal"));
    scope.put("client.account.0.product.AdvanceRepaymentServiceFee.0.HowToCalculate", new Str("FixedAmount"));
    scope.put("client.account.0.product.AdvanceRepaymentServiceFee.0.MaxLoanPeriod", new Num(BigDecimal.valueOf(99)));
    scope.put("client.account.0.product.AdvanceRepaymentServiceFee.0.parameter", new Num(BigDecimal.valueOf(200)));
    scope.put("client.account.0.product.BusinessParameters.GracePeriod", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.product.ChargeAdvanceRepayment.Interest", new Str("SettlementDate"));
    scope.put("client.account.0.product.ChargeAdvanceRepayment.Principal", new Str("Remaining"));
    scope.put("client.account.0.product.ChargeAdvanceRepayment.ServiceFee", new Str("CurrentTerm"));
    scope.put("client.account.0.product.ChargeAdvanceRepayment.Value_addedServiceFee", new Str("CurrentTerm"));
    scope.put("client.account.0.product.compulsorySettlement.CPD", new Num(BigDecimal.valueOf(91)));
    scope.put("client.account.0.product.Exemption.exemptionAmount", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.product.Exemption.exemptionDays", new Num(BigDecimal.valueOf(100)));
    scope.put("client.account.0.product.Fine.0.BaseType", new Str("RemainingPrincipal"));
    scope.put("client.account.0.product.Fine.0.HowToCalculate", new Str("FixedAmount"));
    scope.put("client.account.0.product.Fine.0.Max", new Num(BigDecimal.valueOf(10)));
    scope.put("client.account.0.product.Fine.0.Min", new Num(BigDecimal.valueOf(10)));
    scope.put("client.account.0.product.Fine.0.parameter", new Num(BigDecimal.valueOf(50)));
    scope.put("client.account.0.product.Fine.1.BaseType", new Str("RemainingPrincipal"));
    scope.put("client.account.0.product.Fine.1.HowToCalculate", new Str("FixedAmount"));
    scope.put("client.account.0.product.Fine.1.Max", new Num(BigDecimal.valueOf(30)));
    scope.put("client.account.0.product.Fine.1.Min", new Num(BigDecimal.valueOf(30)));
    scope.put("client.account.0.product.Fine.1.parameter", new Num(BigDecimal.valueOf(100)));
    scope.put("client.account.0.product.Fine.2.BaseType", new Str("RemainingPrincipal"));
    scope.put("client.account.0.product.Fine.2.HowToCalculate", new Str("FixedAmount"));
    scope.put("client.account.0.product.Fine.2.Max", new Num(BigDecimal.valueOf(20)));
    scope.put("client.account.0.product.Fine.2.Min", new Num(BigDecimal.valueOf(20)));
    scope.put("client.account.0.product.Fine.2.parameter", new Num(BigDecimal.valueOf(75)));
    scope.put("client.account.0.product.FineCalculationType", new Str("CPD"));
    scope.put("client.account.0.product.FlexibleRepaymentServicePacket.CalculationParameter", new Num(BigDecimal.valueOf(15)));
    scope.put("client.account.0.product.FlexibleRepaymentServicePacket.ChargeMethod", new Str("Terms"));
    scope.put("client.account.0.product.FlexibleRepaymentServicePacket.HowToCalculate", new Str("FixedAmount"));
    scope.put("client.account.0.product.Hesitation.value", new Num(BigDecimal.valueOf(15)));
    scope.put("client.account.0.product.id", new Str("KAPGVTagPbq3mwB5W"));
    scope.put("client.account.0.product.InterestRate", new Num(BigDecimal.valueOf(0.174)));
    scope.put("client.account.0.product.LifeInsuranceFee.CalculationParameter", new Num(BigDecimal.valueOf(0.007)));
    scope.put("client.account.0.product.LifeInsuranceFee.ChargeMethod", new Str("Terms"));
    scope.put("client.account.0.product.LifeInsuranceFee.HowToCalculate", new Str("BaseRatio"));
    scope.put("client.account.0.product.LoanAmount.MaxAmount", new Num(BigDecimal.valueOf(2500)));
    scope.put("client.account.0.product.LoanAmount.MinAmount", new Num(BigDecimal.valueOf(1000)));
    scope.put("client.account.0.product.LoanType", new Str("MCEI"));
    scope.put("client.account.0.product.productCode", new Str("1101"));
    scope.put("client.account.0.product.RepayManner", new Str("CPM"));
    scope.put("client.account.0.product.RepaymentTerms.Parameter", new Num(BigDecimal.valueOf(6)));
    scope.put("client.account.0.product.RepaymentTerms.Unit", new Str("Month"));
    scope.put("client.account.0.product.RuleInstalmentDate", new Str("Replace"));
    scope.put("client.account.0.product.ServiceFee.CalculationParameter", new Num(BigDecimal.valueOf(0.0265)));
    scope.put("client.account.0.product.ServiceFee.ChargeMethod", new Str("Terms"));
    scope.put("client.account.0.product.ServiceFee.HowToCalculate", new Str("BaseRatio"));
    scope.put("client.account.0.product.StampDutyFee.CalculationParameter", new Num(BigDecimal.valueOf(0.00005)));
    scope.put("client.account.0.product.StampDutyFee.ChargeMethod", new Str("FirstTerm"));
    scope.put("client.account.0.product.StampDutyFee.HowToCalculate", new Str("BaseRatio"));
    scope.put("client.account.0.product.subProductCode", new Str("11010601"));
    scope.put("client.account.0.product.TerminateAgeCd", new Num(BigDecimal.valueOf(4)));
    scope.put("client.account.0.product.ToleranceValue.CPDOverdueToleranceValue", new Num(BigDecimal.valueOf(50)));
    scope.put("client.account.0.product.ToleranceValue.DPDOverdueToleranceValue", new Num(BigDecimal.valueOf(50)));
    scope.put("client.account.0.product.type", new Str("Instalment"));
    scope.put("client.account.0.productCd", new Str("1101"));
    scope.put("client.account.0.purpose", new Str("PL01"));
    scope.put("client.account.0.remainingTerms", new Num(BigDecimal.valueOf(6)));
    scope.put("client.account.0.replacePenaltyRate", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.returnOverpayment", new Bool(false));
    scope.put("client.account.0.stampdutyRate", new Num(BigDecimal.valueOf(0.00005)));
    scope.put("client.account.0.subTerminalType", new Str("APP"));
    scope.put("client.account.0.sumOverpaid", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalPaidInstallmentFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalPaidInterest", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalPaidLifeInsuranceFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalPaidPrepayPkgFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalPaidPrincipalAmt", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalPaidReplaceServiceFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalPaidServiceFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalPaidStampDutyFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalRemainingAmt", new Num(BigDecimal.valueOf(8532.76)));
    scope.put("client.account.0.totalRemInstallmentFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalRemInterest", new Num(BigDecimal.valueOf(132.61)));
    scope.put("client.account.0.totalRemLifeInsuranceFee", new Num(BigDecimal.valueOf(2700.00)));
    scope.put("client.account.0.totalRemPrepayPkgFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalRemPrincipalAmt", new Num(BigDecimal.valueOf(3000.00)));
    scope.put("client.account.0.totalRemReplaceServiceFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalRemServiceFee", new Num(BigDecimal.valueOf(2700.00)));
    scope.put("client.account.0.totalRemStampDutyFee", new Num(BigDecimal.valueOf(0.15)));
    scope.put("client.account.0.transaction.0.acctNbr", new Str("10000000"));
    scope.put("client.account.0.transaction.0.acctType", new Str("E"));
    scope.put("client.account.0.transaction.0.acqId", new Str("10000000"));
    scope.put("client.account.0.transaction.0.businessDate", new Dat(LocalDate.parse("2016-08-01")));
    scope.put("client.account.0.transaction.0.cardNo", new Str("6225882121996634"));
    scope.put("client.account.0.transaction.0.cardType", new Str("0"));
    scope.put("client.account.0.transaction.0.certId", new Str("999701976092680004"));
    scope.put("client.account.0.transaction.0.certType", new Str("06"));
    scope.put("client.account.0.transaction.0.city", new Str("1"));
    scope.put("client.account.0.transaction.0.code", new Str("0"));
    scope.put("client.account.0.transaction.0.commandType", new Str("SPA"));
    scope.put("client.account.0.transaction.0.comparedInd", new Str("N"));
    scope.put("client.account.0.transaction.0.contrNbr", new Str("checkContrNbr"));
    scope.put("client.account.0.transaction.0.currency", new Str("156"));
    scope.put("client.account.0.transaction.0.failureAmt", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.transaction.0.flag", new Str("00"));
    scope.put("client.account.0.transaction.0.id", new Str("d0m2Bemk47k41sPiC"));
    scope.put("client.account.0.transaction.0.loanUsage", new Str("L"));
    scope.put("client.account.0.transaction.0.matchInd", new Str("Y"));
    scope.put("client.account.0.transaction.0.openBankId", new Str("0100"));
    scope.put("client.account.0.transaction.0.orderStatus", new Str("S"));
    scope.put("client.account.0.transaction.0.orderTime", new Dat(LocalDate.parse("2016-09-01")));
    scope.put("client.account.0.transaction.0.org", new Str("000000000001"));
    scope.put("client.account.0.transaction.0.payBizCode", new Str("1"));
    scope.put("client.account.0.transaction.0.payChannelId", new Str("1"));
    scope.put("client.account.0.transaction.0.priv1", new Str("T110E5"));
    scope.put("client.account.0.transaction.0.purpose", new Str("放款申请"));
    scope.put("client.account.0.transaction.0.refNbr", new Str("1609011338269589973482"));
    scope.put("client.account.0.transaction.0.sendTime", new Dat(LocalDate.parse("2016-09-01")));
    scope.put("client.account.0.transaction.0.setupDate", new Dat(LocalDate.parse("2016-08-01")));
    scope.put("client.account.0.transaction.0.state", new Str("1"));
    scope.put("client.account.0.transaction.0.subBank", new Str("000000001"));
    scope.put("client.account.0.transaction.0.successAmt", new Num(BigDecimal.valueOf(3000.00)));
    scope.put("client.account.0.transaction.0.txnAmt", new Num(BigDecimal.valueOf(3000)));
    scope.put("client.account.0.transaction.0.txnType", new Str("AgentDebit"));
    scope.put("client.account.0.transaction.0.usrName", new Str("Beam"));
    scope.put("client.account.0.transaction.1.acctNbr", new Str("10000000"));
    scope.put("client.account.0.transaction.1.acctType", new Str("E"));
    scope.put("client.account.0.transaction.1.acqId", new Str("10000000"));
    scope.put("client.account.0.transaction.1.businessDate", new Dat(LocalDate.parse("2016-09-01")));
    scope.put("client.account.0.transaction.1.cardNo", new Str("6225882121996634"));
    scope.put("client.account.0.transaction.1.cardType", new Str("0"));
    scope.put("client.account.0.transaction.1.certId", new Str("999701976092680004"));
    scope.put("client.account.0.transaction.1.certType", new Str("06"));
    scope.put("client.account.0.transaction.1.city", new Str("1"));
    scope.put("client.account.0.transaction.1.commandType", new Str("SDB"));
    scope.put("client.account.0.transaction.1.comparedInd", new Str("N"));
    scope.put("client.account.0.transaction.1.contrNbr", new Str("checkContrNbr"));
    scope.put("client.account.0.transaction.1.currency", new Str("156"));
    scope.put("client.account.0.transaction.1.failureAmt", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.transaction.1.flag", new Str("00"));
    scope.put("client.account.0.transaction.1.id", new Str("G63l0Bjak9KLY3Aos"));
    scope.put("client.account.0.transaction.1.loanUsage", new Str("N"));
    scope.put("client.account.0.transaction.1.matchInd", new Str("Y"));
    scope.put("client.account.0.transaction.1.openBankId", new Str("0100"));
    scope.put("client.account.0.transaction.1.orderStatus", new Str("S"));
    scope.put("client.account.0.transaction.1.orderTime", new Dat(LocalDate.parse("2016-09-01")));
    scope.put("client.account.0.transaction.1.org", new Str("000000000001"));
    scope.put("client.account.0.transaction.1.payBizCode", new Str("0"));
    scope.put("client.account.0.transaction.1.payChannelId", new Str("1"));
    scope.put("client.account.0.transaction.1.priv1", new Str("T110E5"));
    scope.put("client.account.0.transaction.1.purpose", new Str("正常扣款"));
    scope.put("client.account.0.transaction.1.sendTime", new Dat(LocalDate.parse("2016-09-01")));
    scope.put("client.account.0.transaction.1.setupDate", new Dat(LocalDate.parse("2016-09-01")));
    scope.put("client.account.0.transaction.1.state", new Str("1"));
    scope.put("client.account.0.transaction.1.subBank", new Str("000000001"));
    scope.put("client.account.0.transaction.1.successAmt", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.transaction.1.txnType", new Str("AgentCredit"));
    scope.put("client.account.0.transaction.1.txnAmt", new Num(BigDecimal.valueOf(1422.25)));
    scope.put("client.account.0.transaction.1.usrName", new Str("Beam"));
    scope.put("client.areacode", new Str("110229"));
    scope.put("client.birthday", new Dat(LocalDate.parse("1976-09-26")));
    scope.put("client.city", new Str("110101"));
    scope.put("client.commAddr", new Str("C"));
    scope.put("client.country", new Str("156"));
    scope.put("client.customername", new Str("silver"));
    scope.put("client.duedate", new Dat(LocalDate.parse("2015-09-25")));
    scope.put("client.eduExperience", new Str("A"));
    scope.put("client.familyaddr", new Str("湖南省衡阳市衡南县"));
    scope.put("client.familystatus", new Str("A"));
    scope.put("client.familytel", new Str("021-6228597"));
    scope.put("client.gender", new Str("M"));
    scope.put("client.idNo", new Str("999701976092680004"));
    scope.put("client.idType", new Str("I"));
    scope.put("client.link1", new Str("李四"));
    scope.put("client.linkmobile1", new Str("13244556677"));
    scope.put("client.linkrelation1", new Str("F"));
    scope.put("client.maritalStatus", new Str("C"));
    scope.put("client.mobileNo", new Str("13760376864"));
    scope.put("client.positionLevel", new Str("A"));
    scope.put("client.province", new Str("110000"));
    scope.put("client.unitkind", new Str("A"));
    scope.put("client.workAddr", new Str("浦东新区高科"));
    scope.put("client.workbegindate", new Str("4"));
    scope.put("client.workCorp", new Str("长亮科技长亮科技"));
    scope.put("client.worknature", new Str("A"));
    scope.put("client.workTel", new Str("568944123"));


  }


  public static void fillPayPlanScope(SortedMap<String, DtType> scope) {
    scope.put("client.account.0.payPlan.0.hasPaid", new Bool(false));

    scope.put("client.account.0.payPlan.0.paidTotal", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.0.paidInterest", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.0.paidPrincipal", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.0.paidServiceFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.0.paidPrepayPkgFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.0.paidLifeInsurance", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.0.paidReplaceSvcFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.0.paidInstallmentFee", new Num(BigDecimal.valueOf(0)));

    scope.put("client.account.0.payPlan.0.termNumber", new Num(BigDecimal.valueOf(1)));
    scope.put("client.account.0.payPlan.0.payPlanDueDate", new Dat(LocalDate.now().plusMonths(1)));
    scope.put("client.account.0.payPlan.0.termLastPaymentDate", new Dat(LocalDate.of(1990, 12, 11)));

    scope.put("client.account.0.payPlan.0.remainingTotal", new Num(BigDecimal.valueOf(1230)));

    scope.put("client.account.0.payPlan.0.remainingInterest", new Num(BigDecimal.valueOf(100)));
    scope.put("client.account.0.payPlan.0.remainingPrincipal", new Num(BigDecimal.valueOf(1000)));

    scope.put("client.account.0.payPlan.0.remainingServiceFee", new Num(BigDecimal.valueOf(100)));
    scope.put("client.account.0.payPlan.0.remainingPrepayPkgFee", new Num(BigDecimal.valueOf(5)));
    scope.put("client.account.0.payPlan.0.remainingStampDutyFee", new Num(BigDecimal.valueOf(5)));
    scope.put("client.account.0.payPlan.0.remainingLifeInsurance", new Num(BigDecimal.valueOf(10)));
    scope.put("client.account.0.payPlan.0.remainingReplaceSvcFee", new Num(BigDecimal.valueOf(10)));
    scope.put("client.account.0.payPlan.0.remainingInstallmentFee", new Num(BigDecimal.valueOf(0)));


    //2nd
    scope.put("client.account.0.payPlan.1.hasPaid", new Bool(false));

    scope.put("client.account.0.payPlan.1.paidTotal", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.1.paidInterest", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.1.paidPrincipal", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.1.paidServiceFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.1.paidPrepayPkgFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.1.paidLifeInsurance", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.1.paidReplaceSvcFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.1.paidInstallmentFee", new Num(BigDecimal.valueOf(0)));

    scope.put("client.account.0.payPlan.1.termNumber", new Num(BigDecimal.valueOf(2)));
    scope.put("client.account.0.payPlan.1.payPlanDueDate", new Dat(LocalDate.now()));
    scope.put("client.account.0.payPlan.1.termLastPaymentDate", new Dat(LocalDate.of(1990, 12, 11)));

    scope.put("client.account.0.payPlan.1.remainingTotal", new Num(BigDecimal.valueOf(1230)));

    scope.put("client.account.0.payPlan.1.remainingInterest", new Num(BigDecimal.valueOf(100)));
    scope.put("client.account.0.payPlan.1.remainingPrincipal", new Num(BigDecimal.valueOf(1000)));

    scope.put("client.account.0.payPlan.1.remainingServiceFee", new Num(BigDecimal.valueOf(100)));
    scope.put("client.account.0.payPlan.1.remainingPrepayPkgFee", new Num(BigDecimal.valueOf(5)));
    scope.put("client.account.0.payPlan.1.remainingStampDutyFee", new Num(BigDecimal.valueOf(5)));
    scope.put("client.account.0.payPlan.1.remainingLifeInsurance", new Num(BigDecimal.valueOf(10)));
    scope.put("client.account.0.payPlan.1.remainingReplaceSvcFee", new Num(BigDecimal.valueOf(10)));
    scope.put("client.account.0.payPlan.1.remainingInstallmentFee", new Num(BigDecimal.valueOf(0)));

    //3rd
    scope.put("client.account.0.payPlan.2.hasPaid", new Bool(false));

    scope.put("client.account.0.payPlan.2.paidTotal", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.2.paidInterest", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.2.paidPrincipal", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.2.paidServiceFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.2.paidPrepayPkgFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.2.paidLifeInsurance", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.2.paidReplaceSvcFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.payPlan.2.paidInstallmentFee", new Num(BigDecimal.valueOf(0)));

    scope.put("client.account.0.payPlan.2.termNumber", new Num(BigDecimal.valueOf(2)));
    scope.put("client.account.0.payPlan.2.payPlanDueDate", new Dat(LocalDate.now().minusMonths(1)));
    scope.put("client.account.0.payPlan.2.termLastPaymentDate", new Dat(LocalDate.of(1990, 12, 11)));

    scope.put("client.account.0.payPlan.2.remainingTotal", new Num(BigDecimal.valueOf(1230)));

    scope.put("client.account.0.payPlan.2.remainingInterest", new Num(BigDecimal.valueOf(100)));
    scope.put("client.account.0.payPlan.2.remainingPrincipal", new Num(BigDecimal.valueOf(1000)));

    scope.put("client.account.0.payPlan.2.remainingServiceFee", new Num(BigDecimal.valueOf(100)));
    scope.put("client.account.0.payPlan.2.remainingPrepayPkgFee", new Num(BigDecimal.valueOf(5)));
    scope.put("client.account.0.payPlan.2.remainingStampDutyFee", new Num(BigDecimal.valueOf(5)));
    scope.put("client.account.0.payPlan.2.remainingLifeInsurance", new Num(BigDecimal.valueOf(10)));
    scope.put("client.account.0.payPlan.2.remainingReplaceSvcFee", new Num(BigDecimal.valueOf(10)));
    scope.put("client.account.0.payPlan.2.remainingInstallmentFee", new Num(BigDecimal.valueOf(0)));
  }


  public static void fillScope(SortedMap<String, DtType> scope) {
    scope.put("in.accountIndex", new Num(BigDecimal.valueOf(0)));
    scope.put("in.transactionIndex", new Num(BigDecimal.valueOf(0)));
    scope.put("in.transactionId", new Str("transactionId"));
    scope.put("in.channelSerial", new Str("channelSerial"));
    scope.put("in.channelDate", new Dat(LocalDate.now()));
    scope.put("in.txnAmt", new Num(BigDecimal.valueOf(100)));
    scope.put("in.msDdReturnCode", new Str("0"));
    scope.put("in.returnMessage", new Str("This is return message"));
    scope.put("in.channelDate", new Dat(LocalDate.now()));

    scope.put("client.account.0.cpdBeginDate", new Dat(LocalDate.now().minusDays(10)));

    scope.put("client.account.0.transaction.0.loanUsage", new Str("N"));

    scope.put("client.account.0.paidPrincipalAmt", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.paidInterest", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.paidInstallmentFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.paidInsuranceFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.paidStampDutyFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.paidServiceFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.paidLifeInsuranceFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.paidTotalPrepayPkgFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.paidReplaceServiceFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.paidSumOfDebtTillBizDate", new Num(BigDecimal.valueOf(0)));

    scope.put("client.account.0.paidTerms", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.leftTerms", new Num(BigDecimal.valueOf(10)));
    scope.put("client.account.0.nextStmtDate", new Dat(LocalDate.now()));
    scope.put("client.account.0.graceDate", new Dat(LocalDate.now().minusMonths(10)));

    scope.put("client.account.0.leftPrincipalAmt", new Num(BigDecimal.valueOf(2000)));
    scope.put("client.account.0.leftInterest", new Num(BigDecimal.valueOf(100)));
    scope.put("client.account.0.leftInstallmentFee", new Num(BigDecimal.valueOf(60)));
    scope.put("client.account.0.leftReplaceServiceFee", new Num(BigDecimal.valueOf(50)));
    scope.put("client.account.0.leftInsuranceFee", new Num(BigDecimal.valueOf(40)));
    scope.put("client.account.0.leftStampDutyFee", new Num(BigDecimal.valueOf(30)));
    scope.put("client.account.0.leftServiceFee", new Num(BigDecimal.valueOf(20)));
    scope.put("client.account.0.leftLifeInsuranceFee", new Num(BigDecimal.valueOf(10)));

    scope.put("client.account.0.transaction.0.termPrincipalAmt", new Num(BigDecimal.valueOf(100)));
    scope.put("client.account.0.transaction.0.loanTermInterest", new Num(BigDecimal.valueOf(10)));
    scope.put("client.account.0.transaction.0.loanTermInstallmentFee", new Num(BigDecimal.valueOf(6)));
    scope.put("client.account.0.transaction.0.loanTermInsuranceFee", new Num(BigDecimal.valueOf(4)));
    scope.put("client.account.0.transaction.0.loanStampDutyFee", new Num(BigDecimal.valueOf(3)));
    scope.put("client.account.0.transaction.0.loanTermServiceFee", new Num(BigDecimal.valueOf(2)));
    scope.put("client.account.0.transaction.0.loanLifeInsuranceFee", new Num(BigDecimal.valueOf(1)));
    scope.put("client.account.0.transaction.0.loanTermTotalPrepayPkgFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.transaction.0.loanReplaceServiceFee", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.sumOfDebtTillBizDate", new Num(BigDecimal.valueOf(0)));


    scope.put("client.account.0.transaction.0.optDatetime", new Dat(LocalDate.now().minusDays(3)));
    scope.put("client.account.0.transaction.0.orderStatus", new Str("W"));


    scope.put("client.account.0.product", new Str("ProductCode"));
    scope.put("client.account.0.product.FineCalculationType", new Str("CPD"));

    scope.put("client.account.0.product.fine.0.Max", new Num(BigDecimal.valueOf(20)));
    scope.put("client.account.0.product.fine.0.Min", new Num(BigDecimal.valueOf(10)));
    scope.put("client.account.0.product.fine.0.parameter", new Num(BigDecimal.valueOf(50)));

    scope.put("client.account.0.product.fine.1.Max", new Num(BigDecimal.valueOf(30)));
    scope.put("client.account.0.product.fine.1.Min", new Num(BigDecimal.valueOf(21)));
    scope.put("client.account.0.product.fine.1.parameter", new Num(BigDecimal.valueOf(75)));

    scope.put("client.account.0.product.fine.2.Max", new Num(BigDecimal.valueOf(40)));
    scope.put("client.account.0.product.fine.2.Min", new Num(BigDecimal.valueOf(31)));
    scope.put("client.account.0.product.fine.2.parameter", new Num(BigDecimal.valueOf(100)));


    scope.put("client.account.0.product.fine.length", new Num(BigDecimal.valueOf(3)));


    scope.put("client.account.0.acctNbr", new Str("acctNbr"));
    scope.put("client.account.0.acctType", new Str("acctType"));
    scope.put("client.account.0.acqId", new Str("acqId"));
    scope.put("client.account.0.businessDate", new Dat(LocalDate.now()));
    scope.put("client.account.0.ddBankAcctNbr", new Str("cardNo"));
    scope.put("client.idNo", new Str("certId"));
    scope.put("client.IdType", new Str("certType"));
    scope.put("client.account.0.custSource", new Str("channelId"));
    scope.put("client.account.0.ddBankCity", new Str("city"));
    scope.put("client.account.0.contrNbr", new Str("contrNbr"));
    scope.put("client.account.0.loanUsage", new Str("loanUsage"));
    scope.put("client.account.0.ddBankBranch", new Str("openBankId"));
    scope.put("client.account.0.ddBankName", new Str("openBankName"));
    scope.put("client.account.0.optDateTime", new Str("optDateTime"));
    scope.put("client.account.0.orderStatus", new Str("orderStatus"));
    scope.put("client.account.0.setupDate", new Str("setupDate"));
    scope.put("client.account.0.ddBankProvince", new Str("state"));
    scope.put("client.account.0.owningBranch", new Str("subBank"));
    scope.put("client.account.0.ddBankAcctName", new Str("usrName"));


  }

  public static void fillForPayTransFlow(SortedMap<String, DtType> scope) {
    scope.put("client.account.0.acctNbr", new Str("wFsnhgVhs6LqLwmBZ"));
    scope.put("client.account.0.acctType ", new Str("E"));
    scope.put("client.account.0.acqId", new Str("10000000"));
    scope.put("client.account.0.agreementRateExpireDate", new Dat(LocalDate.now()));
    scope.put("client.account.0.applicationNo", new Str("8997882"));
    scope.put("client.account.0.applyDate", new Dat(LocalDate.now()));
    scope.put("client.account.0.batchDate", new Dat(LocalDate.now()));
    scope.put("client.account.0.compoundRate", new Num(BigDecimal.valueOf(0.15)));
    scope.put("client.account.0.contrNbr ", new Str("11011608016LqLwmBZ"));
    scope.put("client.account.0.corpName ", new Str("长亮科技长亮科技"));
    scope.put("client.account.0.createTime", new Dat(LocalDate.now()));
    scope.put("client.account.0.currentTerm", new Num(BigDecimal.valueOf(1)));
    scope.put("client.account.0.cycleDay", new Num(BigDecimal.valueOf(1)));
    scope.put("client.account.0.ddBankAcctName ", new Str("Beam"));
    scope.put("client.account.0.ddBankAcctNbr", new Str("6225882121996634"));
    scope.put("client.account.0.ddBankBranch ", new Str("0100"));
    scope.put("client.account.0.ddBankCity ", new Str("1"));
    scope.put("client.account.0.ddBankCityCode ", new Str("110101"));
    scope.put("client.account.0.ddBankName ", new Str("1"));
    scope.put("client.account.0.ddBankProvince ", new Str("1"));
    scope.put("client.account.0.ddBankProvinceCode ", new Str("110000"));
    scope.put("client.account.0.feeRate", new Num(BigDecimal.valueOf(0.90)));
    scope.put("client.account.0.gender ", new Str("M"));
    scope.put("client.account.0.graceDate", new Dat(LocalDate.now()));
    scope.put("client.account.0.hasAdvancedClearing", new Bool(false));
    scope.put("client.account.0.id ", new Str("VOFKPGiv3xsGKqGTW"));
    scope.put("client.account.0.installmentFeeAmt", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.interestRate", new Num(BigDecimal.valueOf(0.15)));
    scope.put("client.account.0.isAbnormal", new Bool(false));
    scope.put("client.account.0.isFreezed", new Bool(false));
    scope.put("client.account.0.joinLifeInsuInd", new Bool(true));
    scope.put("client.account.0.loanAmt", new Num(BigDecimal.valueOf(3000)));
    scope.put("client.account.0.loanFeeDefId ", new Str("11010601"));
    scope.put("client.account.0.loanTerm", new Num(BigDecimal.valueOf(6)));
    scope.put("client.account.0.mobileNo ", new Str("13760376864"));
    scope.put("client.account.0.name ", new Str("silver"));
    scope.put("client.account.0.nextStmtDate", new Dat(LocalDate.now()));
    scope.put("client.account.0.owningBranch ", new Str("000000001"));
    scope.put("client.account.0.paidSumOfDebtTillBizDate", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.paidTerms", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.penaltyRate", new Num(BigDecimal.valueOf(0.15)));
    scope.put("client.account.0.pmtDueDate", new Dat(LocalDate.now()));
    scope.put("client.account.0.product.advanceRepaymentApply.applyBefore", new Num(BigDecimal.valueOf(5)));
    scope.put("client.account.0.product.advanceRepaymentApply.canToApply", new Bool(true));
    scope.put("client.account.0.product.advanceRepaymentApply.deductBefore", new Num(BigDecimal.valueOf(3)));
    scope.put("client.account.0.product.AdvanceRepaymentServiceFee.0.BaseType", new Str("RemainingPrincipal"));
    scope.put("client.account.0.product.AdvanceRepaymentServiceFee.0.HowToCalculate", new Str("FixedAmount"));
    scope.put("client.account.0.product.AdvanceRepaymentServiceFee.0.MaxLoanPeriod", new Num(BigDecimal.valueOf(99)));
    scope.put("client.account.0.product.AdvanceRepaymentServiceFee.0.parameter", new Num(BigDecimal.valueOf(200)));
    scope.put("client.account.0.product.BusinessParameters.GracePeriod", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.product.ChargeAdvanceRepayment.Interest", new Str("SettlementDate"));
    scope.put("client.account.0.product.ChargeAdvanceRepayment.Principal ", new Str("Remaining"));
    scope.put("client.account.0.product.ChargeAdvanceRepayment.ServiceFee", new Str("CurrentTerm"));
    scope.put("client.account.0.product.ChargeAdvanceRepayment.Value_addedServiceFee ", new Str("CurrentTerm"));
    scope.put("client.account.0.product.compulsorySettlement.CPD", new Num(BigDecimal.valueOf(91)));
    scope.put("client.account.0.product.Exemption.exemptionAmount", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.product.Exemption.exemptionDays", new Num(BigDecimal.valueOf(100)));
    scope.put("client.account.0.product.Fine.0.BaseType", new Str("RemainingPrincipal"));
    scope.put("client.account.0.product.Fine.0.HowToCalculate", new Str("FixedAmount"));
    scope.put("client.account.0.product.Fine.0.Max", new Num(BigDecimal.valueOf(10)));
    scope.put("client.account.0.product.Fine.0.Min", new Num(BigDecimal.valueOf(10)));
    scope.put("client.account.0.product.Fine.0.parameter", new Num(BigDecimal.valueOf(50)));
    scope.put("client.account.0.product.Fine.1.BaseType", new Str("RemainingPrincipal"));
    scope.put("client.account.0.product.Fine.1.HowToCalculate", new Str("FixedAmount"));
    scope.put("client.account.0.product.Fine.1.Max", new Num(BigDecimal.valueOf(30)));
    scope.put("client.account.0.product.Fine.1.Min", new Num(BigDecimal.valueOf(30)));
    scope.put("client.account.0.product.Fine.1.parameter", new Num(BigDecimal.valueOf(100)));
    scope.put("client.account.0.product.Fine.2.BaseType", new Str("RemainingPrincipal"));
    scope.put("client.account.0.product.Fine.2.HowToCalculate", new Str("FixedAmount"));
    scope.put("client.account.0.product.Fine.2.Max", new Num(BigDecimal.valueOf(20)));
    scope.put("client.account.0.product.Fine.2.Min", new Num(BigDecimal.valueOf(20)));
    scope.put("client.account.0.product.Fine.2.parameter", new Num(BigDecimal.valueOf(75)));
    scope.put("client.account.0.product.FineCalculationType", new Str("CPD"));
    scope.put("client.account.0.product.FlexibleRepaymentServicePacket.CalculationParameter", new Num(BigDecimal.valueOf(15)));
    scope.put("client.account.0.product.FlexibleRepaymentServicePacket.ChargeMethod", new Str("Terms"));
    scope.put("client.account.0.product.FlexibleRepaymentServicePacket.HowToCalculate", new Str("FixedAmount"));
    scope.put("client.account.0.product.Hesitation.value", new Num(BigDecimal.valueOf(15)));
    scope.put("client.account.0.product.id ", new Str("KAPGVTagPbq3mwB5W"));
    scope.put("client.account.0.product.InterestRate", new Num(BigDecimal.valueOf(0.174)));
    scope.put("client.account.0.product.LifeInsuranceFee.CalculationParameter", new Num(BigDecimal.valueOf(0.007)));
    scope.put("client.account.0.product.LifeInsuranceFee.ChargeMethod", new Str("Terms"));
    scope.put("client.account.0.product.LifeInsuranceFee.HowToCalculate", new Str("BaseRatio"));
    scope.put("client.account.0.product.LoanAmount.MaxAmount", new Num(BigDecimal.valueOf(2500)));
    scope.put("client.account.0.product.LoanAmount.MinAmount", new Num(BigDecimal.valueOf(1000)));
    scope.put("client.account.0.product.LoanType ", new Str("MCEI"));
    scope.put("client.account.0.product.productCode", new Str("1101"));
    scope.put("client.account.0.product.RepayManner", new Str("CPM"));
    scope.put("client.account.0.product.RepaymentTerms.Parameter", new Num(BigDecimal.valueOf(6)));
    scope.put("client.account.0.product.RepaymentTerms.Unit", new Str("Month"));
    scope.put("client.account.0.product.RuleInstalmentDate ", new Str("Replace"));
    scope.put("client.account.0.product.ServiceFee.CalculationParameter", new Num(BigDecimal.valueOf(0.0265)));
    scope.put("client.account.0.product.ServiceFee.ChargeMethod", new Str("Terms"));
    scope.put("client.account.0.product.ServiceFee.HowToCalculate", new Str("BaseRatio"));
    scope.put("client.account.0.product.StampDutyFee.CalculationParameter", new Num(BigDecimal.valueOf(0.00005)));
    scope.put("client.account.0.product.StampDutyFee.ChargeMethod", new Str("FirstTerm"));
    scope.put("client.account.0.product.StampDutyFee.HowToCalculate", new Str("BaseRatio"));
    scope.put("client.account.0.product.subProductCode ", new Str("11010601"));
    scope.put("client.account.0.product.TerminateAgeCd", new Num(BigDecimal.valueOf(4)));
    scope.put("client.account.0.product.ToleranceValue.CPDOverdueToleranceValue", new Num(BigDecimal.valueOf(50)));
    scope.put("client.account.0.product.ToleranceValue.DPDOverdueToleranceValue", new Num(BigDecimal.valueOf(50)));
    scope.put("client.account.0.product.type ", new Str("Instalment"));
    scope.put("client.account.0.productCd", new Str("1101"));
    scope.put("client.account.0.purpose", new Str("PL01"));
    scope.put("client.account.0.remainingTerms", new Num(BigDecimal.valueOf(6)));
    scope.put("client.account.0.replacePenaltyRate", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.returnOverpayment", new Bool(false));
    scope.put("client.account.0.stampdutyRate", new Num(BigDecimal.valueOf(0.00005)));
    scope.put("client.account.0.subTerminalType", new Str("APP"));
    scope.put("client.account.0.sumOverpaid", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalPaidInstallmentFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalPaidInterest", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalPaidLifeInsuranceFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalPaidPrepayPkgFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalPaidPrincipalAmt", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalPaidReplaceServiceFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalPaidServiceFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.totalPaidStampDutyFee", new Num(BigDecimal.valueOf(0.00)));
    scope.put("client.account.0.transaction.0.acctNbr", new Str("wFsnhgVhs6LqLwmBZ"));
    scope.put("client.account.0.transaction.0.acctType ", new Str("E"));
    scope.put("client.account.0.transaction.0.acqId", new Str("10000000"));
    scope.put("client.account.0.transaction.0.businessDate", new Dat(LocalDate.now()));
    scope.put("client.account.0.transaction.0.cardNo ", new Str("6225882121996634"));
    scope.put("client.account.0.transaction.0.city ", new Str("1"));
    scope.put("client.account.0.transaction.0.commandType", new Str("SPA"));
    scope.put("client.account.0.transaction.0.comparedInd", new Str("N"));
    scope.put("client.account.0.transaction.0.contrNbr ", new Str("11011608016LqLwmBZ"));
    scope.put("client.account.0.transaction.0.currency ", new Str("156"));
    scope.put("client.account.0.transaction.0.failureAmt", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.transaction.0.flag ", new Str("00"));
    scope.put("client.account.0.transaction.0.id ", new Str("36WlIxWB3Ue8kVish"));
    scope.put("client.account.0.transaction.0.loanUsage", new Str("L"));
    scope.put("client.account.0.transaction.0.matchInd ", new Str("Y"));
    scope.put("client.account.0.transaction.0.openBankId ", new Str("0100"));
    scope.put("client.account.0.transaction.0.openBankName ", new Str("1"));
    scope.put("client.account.0.transaction.0.orderStatus", new Str("W"));
    scope.put("client.account.0.transaction.0.orderTime", new Dat(LocalDate.now()));
    scope.put("client.account.0.transaction.0.payBizCode ", new Str("1"));
    scope.put("client.account.0.transaction.0.priv1", new Str("T110E5"));
    scope.put("client.account.0.transaction.0.purpose", new Str("放款申请"));
    scope.put("client.account.0.transaction.0.sendTime", new Dat(LocalDate.now()));
    scope.put("client.account.0.transaction.0.setupDate", new Dat(LocalDate.now()));
    scope.put("client.account.0.transaction.0.state", new Str("1"));
    scope.put("client.account.0.transaction.0.subBank", new Str("000000001"));
    scope.put("client.account.0.transaction.0.successAmt", new Num(BigDecimal.valueOf(0)));
    scope.put("client.account.0.transaction.0.txnAmt", new Num(BigDecimal.valueOf(3000)));
    scope.put("client.account.0.transaction.0.txnType", new Str("AgentDebit"));
    scope.put("client.account.0.transaction.0.usrName", new Str("Beam"));
    scope.put("client.uuid ", new Str("ll997b2289904836a8eb42276d458004"));
  }

}

