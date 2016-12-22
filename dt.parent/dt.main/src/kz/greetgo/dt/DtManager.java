package kz.greetgo.dt;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public interface DtManager extends HasRegisterNativeProcedure {
  String BATCH_DT_MAIN = "__BATCH_DT_MAIN__";

  DtManager copy();

  DtExecuteResult executeTree(String treeName, SortedMap<String, DtType> scope);

  DtExecuteResult executeTree(String treeName, SortedMap<String, DtType> scope, List<Integer> breakpoints);

  DtExecuteResult testTree(String treeName, SortedMap<String, DtType> scope, List<Integer> breakpoints);

  Map<String, DtType> getScope();


  Map<String, Long> getMetrics();

  void cleanMetrics();
}
