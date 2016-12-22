package kz.greetgo.dt.dt_manager_compare_performance_test;

import kz.greetgo.dt.*;
import kz.greetgo.dt.gen.AstObj;
import kz.greetgo.util.ServerUtil;
import scala.math.BigDecimal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static kz.greetgo.dt.DtManager3Test.*;

public class DtManagerComparePerformanceProcedures {

  private static Num num_(long value) {
    return Num.apply(BigDecimal.decimal(value));
  }

  private static Dat dat_(String yyyMMdd) {
    final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    return Dat.apply(LocalDate.parse(yyyMMdd, dtf));
  }

  private static Num num_(double value) {
    return Num.apply(BigDecimal.decimal(value));
  }

  private static Bool bool_(boolean value) {
    return Bool.apply(value);
  }

  private static Str str_(String value) {
    return Str.apply(value);
  }

  public SortedMap<String, DtType> newScope() {
    SortedMap<String, DtType> scope = new TreeMap<>();
    scope.put("in.accountIndex", num_(1));
    setAccount(scope, 0);
    setAccount(scope, 1);

    return trimKeys(scope);
  }

  private static SortedMap<String, DtType> trimKeys(SortedMap<String, DtType> scope) {
    SortedMap<String, DtType> ret = new TreeMap<>();
    scope.entrySet().forEach(e -> {
      String trimmedKey = e.getKey().trim();
      if (trimmedKey.length() > 0) ret.put(trimmedKey, e.getValue());
    });
    return ret;
  }

  private void setAccount(Map<String, DtType> scope, int i) {
    scope.put("client.account." + i + ".product.advanceRepaymentApply.applyBefore      ", num_(12));
    scope.put("client.account." + i + ".product.advanceRepaymentApply.canToApply       ", bool_(true));
    scope.put("client.account." + i + ".product.advanceRepaymentApply.deductBefore     ", num_(3));
    scope.put("client.account." + i + ".product.AdvanceRepaymentServiceFee.0.parameter ", num_(3.25));

//    scope.put("client.account." + i + ".product.ChargeAdvanceRepayment.Interest", str_("Remaining"));
//    scope.put("client.account." + i + ".product.ChargeAdvanceRepayment.Interest", str_("CurrentTerm"));
    scope.put("client.account." + i + ".product.ChargeAdvanceRepayment.Interest", str_("SettlementDate"));

//    scope.put("client.account." + i + ".product.ChargeAdvanceRepayment.Principal", str_("Remaining"));
//    scope.put("client.account." + i + ".product.ChargeAdvanceRepayment.Principal", str_("CurrentTerm"));
    scope.put("client.account." + i + ".product.ChargeAdvanceRepayment.Principal", str_("SettlementDate"));

//    scope.put("client.account." + i + ".product.ChargeAdvanceRepayment.ServiceFee", str_("Remaining"));
//    scope.put("client.account." + i + ".product.ChargeAdvanceRepayment.ServiceFee", str_("CurrentTerm"));
    scope.put("client.account." + i + ".product.ChargeAdvanceRepayment.ServiceFee", str_("SettlementDate"));

//    scope.put("client.account." + i + ".product.ChargeAdvanceRepayment.Value_addedServiceFee", str_("Remaining"));
//    scope.put("client.account." + i + ".product.ChargeAdvanceRepayment.Value_addedServiceFee", str_("CurrentTerm"));
    scope.put("client.account." + i + ".product.ChargeAdvanceRepayment.Value_addedServiceFee", str_("SettlementDate"));

    scope.put("client.account." + i + ".hasAdvancedClearing ", bool_(false));
    scope.put("client.account." + i + ".sumOverpaid         ", num_(23.4));
    scope.put("client.account." + i + ".currentTerm         ", num_(2));
    scope.put("client.account." + i + ".totalRemServiceFee  ", num_(100.01));
    scope.put("client.account." + i + ".nextStmtDate        ", dat_("2011-01-01"));
    scope.put("client.account." + i + ".createTime          ", dat_("2011-01-01"));

    scope.put("client.account." + i + ".totalRemLifeInsuranceFee  ", num_(1.01));
    scope.put("client.account." + i + ".totalRemInstallmentFee    ", num_(1.02));
    scope.put("client.account." + i + ".totalRemPrepayPkgFee      ", num_(1.03));
    scope.put("client.account." + i + ".totalRemReplaceServiceFee ", num_(1.04));
    scope.put("client.account." + i + ".totalRemAgentFee          ", num_(1.05));

    scope.put("client.account." + i + ".payPlan.1.remainingInterest   ", num_(120.21));
    scope.put("client.account." + i + ".payPlan.1.remainingPrincipal  ", num_(12.21));
    scope.put("client.account." + i + ".payPlan.1.remainingServiceFee ", num_(20.21));

    scope.put("client.account." + i + ".payPlan.1.remainingLifeInsurance  ", num_(120.21));
    scope.put("client.account." + i + ".payPlan.1.remainingInstallmentFee ", num_(12.21));
    scope.put("client.account." + i + ".payPlan.1.remainingPrepayPkgFee   ", num_(20.21));
    scope.put("client.account." + i + ".payPlan.1.remainingReplaceSvcFee  ", num_(20.21));
    scope.put("client.account." + i + ".payPlan.1.remainingAgentFee       ", num_(20.21));
  }


  public void registerProcedures(DtManagerBuilder builder) throws Exception {
    builder.registerTree("main", "group(\n" +
        "  assign(date, businessDay()),\n" +
        "  assign(enableApply, false()),\n" +
        "  assign(status, \"canNotApply\"),\n" +
        "  assign(nextStmtDate, businessDay()),\n" +
        "  assign(txnAmt, 1),\n" +
        "  assign(feeAmt, 1),\n" +
        "  procedure(advanceRepayment)\n" +
        ")");
    registerUserProcedure(builder, "advanceRepayment");
    registerUserProcedure(builder, "calcAdvanceInterestAmt");
    registerUserProcedure(builder, "calcAdvancePrincipalAmt");
    registerUserProcedure(builder, "calcAdvanceRepaymentTxnFee");
    registerUserProcedure(builder, "calcAdvanceServiceFeeAmt");
    registerUserProcedure(builder, "calculateDebtTillSettlement");
    registerUserProcedure(builder, "calcValue_addedServiceFee");
  }

  private void registerUserProcedure(DtManagerBuilder builder, String name) throws Exception {
    builder.registerUserProcedure(
        name,
        ServerUtil.streamToStr0(
            getClass().getResourceAsStream("txt/" + name + ".txt")
        )
    );
  }
}
