package kz.greetgo.dt;

import kz.greetgo.dt.gen.AstObj;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

public class DtManager3Test extends AbstractDtManagerTest {

  @Override
  protected DtManagerWrapper createDtManager(NativeFunctions nativeFunctions) {
    return new DtManagerWrapper() {
      final DtManager3 delegate = new DtManager3(nativeFunctions);

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
        return delegate.executeTree(treeName, scope);
      }

      @Override
      public SortedMap<String, DtType> getScope() {
        return new TreeMap<>(delegate.getScope());
      }

      @Override
      public void compile() {
        delegate.compile();
      }

      @Override
      public void setStructure(AstObj structure) {
        delegate.setStructure(structure);
      }
    };
  }

  @Override
  public void testWithoutDash() {
    super.testWithoutDash();
  }

  @Override
  public void emptyGroup() {
    super.emptyGroup();
  }

  @Override
  public void foreach_break_complex() throws Exception {
    super.foreach_break_complex();
  }

  @Override
  public void type_inference() throws Exception {
    super.type_inference();
  }

}
