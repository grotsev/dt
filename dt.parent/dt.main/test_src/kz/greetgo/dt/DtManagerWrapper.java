package kz.greetgo.dt;

import kz.greetgo.dt.gen.AstObj;

import java.util.SortedMap;
import java.util.function.Consumer;

public interface DtManagerWrapper {
  void registerNativeProcedure(String name, Consumer<ScopeAccess> body);

  void registerUserProcedure(String name, String code);

  void registerTree(String treeName, String code);

  DtExecuteResult executeTree(String treeName, SortedMap<String, DtType> scope);

  SortedMap<String, DtType> getScope();

  void compile();

  void setStructure(AstObj structure);
}
