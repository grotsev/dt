package kz.greetgo.dt;

import kz.greetgo.dt.gen.AstObj;

import java.util.SortedMap;
import java.util.function.Consumer;

public class DtManager1Test extends AbstractDtManagerTest {
  @Override
  protected DtManagerWrapper createDtManager(NativeFunctions nativeFunctions) {
    return new DtManagerWrapper() {

      final DtManager1 delegate = new DtManager1(nativeFunctions);

      @Override
      public void registerNativeProcedure(String name, Consumer<ScopeAccess> body) {
        delegate.registerNativeProcedure(name, body);
      }

      @Override
      public void registerUserProcedure(String name, String code) {
        delegate.registerUserProcedure(name, code);
      }

      @Override
      public void registerTree(String treeName, String code) {
        delegate.registerTree(treeName, code);
      }

      @Override
      public DtExecuteResult executeTree(String treeName, SortedMap<String, DtType> scope) {
        return delegate.executeTree(treeName, this.scope = scope);
      }

      private SortedMap<String, DtType> scope;

      @Override
      public SortedMap<String, DtType> getScope() {
        return scope;
      }

      @Override
      public void compile() {
      }

      @Override
      public void setStructure(AstObj structure) {
      }
    };
  }
}
