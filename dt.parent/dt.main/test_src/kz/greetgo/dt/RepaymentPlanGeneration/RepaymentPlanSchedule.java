package kz.greetgo.dt.RepaymentPlanGeneration;

import java.math.BigDecimal;

/**
 * Created by ilyas on 20.07.2016.
 */
public class RepaymentPlanSchedule {
    public Integer loanCurrTerm = 0;
    public BigDecimal loanTermTotAmt = BigDecimal.ZERO;
    public BigDecimal loanTermPrin = BigDecimal.ZERO;
    public BigDecimal loanTermInstallFee = BigDecimal.ZERO;
    public BigDecimal loanTermSvcFee = BigDecimal.ZERO;
    public BigDecimal loanTermInt = BigDecimal.ZERO;
    public BigDecimal loanLifeInsuFee = BigDecimal.ZERO;
    public BigDecimal loanPrepayPkgFee = BigDecimal.ZERO;
    public BigDecimal loanInsuranceFee = BigDecimal.ZERO;
    public BigDecimal loanAgentFee = BigDecimal.ZERO;
    public String loanPmtDueDate = "";
    public BigDecimal loanStampDutyAmt = BigDecimal.ZERO;
    ;
}
