package kz.greetgo.dt;

import java.util.function.Consumer;

public interface HasRegisterNativeProcedure {
  void registerNativeProcedure(String name, Consumer<ScopeAccess> consumer);
}
