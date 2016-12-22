package kz.greetgo.dt;

import org.testng.annotations.Test;
import scala.math.BigDecimal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;


public class DtMSLoanOpeningTest {
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
  public void test() {
    DtManager1 m = new DtManager1(nativeFunctions);

    m.registerNativeProcedure("nativeLog", scope -> {
      System.out.println(scope);
      scope.set("logged", new Bool(true));
    });

    m.registerUserProcedure("userLog", "condition(case(x=1, procedure(nativeLog)))");

    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("x", new Num(new BigDecimal(new java.math.BigDecimal(1))));


    m.registerTree("main", "procedure(userLog)");

    m.executeTree("main", scope);

    System.out.println(scope);
  }

  private String resource(String name) throws URISyntaxException, IOException {
    java.net.URL url = getClass().getResource(name);
    java.nio.file.Path resPath = java.nio.file.Paths.get(url.toURI());
    return new String(java.nio.file.Files.readAllBytes(resPath), "UTF8");
  }

  /*
  Вот это запускать!
   */

  @Test(expectedExceptions = ErrorException.class)
  public void createAccount() throws IOException, URISyntaxException {
    DtManager1 m = new DtManager1(nativeFunctions);

    m.registerNativeProcedure("payResultProc", stringDtTypeSortedMap -> System.out.println("Execute native payResultProc"));

    m.registerNativeProcedure("sendSMS", stringDtTypeSortedMap -> System.out.println("Execute native sendSMS"));

    m.registerNativeProcedure("genContrNbr", scope -> {
      String acctNbr = ((Str) scope.get("client.tmp.acctNbr")).str();
      String loanCode = ((Str) scope.get("client.tmp.loanCode")).str();
      scope.set("client.tmp.contrNbr", new Str(genContrNbr(acctNbr, nativeFunctions.businessDay(), loanCode)));
    });
    m.registerNativeProcedure("genRefnbr", scope -> {
      String refNbr = genRefnbr(nativeFunctions.today(), ((Str) scope.get("client.tmp.serviceSn")).str());
      scope.set("client.tmp.refNbr", new Str(refNbr));
    });

    m.registerUserProcedure("createAccountInner", resource("createAccountInner.txt"));
    m.registerUserProcedure("generateTransaction", resource("createPaymentTransaction.txt"));

    SortedMap<String, DtType> scope = new TreeMap<>();
    fillScope(scope);

    String code = resource("createAccount.txt");
    m.registerTree("main", code);
    DtExecuteResult exeResult = m.executeTree("main", scope);


    for (String s : scope.keySet()) {
      System.out.println(s + " = " + scope.get(s));
    }

    System.out.println("getMessages = " + exeResult.messages);
  }


  private void fillScope(SortedMap<String, DtType> scope) {
    scope.put("client.tmp.customername", new Str("Василий"));
    scope.put("client.tmp.uuid", new Str("0000001"));
    scope.put("client.tmp.gender", new Str("M"));
    scope.put("client.tmp.birthday", new Dat(LocalDate.of(1990, 12, 11)));
    scope.put("client.tmp.maritalStatus", new Str("W"));
    scope.put("client.tmp.country", new Str("156"));
    scope.put("client.tmp.idNo", new Str("123456789012345678"));
    scope.put("client.tmp.idType", new Str("I"));
    scope.put("client.tmp.duedate", new Dat(LocalDate.of(2016, 7, 18)));
    scope.put("client.tmp.cellPhone", new Str("+77015135154"));
    scope.put("client.tmp.zipcode", new Str("0500001"));
    scope.put("client.tmp.province", new Str("Provinde"));
    scope.put("client.tmp.city", new Str("City"));
    scope.put("client.tmp.areacode", new Str("AreaCode"));
    scope.put("client.tmp.familyaddr", new Str("FamilyAddress"));
    scope.put("client.tmp.familystatus", new Str("A"));
    scope.put("client.tmp.familytel", new Str("+77015135154"));
    scope.put("client.tmp.eduExperience", new Str("B"));
    scope.put("client.tmp.eduDegree", new Str("A"));
    scope.put("client.tmp.commAddr", new Str("H"));
    scope.put("client.tmp.workAddr", new Str("Chernomorskay 12b"));
    scope.put("client.tmp.workCorp", new Str("greetgo!"));
    scope.put("client.tmp.workZip", new Str("0500001"));
    scope.put("client.tmp.workTel", new Str("3861386"));
    scope.put("client.tmp.workbegindate", new Str("10"));
    scope.put("client.tmp.positionLevel", new Str("A"));
    scope.put("client.tmp.position", new Str("A"));
    scope.put("client.tmp.worknature", new Str("B"));
    scope.put("client.tmp.unitkind", new Str("C"));
    scope.put("client.tmp.link1", new Str("Дядя Стёпа"));
    scope.put("client.tmp.linkrelation1", new Str("T"));
    scope.put("client.tmp.linkmobile1", new Str("77011077701"));
    scope.put("client.tmp.link2", new Str("Тётя Мотя"));
    scope.put("client.tmp.linkrelation2", new Str("C"));
    scope.put("client.tmp.linkmobile2", new Str("77017701770"));
    scope.put("client.tmp.applyDate", new Dat(LocalDate.of(2016, 6, 18)));
    scope.put("client.tmp.applyNo", new Str("12345678901234567890123456789012"));
    scope.put("client.tmp.contraExpireDate", new Dat(LocalDate.of(2016, 12, 18)));
    scope.put("client.tmp.businessType", new Str("BusinessType"));
    scope.put("client.tmp.loanCode", new Str("1101"));
    scope.put("client.tmp.loanFeeDefId", new Str("12345678"));
    scope.put("client.tmp.loanAmt", new Num(BigDecimal.int2bigDecimal(2000)));
    scope.put("client.tmp.loanTerm", new Str("6"));
    scope.put("client.tmp.incomeFlag", new Str("W"));
    scope.put("client.tmp.monthlyWages", new Num(BigDecimal.int2bigDecimal(0)));
    scope.put("client.tmp.putpaycardid", new Str("12345678901234567890123456789012"));
    scope.put("client.tmp.bankcardowner", new Str("VASILIY PUPKIN"));
    scope.put("client.tmp.bankcode", new Str("0987654321"));
    scope.put("client.tmp.bankname", new Str("MAIN BANK"));
    scope.put("client.tmp.bankprovince", new Str("BankProvince"));
    scope.put("client.tmp.bankprovincecode", new Str("BankProvCode"));
    scope.put("client.tmp.bankcity", new Str("BankCity"));
    scope.put("client.tmp.bankcitycode", new Str("BankCityCode"));
    scope.put("client.tmp.agreeRateExpireDate", new Dat(LocalDate.of(2016, 12, 31)));
    scope.put("client.tmp.jionLifeInsuInd", new Bool(true));
    scope.put("client.tmp.cooperatorID", new Str("0"));
    scope.put("client.tmp.purpose", new Str("PURP"));
    scope.put("client.tmp.agreeRateInd", new Bool(true));
    scope.put("client.tmp.feeRate", new Bool(true));
    scope.put("client.tmp.feeAmount", new Num(BigDecimal.int2bigDecimal(10)));
    scope.put("client.tmp.lifeInsuFeeRate", new Num(BigDecimal.double2bigDecimal(0.6)));
    scope.put("client.tmp.lifeInsuFeeAmt", new Num(BigDecimal.int2bigDecimal(12)));
    scope.put("client.tmp.insRate", new Num(BigDecimal.double2bigDecimal(0.5)));
    scope.put("client.tmp.insAmt", new Num(BigDecimal.int2bigDecimal(110)));
    scope.put("client.tmp.installmentFeeRate", new Num(BigDecimal.double2bigDecimal(0.2)));
    scope.put("client.tmp.installmentFeeAmt", new Num(BigDecimal.int2bigDecimal(90)));
    scope.put("client.tmp.prepaymentFeeRate", new Num(BigDecimal.double2bigDecimal(0.4)));
    scope.put("client.tmp.prepaymentFeeAmt", new Num(BigDecimal.int2bigDecimal(80)));
    scope.put("client.tmp.penaltyRate", new Num(BigDecimal.double2bigDecimal(0.4)));
    scope.put("client.tmp.compoundRate", new Num(BigDecimal.double2bigDecimal(0.4)));
    scope.put("client.tmp.interestRate", new Num(BigDecimal.double2bigDecimal(0.4)));
    scope.put("client.tmp.agentFeeRate", new Num(BigDecimal.double2bigDecimal(0.4)));
    scope.put("client.tmp.agentFeeAmount", new Num(BigDecimal.double2bigDecimal(40)));

    scope.put("client.tmp.org", new Str("MSXF"));
    scope.put("client.tmp.opId", new Str("" + (int) (Math.random() * 1000)));
    scope.put("client.tmp.inputSource", new Str("MC"));
    scope.put("client.tmp.bizDate", new Dat(LocalDate.now()));
    scope.put("client.tmp.requestTime", new Dat(LocalDate.now()));
    scope.put("client.tmp.serviceSn", new Str("" + (int) (Math.random() * 10000000))); //

    scope.put("client.tmp.serviceId", new Str("" + (int) (Math.random() * 1000)));
    scope.put("client.tmp.subTerminalType", new Str("" + (int) (Math.random() * 1000)));
    scope.put("client.tmp.acqId", new Str("" + (int) (Math.random() * 1000)));

    //CCS_ACCT_NBR_GEN
    // scope.put("client.tmp.acctNbr", new Num(BigDecimal.double2bigDecimal((int) (Math.random() * 1000000000))));


  }

  private String genRefnbr(LocalDate batchDate, String servicesn) {
    String nowdate = null;
    String tmpdate = null;
    tmpdate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmssSSS"));
    if (null != batchDate) {
      nowdate = batchDate.format(DateTimeFormatter.ofPattern("yyMMdd")) + tmpdate.substring(tmpdate.length() - 9);
    } else {
      nowdate = tmpdate;
    }
    int randonstr = (int) (8999 * Math.random()) + 1000;
    if (StringUtils.isNotBlank(servicesn)) {
      return nowdate + String.valueOf(randonstr) + servicesn.substring(servicesn.length() - 3);
    } else {
      randonstr = (int) (8999999 * Math.random()) + 1000000;
      return nowdate + String.valueOf(randonstr);
    }
  }


  private String genContrNbr(String acctNbr, LocalDate bizDate, String loanCode) {
    String seq = acctNbr;
    StringBuilder strBuf = new StringBuilder(loanCode);
    strBuf.append(bizDate.format(DateTimeFormatter.ofPattern("yyMMdd")));
    if (seq.length() <= 8) {
      System.out.println(acctNbr);
      System.out.println(String.format("%08d", acctNbr));
      strBuf.append(String.format("%08d", acctNbr));
    } else {
      strBuf.append(seq.substring(seq.length() - 8, seq.length()));
    }

    return strBuf.toString();
  }


}
