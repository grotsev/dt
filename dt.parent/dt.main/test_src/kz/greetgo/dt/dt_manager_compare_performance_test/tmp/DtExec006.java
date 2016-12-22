package kz.greetgo.dt.dt_manager_compare_performance_test.tmp;

import kz.greetgo.dt.*;
import kz.greetgo.dt.gen.CallInstance;
import kz.greetgo.dt.gen.DtRunnable;
import kz.greetgo.dt.gen.U;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DtExec006 implements DtRunnable {

  private final ArrayList<String> messages = new ArrayList<>();
  private Set<Integer> breakpoints;
  private BitSet reached;
  private ArrayList<CallInstance> callStack = new ArrayList<>();

  @Override
  public List<String> messages() {
    return messages;
  }

  @Override
  public Set<Integer> reached() {
    return reached == null ? null : reached.stream().boxed().collect(Collectors.toSet());
  }

  private Map<String, Consumer<ScopeAccess>> nativeProcedures = new HashMap<>();

  private void callNativeProcedure(String name) {
    Consumer<ScopeAccess> nativeProcedure = nativeProcedures.get(name);
    if (nativeProcedure != null) nativeProcedure.accept(this);
    else throw new ErrorException("No procedure " + name);
  }

  @Override
  public void registerNativeProcedure(String name, Consumer<ScopeAccess> body) {
    nativeProcedures.put(name, body);
  }

  @Override
  public void setNativeProcedures(Map<String, Consumer<ScopeAccess>> nativeProcedures) {
    this.nativeProcedures = nativeProcedures;
  }

  @Override
  public void setNativeFunctions(NativeFunctions nativeFunctions) {
    this.nativeFunctions = nativeFunctions;
  }

  @Override
  public DtType get(String path) {
    return scope.get(Arrays.asList(path.split("\\.")));
  }

  @Override
  public void set(String path, DtType value) {
    scope.set(path.split("\\."), 0, value);
  }

  @Override
  public Map<String, DtType> scope() {
    Map<String, DtType> s = new HashMap<>();
    scope.fill(s, "");
    return s;
  }

  @Override
  public Set<String> allPaths() {
    return scope().keySet();
  }

  private static class StopException extends RuntimeException {
  }

  private static void stop() {
    throw new StopException();
  }

  private static class ExecMetrics {
    long prepareScope;
    long execute;
    long saveScope;

    long count;

    public void clean() {
      prepareScope = execute = saveScope = count = 0;
    }
  }

  private final ExecMetrics execMetrics = new ExecMetrics();

  @Override
  public void cleanExecMetrics() {
    execMetrics.clean();
  }

  @Override
  public Map<String, Long> getExecMetrics() {
    Map<String, Long> ret = new HashMap<>();
    ret.put("prepareScope", execMetrics.prepareScope);
    ret.put("execute", execMetrics.execute);
    ret.put("saveScope", execMetrics.saveScope);
    ret.put("count", execMetrics.count);
    return ret;
  }

  @Override
  public Map<String, DtType> exec(String treeName, Map<String, DtType> extScope, Set<Integer> breakpoints) {
    long time1 = 0, time2 = 0, time3 = 0, time4 = 0;

    this.breakpoints = breakpoints;
    reached = breakpoints == null ? null : new BitSet(8192);

    Map<String, DtType> resultScope = new HashMap<>();
    try {
      time1 = System.nanoTime();

      if (extScope instanceof SortedMap) {
        extScope.entrySet().forEach(e -> scope.set(e.getKey().split("\\."), 0, e.getValue()));
      } else {
        extScope.entrySet().stream()
            .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
            .forEach(e -> scope.set(e.getKey().split("\\."), 0, e.getValue()));
        throw new RuntimeException();
      }

      time2 = System.nanoTime();
      switch (treeName) {
        case "calcAdvanceRepaymentTxnFee":
          calcAdvanceRepaymentTxnFee();
          break;
        case "calcAdvanceServiceFeeAmt":
          calcAdvanceServiceFeeAmt();
          break;
        case "advanceRepayment":
          advanceRepayment();
          break;
        case "calcAdvancePrincipalAmt":
          calcAdvancePrincipalAmt();
          break;
        case "calcValue_addedServiceFee":
          calcValue_addedServiceFee();
          break;
        case "calcAdvanceInterestAmt":
          calcAdvanceInterestAmt();
          break;
        case "calculateDebtTillSettlement":
          calculateDebtTillSettlement();
          break;
        case "main":
          main();
          break;
        default:
          throw new IllegalArgumentException("No tree with name " + treeName);
      }
    } catch (BreakPointException e) {
    } catch (StopException e) {
    } catch (ErrorException e) {
      throw new ErrorException(callStack.toString() + " " + e.getMessage());
    } catch (Exception e) {
      throw new ExprException(e, callStack.toString() + " " + e.getMessage());
    }
    time3 = System.nanoTime();
    scope.fill(resultScope, "");
    time4 = System.nanoTime();
    execMetrics.prepareScope += time2 - time1;
    execMetrics.execute += time3 - time2;
    execMetrics.saveScope += time4 - time3;
    execMetrics.count++;

    return resultScope;
  }

  public static final class Scope {
    public LocalDate date;
    public BigDecimal amount;

    public static final class in {
      public BigDecimal accountIndex;

      public DtType get(List<String> path) {
        if (path.isEmpty()) throw new ErrorException("get from empty path");
        switch (path.get(0)) {
          case "accountIndex":
            return this.accountIndex == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.accountIndex));
          default:
            throw new ErrorException("(Get) Undefined field " + path.get(0));
        }
      }

      public final void set(String[] path, int o, DtType value) {
        if (path.length <= o) throw new ErrorException("set to empty path");
        switch (path[o]) {
          case "accountIndex":
            this.accountIndex = ((Num) value).num().underlying();
            break;
          default:
            throw new ErrorException("(Set) Undefined field " + path[o]);
        }
      }

      public void fill(Map<String, DtType> scope, String prefix) {
        if (this.accountIndex != null)
          scope.put((prefix + ".accountIndex").substring(1), new Num(new scala.math.BigDecimal(this.accountIndex)));
      }
    }

    public final in in = new in();
    public Boolean enableApply;
    public BigDecimal termNumber;
    public LocalDate appliedDate;
    public BigDecimal feeAmt;
    public BigDecimal dayNumber;
    public BigDecimal _applyBefore;

    public static final class client {
      public static final class account {
        public static final class product {
          public static final class advanceRepaymentApply {
            public BigDecimal applyBefore;
            public Boolean canToApply;
            public BigDecimal deductBefore;

            public DtType get(List<String> path) {
              if (path.isEmpty()) throw new ErrorException("get from empty path");
              switch (path.get(0)) {
                case "applyBefore":
                  return this.applyBefore == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.applyBefore));
                case "canToApply":
                  return this.canToApply == null ? null : new Bool(this.canToApply);
                case "deductBefore":
                  return this.deductBefore == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.deductBefore));
                default:
                  throw new ErrorException("(Get) Undefined field " + path.get(0));
              }
            }

            public final void set(String[] path, int o, DtType value) {
              if (path.length <= o) throw new ErrorException("set to empty path");
              switch (path[o]) {
                case "applyBefore":
                  this.applyBefore = ((Num) value).num().underlying();
                  break;
                case "canToApply":
                  this.canToApply = ((Bool) value).bool();
                  break;
                case "deductBefore":
                  this.deductBefore = ((Num) value).num().underlying();
                  break;
                default:
                  throw new ErrorException("(Set) Undefined field " + path[o]);
              }
            }

            public void fill(Map<String, DtType> scope, String prefix) {
              if (this.applyBefore != null)
                scope.put((prefix + ".applyBefore").substring(1), new Num(new scala.math.BigDecimal(this.applyBefore)));
              if (this.canToApply != null) scope.put((prefix + ".canToApply").substring(1), new Bool(this.canToApply));
              if (this.deductBefore != null)
                scope.put((prefix + ".deductBefore").substring(1), new Num(new scala.math.BigDecimal(this.deductBefore)));
            }
          }

          public final advanceRepaymentApply advanceRepaymentApply = new advanceRepaymentApply();

          public static final class ChargeAdvanceRepayment {
            public String Interest;
            public String ServiceFee;
            public String Principal;
            public String Value_addedServiceFee;

            public DtType get(List<String> path) {
              if (path.isEmpty()) throw new ErrorException("get from empty path");
              switch (path.get(0)) {
                case "Interest":
                  return this.Interest == null ? null : new Str(this.Interest);
                case "ServiceFee":
                  return this.ServiceFee == null ? null : new Str(this.ServiceFee);
                case "Principal":
                  return this.Principal == null ? null : new Str(this.Principal);
                case "Value_addedServiceFee":
                  return this.Value_addedServiceFee == null ? null : new Str(this.Value_addedServiceFee);
                default:
                  throw new ErrorException("(Get) Undefined field " + path.get(0));
              }
            }

            public final void set(String[] path, int o, DtType value) {
              if (path.length <= o) throw new ErrorException("set to empty path");
              switch (path[o]) {
                case "Interest":
                  this.Interest = ((Str) value).str();
                  break;
                case "ServiceFee":
                  this.ServiceFee = ((Str) value).str();
                  break;
                case "Principal":
                  this.Principal = ((Str) value).str();
                  break;
                case "Value_addedServiceFee":
                  this.Value_addedServiceFee = ((Str) value).str();
                  break;
                default:
                  throw new ErrorException("(Set) Undefined field " + path[o]);
              }
            }

            public void fill(Map<String, DtType> scope, String prefix) {
              if (this.Interest != null) scope.put((prefix + ".Interest").substring(1), new Str(this.Interest));
              if (this.ServiceFee != null) scope.put((prefix + ".ServiceFee").substring(1), new Str(this.ServiceFee));
              if (this.Principal != null) scope.put((prefix + ".Principal").substring(1), new Str(this.Principal));
              if (this.Value_addedServiceFee != null)
                scope.put((prefix + ".Value_addedServiceFee").substring(1), new Str(this.Value_addedServiceFee));
            }
          }

          public final ChargeAdvanceRepayment ChargeAdvanceRepayment = new ChargeAdvanceRepayment();

          public static final class AdvanceRepaymentServiceFee {
            public BigDecimal parameter;

            public DtType get(List<String> path) {
              if (path.isEmpty()) throw new ErrorException("get from empty path");
              switch (path.get(0)) {
                case "parameter":
                  return this.parameter == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.parameter));
                default:
                  throw new ErrorException("(Get) Undefined field " + path.get(0));
              }
            }

            public final void set(String[] path, int o, DtType value) {
              if (path.length <= o) throw new ErrorException("set to empty path");
              switch (path[o]) {
                case "parameter":
                  this.parameter = ((Num) value).num().underlying();
                  break;
                default:
                  throw new ErrorException("(Set) Undefined field " + path[o]);
              }
            }

            public void fill(Map<String, DtType> scope, String prefix) {
              if (this.parameter != null)
                scope.put((prefix + ".parameter").substring(1), new Num(new scala.math.BigDecimal(this.parameter)));
            }
          }

          public final ArrayList<AdvanceRepaymentServiceFee> AdvanceRepaymentServiceFee = new ArrayList<AdvanceRepaymentServiceFee>();

          public final AdvanceRepaymentServiceFee AdvanceRepaymentServiceFee(boolean save, BigDecimal num) {
            if (num != null) {
              int n = num.intValue();
              int s = this.AdvanceRepaymentServiceFee.size();
              if (-s <= n && n < s) return this.AdvanceRepaymentServiceFee.get(n >= 0 ? n : n + s);
            }
            AdvanceRepaymentServiceFee x = new AdvanceRepaymentServiceFee();
            if (save && num != null) {
              int n = num.intValue();
              if (n >= 0) {
                U.ensureSize(this.AdvanceRepaymentServiceFee, n + 1);
                this.AdvanceRepaymentServiceFee.set(n, x);
              } else throw new ArrayIndexOutOfBoundsException(n);
            }
            return x;
          }

          public DtType get(List<String> path) {
            if (path.isEmpty()) throw new ErrorException("get from empty path");
            switch (path.get(0)) {
              case "advanceRepaymentApply":
                return this.advanceRepaymentApply.get(path.subList(1, path.size()));
              case "ChargeAdvanceRepayment":
                return this.ChargeAdvanceRepayment.get(path.subList(1, path.size()));
              case "AdvanceRepaymentServiceFee": {
                int index = Integer.parseInt(path.get(1));
                if (index < 0 || index >= this.AdvanceRepaymentServiceFee.size() || this.AdvanceRepaymentServiceFee.get(index) == null)
                  return null;
                return this.AdvanceRepaymentServiceFee.get(index).get(path.subList(2, path.size()));
              }
              default:
                throw new ErrorException("(Get) Undefined field " + path.get(0));
            }
          }

          public final void set(String[] path, int o, DtType value) {
            if (path.length <= o) throw new ErrorException("set to empty path");
            switch (path[o]) {
              case "advanceRepaymentApply":
                this.advanceRepaymentApply.set(path, o + 1, value);
                break;
              case "ChargeAdvanceRepayment":
                this.ChargeAdvanceRepayment.set(path, o + 1, value);
                break;
              case "AdvanceRepaymentServiceFee": {
                int index = Integer.parseInt(path[o + 1]);
                U.ensureSize(this.AdvanceRepaymentServiceFee, index + 1);
                if (this.AdvanceRepaymentServiceFee.get(index) == null)
                  this.AdvanceRepaymentServiceFee.set(index, new AdvanceRepaymentServiceFee());
                this.AdvanceRepaymentServiceFee.get(index).set(path, o + 2, value);
                break;
              }
              default:
                throw new ErrorException("(Set) Undefined field " + path[o]);
            }
          }

          public void fill(Map<String, DtType> scope, String prefix) {
            if (this.advanceRepaymentApply != null)
              this.advanceRepaymentApply.fill(scope, prefix + ".advanceRepaymentApply");
            if (this.ChargeAdvanceRepayment != null)
              this.ChargeAdvanceRepayment.fill(scope, prefix + ".ChargeAdvanceRepayment");
            for (int i = 0; i < this.AdvanceRepaymentServiceFee.size(); i++)
              if (this.AdvanceRepaymentServiceFee.get(i) != null)
                this.AdvanceRepaymentServiceFee.get(i).fill(scope, prefix + ".AdvanceRepaymentServiceFee" + "." + i);
          }
        }

        public final product product = new product();
        public BigDecimal totalRemPrincipalAmt;
        public BigDecimal totalRemServiceFee;
        public LocalDate batchDate;
        public BigDecimal totalRemInterest;
        public BigDecimal sumOverpaid;
        public BigDecimal totalRemReplaceServiceFee;
        public Boolean hasAdvancedClearing;
        public BigDecimal totalRemAgentFee;
        public BigDecimal totalRemInstallmentFee;
        public BigDecimal advanceRepaymentAmount;
        public LocalDate appliedAdvanceRepaymentDate;
        public BigDecimal currentTerm;
        public LocalDate createTime;
        public BigDecimal totalRemLifeInsuranceFee;
        public String id;
        public BigDecimal totalRemPrepayPkgFee;

        public static final class payPlan {
          public BigDecimal remainingPrepayPkgFee;
          public BigDecimal remainingReplaceSvcFee;
          public BigDecimal remainingPrincipal;
          public BigDecimal remainingInterest;
          public BigDecimal remainingLifeInsurance;
          public BigDecimal remainingServiceFee;
          public BigDecimal remainingInstallmentFee;
          public BigDecimal remainingAgentFee;

          public DtType get(List<String> path) {
            if (path.isEmpty()) throw new ErrorException("get from empty path");
            switch (path.get(0)) {
              case "remainingPrepayPkgFee":
                return this.remainingPrepayPkgFee == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.remainingPrepayPkgFee));
              case "remainingReplaceSvcFee":
                return this.remainingReplaceSvcFee == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.remainingReplaceSvcFee));
              case "remainingPrincipal":
                return this.remainingPrincipal == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.remainingPrincipal));
              case "remainingInterest":
                return this.remainingInterest == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.remainingInterest));
              case "remainingLifeInsurance":
                return this.remainingLifeInsurance == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.remainingLifeInsurance));
              case "remainingServiceFee":
                return this.remainingServiceFee == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.remainingServiceFee));
              case "remainingInstallmentFee":
                return this.remainingInstallmentFee == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.remainingInstallmentFee));
              case "remainingAgentFee":
                return this.remainingAgentFee == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.remainingAgentFee));
              default:
                throw new ErrorException("(Get) Undefined field " + path.get(0));
            }
          }

          public final void set(String[] path, int o, DtType value) {
            if (path.length <= o) throw new ErrorException("set to empty path");
            switch (path[o]) {
              case "remainingPrepayPkgFee":
                this.remainingPrepayPkgFee = ((Num) value).num().underlying();
                break;
              case "remainingReplaceSvcFee":
                this.remainingReplaceSvcFee = ((Num) value).num().underlying();
                break;
              case "remainingPrincipal":
                this.remainingPrincipal = ((Num) value).num().underlying();
                break;
              case "remainingInterest":
                this.remainingInterest = ((Num) value).num().underlying();
                break;
              case "remainingLifeInsurance":
                this.remainingLifeInsurance = ((Num) value).num().underlying();
                break;
              case "remainingServiceFee":
                this.remainingServiceFee = ((Num) value).num().underlying();
                break;
              case "remainingInstallmentFee":
                this.remainingInstallmentFee = ((Num) value).num().underlying();
                break;
              case "remainingAgentFee":
                this.remainingAgentFee = ((Num) value).num().underlying();
                break;
              default:
                throw new ErrorException("(Set) Undefined field " + path[o]);
            }
          }

          public void fill(Map<String, DtType> scope, String prefix) {
            if (this.remainingPrepayPkgFee != null)
              scope.put((prefix + ".remainingPrepayPkgFee").substring(1), new Num(new scala.math.BigDecimal(this.remainingPrepayPkgFee)));
            if (this.remainingReplaceSvcFee != null)
              scope.put((prefix + ".remainingReplaceSvcFee").substring(1), new Num(new scala.math.BigDecimal(this.remainingReplaceSvcFee)));
            if (this.remainingPrincipal != null)
              scope.put((prefix + ".remainingPrincipal").substring(1), new Num(new scala.math.BigDecimal(this.remainingPrincipal)));
            if (this.remainingInterest != null)
              scope.put((prefix + ".remainingInterest").substring(1), new Num(new scala.math.BigDecimal(this.remainingInterest)));
            if (this.remainingLifeInsurance != null)
              scope.put((prefix + ".remainingLifeInsurance").substring(1), new Num(new scala.math.BigDecimal(this.remainingLifeInsurance)));
            if (this.remainingServiceFee != null)
              scope.put((prefix + ".remainingServiceFee").substring(1), new Num(new scala.math.BigDecimal(this.remainingServiceFee)));
            if (this.remainingInstallmentFee != null)
              scope.put((prefix + ".remainingInstallmentFee").substring(1), new Num(new scala.math.BigDecimal(this.remainingInstallmentFee)));
            if (this.remainingAgentFee != null)
              scope.put((prefix + ".remainingAgentFee").substring(1), new Num(new scala.math.BigDecimal(this.remainingAgentFee)));
          }
        }

        public final ArrayList<payPlan> payPlan = new ArrayList<payPlan>();

        public final payPlan payPlan(boolean save, BigDecimal num) {
          if (num != null) {
            int n = num.intValue();
            int s = this.payPlan.size();
            if (-s <= n && n < s) return this.payPlan.get(n >= 0 ? n : n + s);
          }
          payPlan x = new payPlan();
          if (save && num != null) {
            int n = num.intValue();
            if (n >= 0) {
              U.ensureSize(this.payPlan, n + 1);
              this.payPlan.set(n, x);
            } else throw new ArrayIndexOutOfBoundsException(n);
          }
          return x;
        }

        public LocalDate nextStmtDate;

        public DtType get(List<String> path) {
          if (path.isEmpty()) throw new ErrorException("get from empty path");
          switch (path.get(0)) {
            case "product":
              return this.product.get(path.subList(1, path.size()));
            case "totalRemPrincipalAmt":
              return this.totalRemPrincipalAmt == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.totalRemPrincipalAmt));
            case "totalRemServiceFee":
              return this.totalRemServiceFee == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.totalRemServiceFee));
            case "batchDate":
              return this.batchDate == null ? null : new Dat(this.batchDate);
            case "totalRemInterest":
              return this.totalRemInterest == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.totalRemInterest));
            case "sumOverpaid":
              return this.sumOverpaid == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.sumOverpaid));
            case "totalRemReplaceServiceFee":
              return this.totalRemReplaceServiceFee == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.totalRemReplaceServiceFee));
            case "hasAdvancedClearing":
              return this.hasAdvancedClearing == null ? null : new Bool(this.hasAdvancedClearing);
            case "totalRemAgentFee":
              return this.totalRemAgentFee == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.totalRemAgentFee));
            case "totalRemInstallmentFee":
              return this.totalRemInstallmentFee == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.totalRemInstallmentFee));
            case "advanceRepaymentAmount":
              return this.advanceRepaymentAmount == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.advanceRepaymentAmount));
            case "appliedAdvanceRepaymentDate":
              return this.appliedAdvanceRepaymentDate == null ? null : new Dat(this.appliedAdvanceRepaymentDate);
            case "currentTerm":
              return this.currentTerm == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.currentTerm));
            case "createTime":
              return this.createTime == null ? null : new Dat(this.createTime);
            case "totalRemLifeInsuranceFee":
              return this.totalRemLifeInsuranceFee == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.totalRemLifeInsuranceFee));
            case "id":
              return this.id == null ? null : new Str(this.id);
            case "totalRemPrepayPkgFee":
              return this.totalRemPrepayPkgFee == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.totalRemPrepayPkgFee));
            case "payPlan": {
              int index = Integer.parseInt(path.get(1));
              if (index < 0 || index >= this.payPlan.size() || this.payPlan.get(index) == null) return null;
              return this.payPlan.get(index).get(path.subList(2, path.size()));
            }
            case "nextStmtDate":
              return this.nextStmtDate == null ? null : new Dat(this.nextStmtDate);
            default:
              throw new ErrorException("(Get) Undefined field " + path.get(0));
          }
        }

        public final void set(String[] path, int o, DtType value) {
          if (path.length <= o) throw new ErrorException("set to empty path");
          switch (path[o]) {
            case "product":
              this.product.set(path, o + 1, value);
              break;
            case "totalRemPrincipalAmt":
              this.totalRemPrincipalAmt = ((Num) value).num().underlying();
              break;
            case "totalRemServiceFee":
              this.totalRemServiceFee = ((Num) value).num().underlying();
              break;
            case "batchDate":
              this.batchDate = ((Dat) value).dat();
              break;
            case "totalRemInterest":
              this.totalRemInterest = ((Num) value).num().underlying();
              break;
            case "sumOverpaid":
              this.sumOverpaid = ((Num) value).num().underlying();
              break;
            case "totalRemReplaceServiceFee":
              this.totalRemReplaceServiceFee = ((Num) value).num().underlying();
              break;
            case "hasAdvancedClearing":
              this.hasAdvancedClearing = ((Bool) value).bool();
              break;
            case "totalRemAgentFee":
              this.totalRemAgentFee = ((Num) value).num().underlying();
              break;
            case "totalRemInstallmentFee":
              this.totalRemInstallmentFee = ((Num) value).num().underlying();
              break;
            case "advanceRepaymentAmount":
              this.advanceRepaymentAmount = ((Num) value).num().underlying();
              break;
            case "appliedAdvanceRepaymentDate":
              this.appliedAdvanceRepaymentDate = ((Dat) value).dat();
              break;
            case "currentTerm":
              this.currentTerm = ((Num) value).num().underlying();
              break;
            case "createTime":
              this.createTime = ((Dat) value).dat();
              break;
            case "totalRemLifeInsuranceFee":
              this.totalRemLifeInsuranceFee = ((Num) value).num().underlying();
              break;
            case "id":
              this.id = ((Str) value).str();
              break;
            case "totalRemPrepayPkgFee":
              this.totalRemPrepayPkgFee = ((Num) value).num().underlying();
              break;
            case "payPlan": {
              int index = Integer.parseInt(path[o + 1]);
              U.ensureSize(this.payPlan, index + 1);
              if (this.payPlan.get(index) == null) this.payPlan.set(index, new payPlan());
              this.payPlan.get(index).set(path, o + 2, value);
              break;
            }
            case "nextStmtDate":
              this.nextStmtDate = ((Dat) value).dat();
              break;
            default:
              throw new ErrorException("(Set) Undefined field " + path[o]);
          }
        }

        public void fill(Map<String, DtType> scope, String prefix) {
          if (this.product != null) this.product.fill(scope, prefix + ".product");
          if (this.totalRemPrincipalAmt != null)
            scope.put((prefix + ".totalRemPrincipalAmt").substring(1), new Num(new scala.math.BigDecimal(this.totalRemPrincipalAmt)));
          if (this.totalRemServiceFee != null)
            scope.put((prefix + ".totalRemServiceFee").substring(1), new Num(new scala.math.BigDecimal(this.totalRemServiceFee)));
          if (this.batchDate != null) scope.put((prefix + ".batchDate").substring(1), new Dat(this.batchDate));
          if (this.totalRemInterest != null)
            scope.put((prefix + ".totalRemInterest").substring(1), new Num(new scala.math.BigDecimal(this.totalRemInterest)));
          if (this.sumOverpaid != null)
            scope.put((prefix + ".sumOverpaid").substring(1), new Num(new scala.math.BigDecimal(this.sumOverpaid)));
          if (this.totalRemReplaceServiceFee != null)
            scope.put((prefix + ".totalRemReplaceServiceFee").substring(1), new Num(new scala.math.BigDecimal(this.totalRemReplaceServiceFee)));
          if (this.hasAdvancedClearing != null)
            scope.put((prefix + ".hasAdvancedClearing").substring(1), new Bool(this.hasAdvancedClearing));
          if (this.totalRemAgentFee != null)
            scope.put((prefix + ".totalRemAgentFee").substring(1), new Num(new scala.math.BigDecimal(this.totalRemAgentFee)));
          if (this.totalRemInstallmentFee != null)
            scope.put((prefix + ".totalRemInstallmentFee").substring(1), new Num(new scala.math.BigDecimal(this.totalRemInstallmentFee)));
          if (this.advanceRepaymentAmount != null)
            scope.put((prefix + ".advanceRepaymentAmount").substring(1), new Num(new scala.math.BigDecimal(this.advanceRepaymentAmount)));
          if (this.appliedAdvanceRepaymentDate != null)
            scope.put((prefix + ".appliedAdvanceRepaymentDate").substring(1), new Dat(this.appliedAdvanceRepaymentDate));
          if (this.currentTerm != null)
            scope.put((prefix + ".currentTerm").substring(1), new Num(new scala.math.BigDecimal(this.currentTerm)));
          if (this.createTime != null) scope.put((prefix + ".createTime").substring(1), new Dat(this.createTime));
          if (this.totalRemLifeInsuranceFee != null)
            scope.put((prefix + ".totalRemLifeInsuranceFee").substring(1), new Num(new scala.math.BigDecimal(this.totalRemLifeInsuranceFee)));
          if (this.id != null) scope.put((prefix + ".id").substring(1), new Str(this.id));
          if (this.totalRemPrepayPkgFee != null)
            scope.put((prefix + ".totalRemPrepayPkgFee").substring(1), new Num(new scala.math.BigDecimal(this.totalRemPrepayPkgFee)));
          for (int i = 0; i < this.payPlan.size(); i++)
            if (this.payPlan.get(i) != null) this.payPlan.get(i).fill(scope, prefix + ".payPlan" + "." + i);
          if (this.nextStmtDate != null) scope.put((prefix + ".nextStmtDate").substring(1), new Dat(this.nextStmtDate));
        }
      }

      public final ArrayList<account> account = new ArrayList<account>();

      public final account account(boolean save, BigDecimal num) {
        if (num != null) {
          int n = num.intValue();
          int s = this.account.size();
          if (-s <= n && n < s) return this.account.get(n >= 0 ? n : n + s);
        }
        account x = new account();
        if (save && num != null) throw new ErrorException("Can not create client.account");
        return x;
      }

      public DtType get(List<String> path) {
        if (path.isEmpty()) throw new ErrorException("get from empty path");
        switch (path.get(0)) {
          case "account": {
            int index = Integer.parseInt(path.get(1));
            if (index < 0 || index >= this.account.size() || this.account.get(index) == null) return null;
            return this.account.get(index).get(path.subList(2, path.size()));
          }
          default:
            throw new ErrorException("(Get) Undefined field " + path.get(0));
        }
      }

      public final void set(String[] path, int o, DtType value) {
        if (path.length <= o) throw new ErrorException("set to empty path");
        switch (path[o]) {
          case "account": {
            int index = Integer.parseInt(path[o + 1]);
            U.ensureSize(this.account, index + 1);
            if (this.account.get(index) == null) this.account.set(index, new account());
            this.account.get(index).set(path, o + 2, value);
            break;
          }
          default:
            throw new ErrorException("(Set) Undefined field " + path[o]);
        }
      }

      public void fill(Map<String, DtType> scope, String prefix) {
        for (int i = 0; i < this.account.size(); i++)
          if (this.account.get(i) != null) this.account.get(i).fill(scope, prefix + ".account" + "." + i);
      }
    }

    public final client client = new client();
    public BigDecimal _deductBefore;
    public BigDecimal numberOfDaysInMonth;
    public BigDecimal txnAmt;
    public String status;
    public LocalDate nextStmtDate;

    public DtType get(List<String> path) {
      if (path.isEmpty()) throw new ErrorException("get from empty path");
      switch (path.get(0)) {
        case "date":
          return this.date == null ? null : new Dat(this.date);
        case "amount":
          return this.amount == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.amount));
        case "in":
          return this.in.get(path.subList(1, path.size()));
        case "enableApply":
          return this.enableApply == null ? null : new Bool(this.enableApply);
        case "termNumber":
          return this.termNumber == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.termNumber));
        case "appliedDate":
          return this.appliedDate == null ? null : new Dat(this.appliedDate);
        case "feeAmt":
          return this.feeAmt == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.feeAmt));
        case "dayNumber":
          return this.dayNumber == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.dayNumber));
        case "_applyBefore":
          return this._applyBefore == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this._applyBefore));
        case "client":
          return this.client.get(path.subList(1, path.size()));
        case "_deductBefore":
          return this._deductBefore == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this._deductBefore));
        case "numberOfDaysInMonth":
          return this.numberOfDaysInMonth == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.numberOfDaysInMonth));
        case "txnAmt":
          return this.txnAmt == null ? null : new Num(scala.math.BigDecimal.javaBigDecimal2bigDecimal(this.txnAmt));
        case "status":
          return this.status == null ? null : new Str(this.status);
        case "nextStmtDate":
          return this.nextStmtDate == null ? null : new Dat(this.nextStmtDate);
        default:
          throw new ErrorException("(Get) Undefined field " + path.get(0));
      }
    }

    public final void set(String[] path, int o, DtType value) {
      if (path.length <= o) throw new ErrorException("set to empty path");
      switch (path[o]) {
        case "date":
          this.date = ((Dat) value).dat();
          break;
        case "amount":
          this.amount = ((Num) value).num().underlying();
          break;
        case "in":
          this.in.set(path, o + 1, value);
          break;
        case "enableApply":
          this.enableApply = ((Bool) value).bool();
          break;
        case "termNumber":
          this.termNumber = ((Num) value).num().underlying();
          break;
        case "appliedDate":
          this.appliedDate = ((Dat) value).dat();
          break;
        case "feeAmt":
          this.feeAmt = ((Num) value).num().underlying();
          break;
        case "dayNumber":
          this.dayNumber = ((Num) value).num().underlying();
          break;
        case "_applyBefore":
          this._applyBefore = ((Num) value).num().underlying();
          break;
        case "client":
          this.client.set(path, o + 1, value);
          break;
        case "_deductBefore":
          this._deductBefore = ((Num) value).num().underlying();
          break;
        case "numberOfDaysInMonth":
          this.numberOfDaysInMonth = ((Num) value).num().underlying();
          break;
        case "txnAmt":
          this.txnAmt = ((Num) value).num().underlying();
          break;
        case "status":
          this.status = ((Str) value).str();
          break;
        case "nextStmtDate":
          this.nextStmtDate = ((Dat) value).dat();
          break;
        default:
          throw new ErrorException("(Set) Undefined field " + path[o]);
      }
    }

    public void fill(Map<String, DtType> scope, String prefix) {
      if (this.date != null) scope.put((prefix + ".date").substring(1), new Dat(this.date));
      if (this.amount != null)
        scope.put((prefix + ".amount").substring(1), new Num(new scala.math.BigDecimal(this.amount)));
      if (this.in != null) this.in.fill(scope, prefix + ".in");
      if (this.enableApply != null) scope.put((prefix + ".enableApply").substring(1), new Bool(this.enableApply));
      if (this.termNumber != null)
        scope.put((prefix + ".termNumber").substring(1), new Num(new scala.math.BigDecimal(this.termNumber)));
      if (this.appliedDate != null) scope.put((prefix + ".appliedDate").substring(1), new Dat(this.appliedDate));
      if (this.feeAmt != null)
        scope.put((prefix + ".feeAmt").substring(1), new Num(new scala.math.BigDecimal(this.feeAmt)));
      if (this.dayNumber != null)
        scope.put((prefix + ".dayNumber").substring(1), new Num(new scala.math.BigDecimal(this.dayNumber)));
      if (this._applyBefore != null)
        scope.put((prefix + "._applyBefore").substring(1), new Num(new scala.math.BigDecimal(this._applyBefore)));
      if (this.client != null) this.client.fill(scope, prefix + ".client");
      if (this._deductBefore != null)
        scope.put((prefix + "._deductBefore").substring(1), new Num(new scala.math.BigDecimal(this._deductBefore)));
      if (this.numberOfDaysInMonth != null)
        scope.put((prefix + ".numberOfDaysInMonth").substring(1), new Num(new scala.math.BigDecimal(this.numberOfDaysInMonth)));
      if (this.txnAmt != null)
        scope.put((prefix + ".txnAmt").substring(1), new Num(new scala.math.BigDecimal(this.txnAmt)));
      if (this.status != null) scope.put((prefix + ".status").substring(1), new Str(this.status));
      if (this.nextStmtDate != null) scope.put((prefix + ".nextStmtDate").substring(1), new Dat(this.nextStmtDate));
    }
  }

  public Scope scope = new Scope();

  public NativeFunctions nativeFunctions;

  public void calculateDebtTillSettlement() {
    final CallInstance call = new CallInstance("calculateDebtTillSettlement");
    callStack.add(call);
    call.nodeId = 2;
    call.nodeLoc = "1:1";
    call.nodeSrc = "group ( 	assign(dayNumber, day(date)), a...";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    call.nodeId = 6;
    call.nodeLoc = "2:2";
    call.nodeSrc = "assign(dayNumber, day(date))";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    scope.dayNumber = U.day(scope.date);
    call.nodeId = 26;
    call.nodeLoc = "3:5";
    call.nodeSrc = "assign(numberOfDaysInMonth, daysInMonth(date))";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    scope.numberOfDaysInMonth = U.daysInMonth(scope.date);
    call.nodeId = 46;
    call.nodeLoc = "5:5";
    call.nodeSrc = "assign(txnAmt, txnAmt + feeAmt * dayNumb...";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    scope.txnAmt = U.plus(scope.txnAmt, U.div(U.mult(scope.feeAmt, scope.dayNumber), scope.numberOfDaysInMonth));

    callStack.remove(callStack.size() - 1);
  }

  public void calcAdvanceInterestAmt() {
    final CallInstance call = new CallInstance("calcAdvanceInterestAmt");
    callStack.add(call);
    call.nodeId = 2;
    call.nodeLoc = "2:1";
    call.nodeSrc = "group ( condition ( case(daysBetween(cli...";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    {
      call.nodeId = 6;
      call.nodeLoc = "3:5";
      call.nodeSrc = "condition ( case(daysBetween(client.acco...";
      if (breakpoints != null) {
        reached.set(call.nodeId);
        if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
      }
      call.nodeId = 10;
      call.nodeLoc = "4:9";
      call.nodeSrc = "case(daysBetween(client.account[in.accou...";
      if (breakpoints != null) {
        reached.set(call.nodeId);
        if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
      }
      if (U.cmp(U.daysBetween(scope.client.account(false, scope.in.accountIndex).createTime, nativeFunctions.businessDay()), U.num("15")) <= 0) {
        call.nodeId = 40;
        call.nodeLoc = "5:13";
        call.nodeSrc = "group()";
        if (breakpoints != null) {
          reached.set(call.nodeId);
          if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
        }
      } else {
        call.nodeId = 48;
        call.nodeLoc = "8:9";
        call.nodeSrc = "case (client.account[in.accountIndex].pr...";
        if (breakpoints != null) {
          reached.set(call.nodeId);
          if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
        }
        if (U.cmp(scope.client.account(false, scope.in.accountIndex).product.ChargeAdvanceRepayment.Interest, "Remaining") == 0) {
          call.nodeId = 68;
          call.nodeLoc = "9:13";
          call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).totalRemInterest);
        } else {
          call.nodeId = 96;
          call.nodeLoc = "11:9";
          call.nodeSrc = "case (client.account[in.accountIndex].pr...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          if (U.cmp(scope.client.account(false, scope.in.accountIndex).product.ChargeAdvanceRepayment.Interest, "CurrentTerm") == 0) {
            call.nodeId = 116;
            call.nodeLoc = "12:13";
            call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).payPlan(false, U.minus(scope.client.account(false, scope.in.accountIndex).currentTerm, U.num("1"))).remainingInterest);
          } else {
            call.nodeId = 160;
            call.nodeLoc = "14:9";
            call.nodeSrc = "case (client.account[in.accountIndex].pr...";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            if (U.cmp(scope.client.account(false, scope.in.accountIndex).product.ChargeAdvanceRepayment.Interest, "SettlementDate") == 0) {
              call.nodeId = 180;
              call.nodeLoc = "15:13";
              call.nodeSrc = "group( assign (feeAmt, client.account[in...";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              call.nodeId = 184;
              call.nodeLoc = "16:17";
              call.nodeSrc = "assign (feeAmt, client.account[in.accoun...";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              scope.feeAmt = scope.client.account(false, scope.in.accountIndex).payPlan(false, U.minus(scope.client.account(false, scope.in.accountIndex).currentTerm, U.num("1"))).remainingInterest;
              call.nodeId = 222;
              call.nodeLoc = "17:17";
              call.nodeSrc = "procedure (calculateDebtTillSettlement)";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              calculateDebtTillSettlement();
            }
          }
        }
      }
    }

    callStack.remove(callStack.size() - 1);
  }

  public void calcAdvancePrincipalAmt() {
    final CallInstance call = new CallInstance("calcAdvancePrincipalAmt");
    callStack.add(call);
    call.nodeId = 2;
    call.nodeLoc = "2:1";
    call.nodeSrc = "group ( 	condition ( case (client.accoun...";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    {
      call.nodeId = 6;
      call.nodeLoc = "3:2";
      call.nodeSrc = "condition ( case (client.account[in.acco...";
      if (breakpoints != null) {
        reached.set(call.nodeId);
        if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
      }
      call.nodeId = 10;
      call.nodeLoc = "4:9";
      call.nodeSrc = "case (client.account[in.accountIndex].pr...";
      if (breakpoints != null) {
        reached.set(call.nodeId);
        if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
      }
      if (U.cmp(scope.client.account(false, scope.in.accountIndex).product.ChargeAdvanceRepayment.Principal, "Remaining") == 0) {
        call.nodeId = 30;
        call.nodeLoc = "5:13";
        call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
        if (breakpoints != null) {
          reached.set(call.nodeId);
          if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
        }
        scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).totalRemPrincipalAmt);
      } else {
        call.nodeId = 58;
        call.nodeLoc = "7:9";
        call.nodeSrc = "case (client.account[in.accountIndex].pr...";
        if (breakpoints != null) {
          reached.set(call.nodeId);
          if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
        }
        if (U.cmp(scope.client.account(false, scope.in.accountIndex).product.ChargeAdvanceRepayment.Principal, "CurrentTerm") == 0) {
          call.nodeId = 78;
          call.nodeLoc = "8:14";
          call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).payPlan(false, U.minus(scope.client.account(false, scope.in.accountIndex).currentTerm, U.num("1"))).remainingPrincipal);
        } else {
          call.nodeId = 122;
          call.nodeLoc = "10:9";
          call.nodeSrc = "case (client.account[in.accountIndex].pr...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          if (U.cmp(scope.client.account(false, scope.in.accountIndex).product.ChargeAdvanceRepayment.Principal, "SettlementDate") == 0) {
            call.nodeId = 142;
            call.nodeLoc = "11:13";
            call.nodeSrc = "group ( assign (feeAmt, client.account[i...";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            call.nodeId = 146;
            call.nodeLoc = "12:17";
            call.nodeSrc = "assign (feeAmt, client.account[in.accoun...";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            scope.feeAmt = scope.client.account(false, scope.in.accountIndex).payPlan(false, U.minus(scope.client.account(false, scope.in.accountIndex).currentTerm, U.num("1"))).remainingPrincipal;
            call.nodeId = 184;
            call.nodeLoc = "13:17";
            call.nodeSrc = "procedure (calculateDebtTillSettlement)";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            calculateDebtTillSettlement();
          }
        }
      }
    }

    callStack.remove(callStack.size() - 1);
  }

  public void calcAdvanceServiceFeeAmt() {
    final CallInstance call = new CallInstance("calcAdvanceServiceFeeAmt");
    callStack.add(call);
    call.nodeId = 2;
    call.nodeLoc = "2:1";
    call.nodeSrc = "group ( condition ( case(daysBetween(cli...";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    {
      call.nodeId = 6;
      call.nodeLoc = "3:5";
      call.nodeSrc = "condition ( case(daysBetween(client.acco...";
      if (breakpoints != null) {
        reached.set(call.nodeId);
        if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
      }
      call.nodeId = 10;
      call.nodeLoc = "4:9";
      call.nodeSrc = "case(daysBetween(client.account[in.accou...";
      if (breakpoints != null) {
        reached.set(call.nodeId);
        if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
      }
      if (U.cmp(U.daysBetween(scope.client.account(false, scope.in.accountIndex).createTime, nativeFunctions.businessDay()), U.num("15")) <= 0) {
        call.nodeId = 40;
        call.nodeLoc = "5:13";
        call.nodeSrc = "group()";
        if (breakpoints != null) {
          reached.set(call.nodeId);
          if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
        }
      } else {
        call.nodeId = 48;
        call.nodeLoc = "8:9";
        call.nodeSrc = "case (client.account[in.accountIndex].pr...";
        if (breakpoints != null) {
          reached.set(call.nodeId);
          if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
        }
        if (U.cmp(scope.client.account(false, scope.in.accountIndex).product.ChargeAdvanceRepayment.ServiceFee, "Remaining") == 0) {
          call.nodeId = 68;
          call.nodeLoc = "9:13";
          call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).totalRemServiceFee);
        } else {
          call.nodeId = 96;
          call.nodeLoc = "11:9";
          call.nodeSrc = "case (client.account[in.accountIndex].pr...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          if (U.cmp(scope.client.account(false, scope.in.accountIndex).product.ChargeAdvanceRepayment.ServiceFee, "CurrentTerm") == 0) {
            call.nodeId = 116;
            call.nodeLoc = "12:14";
            call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).payPlan(false, U.minus(scope.client.account(false, scope.in.accountIndex).currentTerm, U.num("1"))).remainingServiceFee);
          } else {
            call.nodeId = 160;
            call.nodeLoc = "14:9";
            call.nodeSrc = "case (client.account[in.accountIndex].pr...";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            if (U.cmp(scope.client.account(false, scope.in.accountIndex).product.ChargeAdvanceRepayment.ServiceFee, "SettlementDate") == 0) {
              call.nodeId = 180;
              call.nodeLoc = "15:13";
              call.nodeSrc = "group ( assign (feeAmt, client.account[i...";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              call.nodeId = 184;
              call.nodeLoc = "16:17";
              call.nodeSrc = "assign (feeAmt, client.account[in.accoun...";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              scope.feeAmt = scope.client.account(false, scope.in.accountIndex).payPlan(false, U.minus(scope.client.account(false, scope.in.accountIndex).currentTerm, U.num("1"))).remainingServiceFee;
              call.nodeId = 222;
              call.nodeLoc = "17:17";
              call.nodeSrc = "procedure (calculateDebtTillSettlement)";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              calculateDebtTillSettlement();
            }
          }
        }
      }
    }

    callStack.remove(callStack.size() - 1);
  }

  public void advanceRepayment() {
    final CallInstance call = new CallInstance("advanceRepayment");
    callStack.add(call);
    call.nodeId = 2;
    call.nodeLoc = "1:1";
    call.nodeSrc = "group ( assign(enableApply, false()), as...";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    call.nodeId = 6;
    call.nodeLoc = "2:5";
    call.nodeSrc = "assign(enableApply, false())";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    scope.enableApply = false;
    call.nodeId = 22;
    call.nodeLoc = "3:5";
    call.nodeSrc = "assign(status, \"canNotApply\")";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    scope.status = "canNotApply";
    call.nodeId = 34;
    call.nodeLoc = "4:5";
    call.nodeSrc = "assign(nextStmtDate, client.account[in.a...";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    scope.nextStmtDate = scope.client.account(false, scope.in.accountIndex).nextStmtDate;
    call.nodeId = 56;
    call.nodeLoc = "7:5";
    call.nodeSrc = "assign(_applyBefore, 1)";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    scope._applyBefore = U.num("1");
    call.nodeId = 68;
    call.nodeLoc = "8:5";
    call.nodeSrc = "assign(_applyBefore, client.account[in.a...";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    scope._applyBefore = scope.client.account(false, scope.in.accountIndex).product.advanceRepaymentApply.applyBefore;
    {
      call.nodeId = 92;
      call.nodeLoc = "10:5";
      call.nodeSrc = "condition ( case (daysBetween (businessD...";
      if (breakpoints != null) {
        reached.set(call.nodeId);
        if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
      }
      call.nodeId = 96;
      call.nodeLoc = "11:9";
      call.nodeSrc = "case (daysBetween (businessDay(), nextSt...";
      if (breakpoints != null) {
        reached.set(call.nodeId);
        if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
      }
      if (U.cmp(U.daysBetween(nativeFunctions.businessDay(), scope.nextStmtDate), scope._applyBefore) >= 0) {
        call.nodeId = 120;
        call.nodeLoc = "12:17";
        call.nodeSrc = "group ( assign(enableApply, true()) )";
        if (breakpoints != null) {
          reached.set(call.nodeId);
          if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
        }
        call.nodeId = 124;
        call.nodeLoc = "13:21";
        call.nodeSrc = "assign(enableApply, true())";
        if (breakpoints != null) {
          reached.set(call.nodeId);
          if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
        }
        scope.enableApply = true;
      }
    }
    {
      call.nodeId = 146;
      call.nodeLoc = "17:5";
      call.nodeSrc = "condition ( case ( client.account[in.acc...";
      if (breakpoints != null) {
        reached.set(call.nodeId);
        if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
      }
      call.nodeId = 150;
      call.nodeLoc = "18:9";
      call.nodeSrc = "case ( client.account[in.accountIndex].p...";
      if (breakpoints != null) {
        reached.set(call.nodeId);
        if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
      }
      if (U.cmp(scope.client.account(false, scope.in.accountIndex).product.advanceRepaymentApply.canToApply, false) == 0) {
        call.nodeId = 174;
        call.nodeLoc = "19:13";
        call.nodeSrc = "group( assign(enableApply, false()) )";
        if (breakpoints != null) {
          reached.set(call.nodeId);
          if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
        }
        call.nodeId = 178;
        call.nodeLoc = "20:17";
        call.nodeSrc = "assign(enableApply, false())";
        if (breakpoints != null) {
          reached.set(call.nodeId);
          if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
        }
        scope.enableApply = false;
      } else {
        call.nodeId = 198;
        call.nodeLoc = "23:9";
        call.nodeSrc = "case (client.account[in.accountIndex].ha...";
        if (breakpoints != null) {
          reached.set(call.nodeId);
          if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
        }
        if (U.cmp(scope.client.account(false, scope.in.accountIndex).hasAdvancedClearing, true) == 0) {
          call.nodeId = 220;
          call.nodeLoc = "24:13";
          call.nodeSrc = "group ( assign(status, \"alreadyApplied\")...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          call.nodeId = 224;
          call.nodeLoc = "25:17";
          call.nodeSrc = "assign(status, \"alreadyApplied\")";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          scope.status = "alreadyApplied";
          call.nodeId = 236;
          call.nodeLoc = "26:17";
          call.nodeSrc = "assign(date, client.account[in.accountIndex].batchDate)";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          scope.date = scope.client.account(false, scope.in.accountIndex).batchDate;
          call.nodeId = 258;
          call.nodeLoc = "27:17";
          call.nodeSrc = "assign(appliedDate, client.account[in.ac...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          scope.appliedDate = scope.client.account(false, scope.in.accountIndex).appliedAdvanceRepaymentDate;
          call.nodeId = 280;
          call.nodeLoc = "28:17";
          call.nodeSrc = "assign(txnAmt, client.account[in.account...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          scope.txnAmt = scope.client.account(false, scope.in.accountIndex).advanceRepaymentAmount;
        } else {
          call.nodeId = 306;
          call.nodeLoc = "31:9";
          call.nodeSrc = "case (1 = 1, group ( assign (status, \"ca...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          if (U.cmp(U.num("1"), U.num("1")) == 0) {
            call.nodeId = 314;
            call.nodeLoc = "32:13";
            call.nodeSrc = "group ( assign (status, \"canApply\"), ass...";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            call.nodeId = 318;
            call.nodeLoc = "33:17";
            call.nodeSrc = "assign (status, \"canApply\")";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            scope.status = "canApply";
            call.nodeId = 330;
            call.nodeLoc = "34:17";
            call.nodeSrc = "assign(_deductBefore, client.account[in....";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            scope._deductBefore = scope.client.account(false, scope.in.accountIndex).product.advanceRepaymentApply.deductBefore;
            call.nodeId = 354;
            call.nodeLoc = "35:17";
            call.nodeSrc = "assign(date, setDay(nextStmtDate, _deductBefore))";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            scope.date = U.setDay(scope.nextStmtDate, scope._deductBefore);
            call.nodeId = 378;
            call.nodeLoc = "37:17";
            call.nodeSrc = "group ( //txnAmt is increased in each pr...";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            call.nodeId = 382;
            call.nodeLoc = "39:21";
            call.nodeSrc = "procedure (calcAdvancePrincipalAmt)";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            calcAdvancePrincipalAmt();
            call.nodeId = 392;
            call.nodeLoc = "40:21";
            call.nodeSrc = "procedure (calcAdvanceInterestAmt)";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            calcAdvanceInterestAmt();
            call.nodeId = 402;
            call.nodeLoc = "41:21";
            call.nodeSrc = "procedure (calcAdvanceServiceFeeAmt)";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            calcAdvanceServiceFeeAmt();
            call.nodeId = 412;
            call.nodeLoc = "42:21";
            call.nodeSrc = "procedure (calcValue_addedServiceFee)";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            calcValue_addedServiceFee();
            call.nodeId = 422;
            call.nodeLoc = "43:21";
            call.nodeSrc = "procedure (calcAdvanceRepaymentTxnFee)";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            calcAdvanceRepaymentTxnFee();

            {
              call.nodeId = 434;
              call.nodeLoc = "46:17";
              call.nodeSrc = "condition ( case (client.account[in.acco...";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              call.nodeId = 438;
              call.nodeLoc = "47:21";
              call.nodeSrc = "case (client.account[in.accountIndex].su...";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              if (U.and(U.cmp(scope.client.account(false, scope.in.accountIndex).sumOverpaid, U.num("0")) > 0, U.cmp(scope.txnAmt, U.num("0")) > 0)) {
                call.nodeId = 462;
                call.nodeLoc = "48:25";
                call.nodeSrc = "group ( condition ( case (txnAmt>client....";
                if (breakpoints != null) {
                  reached.set(call.nodeId);
                  if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
                }
                {
                  call.nodeId = 466;
                  call.nodeLoc = "49:29";
                  call.nodeSrc = "condition ( case (txnAmt>client.account[...";
                  if (breakpoints != null) {
                    reached.set(call.nodeId);
                    if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
                  }
                  call.nodeId = 470;
                  call.nodeLoc = "50:33";
                  call.nodeSrc = "case (txnAmt>client.account[in.accountIn...";
                  if (breakpoints != null) {
                    reached.set(call.nodeId);
                    if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
                  }
                  if (U.cmp(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).sumOverpaid) > 0) {
                    call.nodeId = 490;
                    call.nodeLoc = "51:37";
                    call.nodeSrc = "assign(txnAmt, txnAmt-client.account[in....";
                    if (breakpoints != null) {
                      reached.set(call.nodeId);
                      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
                    }
                    scope.txnAmt = U.minus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).sumOverpaid);
                  } else {
                    call.nodeId = 518;
                    call.nodeLoc = "53:33";
                    call.nodeSrc = "case (txnAmt<=client.account[in.accountI...";
                    if (breakpoints != null) {
                      reached.set(call.nodeId);
                      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
                    }
                    if (U.cmp(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).sumOverpaid) <= 0) {
                      call.nodeId = 538;
                      call.nodeLoc = "54:39";
                      call.nodeSrc = "group( assign(txnAmt, 0) )";
                      if (breakpoints != null) {
                        reached.set(call.nodeId);
                        if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
                      }
                      call.nodeId = 542;
                      call.nodeLoc = "55:43";
                      call.nodeSrc = "assign(txnAmt, 0)";
                      if (breakpoints != null) {
                        reached.set(call.nodeId);
                        if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
                      }
                      scope.txnAmt = U.num("0");
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    call.nodeId = 572;
    call.nodeLoc = "66:5";
    call.nodeSrc = "assign(amount, round(txnAmt, 2))";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    scope.amount = U.round(scope.txnAmt, U.num("2"));

    callStack.remove(callStack.size() - 1);
  }

  public void main() {
    final CallInstance call = new CallInstance("main");
    callStack.add(call);
    call.nodeId = 2;
    call.nodeLoc = "1:1";
    call.nodeSrc = "group( assign(date, businessDay()), assi...";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    call.nodeId = 6;
    call.nodeLoc = "2:3";
    call.nodeSrc = "assign(date, businessDay())";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    scope.date = nativeFunctions.businessDay();
    call.nodeId = 22;
    call.nodeLoc = "3:3";
    call.nodeSrc = "assign(enableApply, false())";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    scope.enableApply = false;
    call.nodeId = 38;
    call.nodeLoc = "4:3";
    call.nodeSrc = "assign(status, \"canNotApply\")";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    scope.status = "canNotApply";
    call.nodeId = 50;
    call.nodeLoc = "5:3";
    call.nodeSrc = "assign(nextStmtDate, businessDay())";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    scope.nextStmtDate = nativeFunctions.businessDay();
    call.nodeId = 66;
    call.nodeLoc = "6:3";
    call.nodeSrc = "assign(txnAmt, 1)";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    scope.txnAmt = U.num("1");
    call.nodeId = 78;
    call.nodeLoc = "7:3";
    call.nodeSrc = "assign(feeAmt, 1)";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    scope.feeAmt = U.num("1");
    call.nodeId = 90;
    call.nodeLoc = "8:3";
    call.nodeSrc = "procedure(advanceRepayment)";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    advanceRepayment();

    callStack.remove(callStack.size() - 1);
  }

  public void calcAdvanceRepaymentTxnFee() {
    final CallInstance call = new CallInstance("calcAdvanceRepaymentTxnFee");
    callStack.add(call);
    call.nodeId = 2;
    call.nodeLoc = "2:1";
    call.nodeSrc = "group ( assign(txnAmt, txnAmt + client.a...";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    call.nodeId = 6;
    call.nodeLoc = "3:5";
    call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).product.AdvanceRepaymentServiceFee(false, U.num("0")).parameter);

    callStack.remove(callStack.size() - 1);
  }

  public void calcValue_addedServiceFee() {
    final CallInstance call = new CallInstance("calcValue_addedServiceFee");
    callStack.add(call);
    call.nodeId = 2;
    call.nodeLoc = "2:1";
    call.nodeSrc = "group ( condition ( case(daysBetween(cli...";
    if (breakpoints != null) {
      reached.set(call.nodeId);
      if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
    }
    {
      call.nodeId = 6;
      call.nodeLoc = "4:5";
      call.nodeSrc = "condition ( case(daysBetween(client.acco...";
      if (breakpoints != null) {
        reached.set(call.nodeId);
        if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
      }
      call.nodeId = 10;
      call.nodeLoc = "5:9";
      call.nodeSrc = "case(daysBetween(client.account[in.accou...";
      if (breakpoints != null) {
        reached.set(call.nodeId);
        if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
      }
      if (U.cmp(U.daysBetween(scope.client.account(false, scope.in.accountIndex).createTime, nativeFunctions.businessDay()), U.num("15")) <= 0) {
        call.nodeId = 40;
        call.nodeLoc = "6:13";
        call.nodeSrc = "group()";
        if (breakpoints != null) {
          reached.set(call.nodeId);
          if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
        }
      } else {
        call.nodeId = 48;
        call.nodeLoc = "9:9";
        call.nodeSrc = "case (client.account[in.accountIndex].pr...";
        if (breakpoints != null) {
          reached.set(call.nodeId);
          if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
        }
        if (U.cmp(scope.client.account(false, scope.in.accountIndex).product.ChargeAdvanceRepayment.Value_addedServiceFee, "Remaining") == 0) {
          call.nodeId = 68;
          call.nodeLoc = "10:13";
          call.nodeSrc = "group ( assign(txnAmt, txnAmt + client.a...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          call.nodeId = 72;
          call.nodeLoc = "11:17";
          call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).totalRemLifeInsuranceFee);
          call.nodeId = 98;
          call.nodeLoc = "12:17";
          call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).totalRemInstallmentFee);
          call.nodeId = 124;
          call.nodeLoc = "13:17";
          call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).totalRemPrepayPkgFee);
          call.nodeId = 150;
          call.nodeLoc = "14:17";
          call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).totalRemReplaceServiceFee);
          call.nodeId = 176;
          call.nodeLoc = "15:17";
          call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).totalRemAgentFee);
        } else {
          call.nodeId = 206;
          call.nodeLoc = "18:9";
          call.nodeSrc = "case (client.account[in.accountIndex].pr...";
          if (breakpoints != null) {
            reached.set(call.nodeId);
            if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
          }
          if (U.cmp(scope.client.account(false, scope.in.accountIndex).product.ChargeAdvanceRepayment.Value_addedServiceFee, "CurrentTerm") == 0) {
            call.nodeId = 226;
            call.nodeLoc = "19:13";
            call.nodeSrc = "group ( assign(termNumber, client.accoun...";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            call.nodeId = 230;
            call.nodeLoc = "20:17";
            call.nodeSrc = "assign(termNumber, client.account[in.acc...";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            scope.termNumber = U.minus(scope.client.account(false, scope.in.accountIndex).currentTerm, U.num("1"));
            call.nodeId = 254;
            call.nodeLoc = "21:17";
            call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).payPlan(false, scope.termNumber).remainingLifeInsurance);
            call.nodeId = 286;
            call.nodeLoc = "22:17";
            call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).payPlan(false, scope.termNumber).remainingInstallmentFee);
            call.nodeId = 318;
            call.nodeLoc = "23:17";
            call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).payPlan(false, scope.termNumber).remainingPrepayPkgFee);
            call.nodeId = 350;
            call.nodeLoc = "24:17";
            call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).payPlan(false, scope.termNumber).remainingReplaceSvcFee);
            call.nodeId = 382;
            call.nodeLoc = "25:17";
            call.nodeSrc = "assign(txnAmt, txnAmt + client.account[i...";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            scope.txnAmt = U.plus(scope.txnAmt, scope.client.account(false, scope.in.accountIndex).payPlan(false, scope.termNumber).remainingAgentFee);
          } else {
            call.nodeId = 418;
            call.nodeLoc = "28:9";
            call.nodeSrc = "case (client.account[in.accountIndex].pr...";
            if (breakpoints != null) {
              reached.set(call.nodeId);
              if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
            }
            if (U.cmp(scope.client.account(false, scope.in.accountIndex).product.ChargeAdvanceRepayment.Value_addedServiceFee, "SettlementDate") == 0) {
              call.nodeId = 438;
              call.nodeLoc = "29:13";
              call.nodeSrc = "group ( assign(termNumber, client.accoun...";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              call.nodeId = 442;
              call.nodeLoc = "30:17";
              call.nodeSrc = "assign(termNumber, client.account[in.acc...";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              scope.termNumber = U.minus(scope.client.account(false, scope.in.accountIndex).currentTerm, U.num("1"));
              call.nodeId = 466;
              call.nodeLoc = "32:17";
              call.nodeSrc = "assign (feeAmt, client.account[in.accoun...";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              scope.feeAmt = scope.client.account(false, scope.in.accountIndex).payPlan(false, scope.termNumber).remainingLifeInsurance;
              call.nodeId = 494;
              call.nodeLoc = "33:17";
              call.nodeSrc = "procedure (calculateDebtTillSettlement)";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              calculateDebtTillSettlement();
              call.nodeId = 504;
              call.nodeLoc = "35:17";
              call.nodeSrc = "assign (feeAmt, client.account[in.accoun...";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              scope.feeAmt = scope.client.account(false, scope.in.accountIndex).payPlan(false, scope.termNumber).remainingInstallmentFee;
              call.nodeId = 532;
              call.nodeLoc = "36:17";
              call.nodeSrc = "procedure (calculateDebtTillSettlement)";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              calculateDebtTillSettlement();
              call.nodeId = 542;
              call.nodeLoc = "38:17";
              call.nodeSrc = "assign (feeAmt, client.account[in.accoun...";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              scope.feeAmt = scope.client.account(false, scope.in.accountIndex).payPlan(false, scope.termNumber).remainingPrepayPkgFee;
              call.nodeId = 570;
              call.nodeLoc = "39:17";
              call.nodeSrc = "procedure (calculateDebtTillSettlement)";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              calculateDebtTillSettlement();
              call.nodeId = 580;
              call.nodeLoc = "41:17";
              call.nodeSrc = "assign (feeAmt, client.account[in.accoun...";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              scope.feeAmt = scope.client.account(false, scope.in.accountIndex).payPlan(false, scope.termNumber).remainingReplaceSvcFee;
              call.nodeId = 608;
              call.nodeLoc = "42:17";
              call.nodeSrc = "procedure (calculateDebtTillSettlement)";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              calculateDebtTillSettlement();
              call.nodeId = 618;
              call.nodeLoc = "44:17";
              call.nodeSrc = "assign (feeAmt, client.account[in.accoun...";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              scope.feeAmt = scope.client.account(false, scope.in.accountIndex).payPlan(false, scope.termNumber).remainingAgentFee;
              call.nodeId = 646;
              call.nodeLoc = "45:17";
              call.nodeSrc = "procedure (calculateDebtTillSettlement)";
              if (breakpoints != null) {
                reached.set(call.nodeId);
                if (breakpoints.contains(call.nodeId)) throw new BreakPointException();
              }
              calculateDebtTillSettlement();
            }
          }
        }
      }
    }

    callStack.remove(callStack.size() - 1);
  }
}
