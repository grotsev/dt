package kz.greetgo.dt.RepaymentPlanGeneration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ilyas on 20.07.2016.
 */
public class RepaymentPlanGenerationResponce {
    public Integer loanTerm = 0;
    public BigDecimal loanInitPrin = BigDecimal.ZERO;
    public BigDecimal loanIntSum = BigDecimal.ZERO;
    public BigDecimal loanFeeSum = BigDecimal.ZERO;

    public List<RepaymentPlanSchedule> subScheduleList = new ArrayList<>();


}
