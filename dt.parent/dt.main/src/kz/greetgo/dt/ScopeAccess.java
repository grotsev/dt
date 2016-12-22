package kz.greetgo.dt;

import java.util.Set;

public interface ScopeAccess {

  DtType get(String path);

  void set(String path, DtType value);

  Set<String> allPaths();
}
