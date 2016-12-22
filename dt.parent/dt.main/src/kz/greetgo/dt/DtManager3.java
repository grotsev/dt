package kz.greetgo.dt;

import kz.greetgo.dt.gen.AstObj;
import kz.greetgo.dt.gen.DtLangTranslator;
import kz.greetgo.dt.gen.DtRunnable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Consumer;

public class DtManager3 implements DtManager {

  private final Map<String, Consumer<ScopeAccess>> nativeProcedures = new HashMap<>();
  private final NativeFunctions nativeFunctions;
  private Class<DtRunnable> cls;
  private Map<String, String> entryPoints = new HashMap<>();
  private Map<String, String> userProcedures = new HashMap<>();
  private Map<String, DtType> scope;
  // Breakpoint need source map for itself so (!genSourceMap && genBreakpoint) behaves like (!genSourceMap && !genBreakpoint)
  public boolean genSourceMap = true;
  public boolean genBreakpoint = true;
  public boolean genMetrics = false;
  public Iterable<String> compilerOptions;

  public DtManager3(NativeFunctions nativeFunctions) {
    this.nativeFunctions = nativeFunctions;
  }

  public DtManager3(DtManager3 original) {
    if (original.cls == null) throw new IllegalArgumentException("Original is not compiled");
    nativeFunctions = original.nativeFunctions;
    cls = original.cls;
    nativeProcedures.putAll(original.nativeProcedures);
    structure = original.structure;
    genMetrics = original.genMetrics;
    if (genMetrics) {
      metrics.putAll(original.metrics);
    }
  }

  public DtManager3 copy() {
    return new DtManager3(this);
  }

  private AstObj structure;

  public void setStructure(AstObj structure) {
    checkNotCompiled();
    this.structure = structure;
  }

  public void registerNativeProcedure(String name, Consumer<ScopeAccess> body) {
    nativeProcedures.put(name, body);
  }

  public void registerUserProcedure(String name, String code) {
    checkNotCompiled();
    userProcedures.put(name, code);
  }

  public void registerTree(String treeName, String code) {
    checkNotCompiled();
    entryPoints.put(treeName, code);
  }

  public DtExecuteResult executeTree(String treeName, SortedMap<String, DtType> scope) {
    return executeTree(treeName, scope, null);
  }

  private final Map<String, Long> metrics = new HashMap<>();

  @Override
  public Map<String, Long> getMetrics() {
    return Collections.unmodifiableMap(metrics);
  }

  @Override
  public void cleanMetrics() {
    metrics.clear();
  }

  private void appendMetrics(Map<String, Long> execMetrics) {
    execMetrics.entrySet().forEach(e -> {
      Long value = metrics.get(e.getKey());
      if (value == null) value = e.getValue();
      else value += e.getValue();
      metrics.put(e.getKey(), value);
    });
  }

  public DtExecuteResult executeTree(String treeName, SortedMap<String, DtType> scope, List<Integer> breakpoints) {
    try {
      DtRunnable exe = cls.newInstance();
      exe.setNativeProcedures(nativeProcedures);
      exe.setNativeFunctions(nativeFunctions);
      exe.exec(treeName, scope, breakpoints == null ? null : new HashSet<>(breakpoints));
      if (genMetrics) appendMetrics(exe.getExecMetrics());
      this.scope = exe.scope();
      scope.clear();
      scope.putAll(exe.scope());
      DtExecuteResult r = new DtExecuteResult(exe.messages(), exe.reached());
      return r;
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }


  public DtExecuteResult testTree(String treeName, SortedMap<String, DtType> scope, List<Integer> breakpoints) {
    try {
      DtRunnable exe = cls.newInstance();
      exe.setNativeProcedures(nativeProcedures);
      exe.setNativeFunctions(nativeFunctions);
      try {
        exe.exec(treeName, scope, new HashSet<>(breakpoints));
      } catch (ErrorException e) {
        exe.messages().add(e.msg());
      }
      this.scope = exe.scope();
      DtExecuteResult r = new DtExecuteResult(exe.messages(), exe.reached());
      return r;
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public void compile() {
    checkNotCompiled();
    if (structure == null) throw new NullPointerException("Structure is not initialized");
    DtLangTranslator tr = new DtLangTranslator(structure, genSourceMap, genBreakpoint, genMetrics);
    for (Map.Entry<String, String> e : entryPoints.entrySet())
      tr.registerEntryPoint(e.getKey(), e.getValue());
    for (Map.Entry<String, String> e : userProcedures.entrySet())
      tr.registerUserProcedure(e.getKey(), e.getValue());
    cls = tr.compile(compilerOptions);
  }

  private void checkNotCompiled() {
    if (cls != null) throw new IllegalStateException("Can not change compiled manager");
  }

  public Map<String, DtType> getScope() {
    return scope;
  }

}
