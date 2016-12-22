package kz.greetgo.dt.gen;

import kz.greetgo.dt.DtType;
import kz.greetgo.dt.NativeFunctions;
import kz.greetgo.dt.ScopeAccess;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by den on 26.10.16.
 */
public interface DtRunnable extends ScopeAccess {
  void cleanExecMetrics();

  Map<String, Long> getExecMetrics();

  Map<String, DtType> exec(String treeName, Map<String, DtType> extScope, Set<Integer> breakpoints);

  void registerNativeProcedure(String name, Consumer<ScopeAccess> body);

  Map<String, DtType> scope();

  List<String> messages();

  Set<Integer> reached();

  void setNativeProcedures(Map<String, Consumer<ScopeAccess>> nativeProcedures);

  void setNativeFunctions(NativeFunctions nativeFunctions);
}
