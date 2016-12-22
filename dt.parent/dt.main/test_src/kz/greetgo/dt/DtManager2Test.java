package kz.greetgo.dt;

import kz.greetgo.dt.gen.AstObj;

import java.util.Set;
import java.util.SortedMap;
import java.util.function.Consumer;

public class DtManager2Test extends AbstractDtManagerTest {

  @Override
  protected DtManagerWrapper createDtManager(NativeFunctions nativeFunctions) {
    return new DtManagerWrapper() {

      final DtManager2 delegate = new DtManager2(nativeFunctions);

      @Override
      public void registerNativeProcedure(String name, Consumer<ScopeAccess> body) {
        delegate.registerNativeProcedure(name, obj -> {
          SortedMap<String, DtType> scope1 = DtManagerUtil.objToScope(obj);
          body.accept(new ScopeAccess() {
            @Override
            public DtType get(String path) {
              return scope1.get(path);
            }

            @Override
            public void set(String path, DtType value) {
              // TODO
            }

            @Override
            public Set<String> allPaths() {
              return null; // TODO
            }
          });
        });
      }

      @Override
      public void compile() {
      }

      @Override
      public void setStructure(AstObj structure) {
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
        Obj obj = DtManagerUtil.scopeToObj(scope);
        DtExecuteResult ret = delegate.executeTree(treeName, obj);
        this.scope = DtManagerUtil.objToScope(obj);
        return ret;
      }

      private SortedMap<String, DtType> scope;

      @Override
      public SortedMap<String, DtType> getScope() {
        return scope;
      }
    };
  }
}
