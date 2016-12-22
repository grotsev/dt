package kz.greetgo.dt;

import kz.greetgo.dt.gen.AstObj;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DtManagerBuilder implements HasRegisterNativeProcedure {

  private NativeFunctions nativeFunctions;

  public boolean useDtManager3 = false;

  private AstObj structure = null;

  public void setStructure(AstObj structure) {
    this.structure = structure;
  }

  public DtManagerBuilder(NativeFunctions nativeFunctions) {
    this.nativeFunctions = nativeFunctions;
  }

  public class Options {
    private Options() {
    }

    public boolean genSourceMap = true;
    public boolean genBreakpoint = true;
    public boolean genMetrics = false;

    public void applyTo(DtManager3 dtManager3) {
      dtManager3.genBreakpoint = genBreakpoint;
      dtManager3.genSourceMap = genSourceMap;
      dtManager3.genMetrics = genMetrics;
    }
  }

  public final Options options = new Options();

  public DtManagerBuilder() {
  }

  public DtManagerBuilder setNativeFunctions(NativeFunctions nativeFunctions) {
    this.nativeFunctions = nativeFunctions;
    return this;
  }

  public DtManager newDtManager() {
    return useDtManager3 ? newDtManager3() : newDtManager1();
  }

  public Iterable<String> compilerOptions = null;

  private DtManager newDtManager3() {
    if (nativeFunctions == null) throw new NullPointerException("nativeFunctions == null");
    if (structure == null) throw new NullPointerException("structure == null");
    DtManager3 ret = new DtManager3(nativeFunctions);
    ret.compilerOptions = this.compilerOptions;
    registerTreeList.forEach(r -> ret.registerTree(r.treeName, r.code));
    registerUserProcedureList.forEach(r -> ret.registerUserProcedure(r.name, r.code));
    registerNativeProcedureList.forEach(r -> ret.registerNativeProcedure(r.name, r.consumer));
    ret.setStructure(structure);
    this.options.applyTo(ret);
    ret.compile();
    return ret;
  }

  private DtManager newDtManager1() {
    if (nativeFunctions == null) throw new NullPointerException("nativeFunctions == null");
    DtManager1 ret = new DtManager1(nativeFunctions);
    registerTreeList.forEach(r -> ret.registerTree(r.treeName, r.code));
    registerUserProcedureList.forEach(r -> ret.registerUserProcedure(r.name, r.code));
    registerNativeProcedureList.forEach(r -> ret.registerNativeProcedure(r.name, r.consumer));
    return ret;
  }

  private static class RegisterTree {
    final String treeName;
    final String code;

    public RegisterTree(String treeName, String code) {
      this.treeName = treeName;
      this.code = code;
    }
  }

  private final List<RegisterTree> registerTreeList = new ArrayList<>();

  public void registerTree(String treeName, String code) {
    registerTreeList.add(new RegisterTree(treeName, code));
  }

  private static class RegisterUserProcedure {
    final String name;
    final String code;

    public RegisterUserProcedure(String name, String code) {
      this.name = name;
      this.code = code;
    }
  }

  private final List<RegisterUserProcedure> registerUserProcedureList = new ArrayList<>();

  public void registerUserProcedure(String name, String code) {
    registerUserProcedureList.add(new RegisterUserProcedure(name, code));
  }

  private static class RegisterNativeProcedure {
    final String name;
    final Consumer<ScopeAccess> consumer;

    public RegisterNativeProcedure(String name, Consumer<ScopeAccess> consumer) {
      this.name = name;
      this.consumer = consumer;
    }
  }

  private final List<RegisterNativeProcedure> registerNativeProcedureList = new ArrayList<>();

  @Override
  public void registerNativeProcedure(String name, Consumer<ScopeAccess> consumer) {
    registerNativeProcedureList.add(new RegisterNativeProcedure(name, consumer));
  }
}
