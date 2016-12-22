package kz.greetgo.dt;

import name.lakhin.eliah.projects.papacarlo.Lexer;
import name.lakhin.eliah.projects.papacarlo.Syntax;
import name.lakhin.eliah.projects.papacarlo.syntax.Error;
import name.lakhin.eliah.projects.papacarlo.syntax.Node;

import java.util.*;
import java.util.function.Consumer;

public class DtManager1 implements DtManager {

  private final Map<String, Consumer<SortedMap<String, DtType>>> nativeProcedures = new HashMap<>();
  private final Map<String, Node> trees = new HashMap<>();
  private final NativeFunctions nativeFunctions;
  private SortedMap<String, DtType> scope;

  public DtManager1(NativeFunctions nativeFunctions) {
    this.nativeFunctions = nativeFunctions;
  }

  private DtManager1(DtManager1 dtManager) {
    nativeFunctions = dtManager.nativeFunctions;
    nativeProcedures.putAll(dtManager.nativeProcedures);
    trees.putAll(dtManager.trees);
  }

  public DtManager1 copy() {
    return new DtManager1(this);
  }

  public void registerNativeProcedure(String name, Consumer<ScopeAccess> body) {
    nativeProcedures.put(name, scope -> body.accept(new ScopeAccess() {
      @Override
      public DtType get(String path) {
        return scope.get(path);
      }

      @Override
      public void set(String path, DtType value) {
        scope.put(path, value);
      }

      @Override
      public Set<String> allPaths() {
        return scope.keySet();
      }
    }));
  }

  private static Node parse(String code) {
    Lexer lexer = DtLang$.MODULE$.lexer();
    Syntax syntax = DtLang$.MODULE$.syntax(lexer);
    lexer.input(code);
    List<Error> errors = new ArrayList<>();
    //noinspection unchecked
    errors.addAll(scala.collection.JavaConverters.asJavaCollectionConverter(syntax.getErrors()).asJavaCollection());
    if (!errors.isEmpty()) throw new IllegalArgumentException("Syntax errors: " + errors);
    return syntax.getRootNode().get();
  }

  public void registerUserProcedure(String name, String code) {
    registerTree(name, code);
  }

  public void registerTree(String treeName, String code) {
    trees.put(treeName, parse(code));
  }

  public DtExecuteResult executeTree(String treeName, SortedMap<String, DtType> scope) {
    return executeTree(treeName, scope, null);
  }

  public DtExecuteResult executeTree(String treeName, SortedMap<String, DtType> scope, List<Integer> breakpoints) {
    if (scope == null) throw new IllegalArgumentException("No scope");
    this.scope = scope;
    DtLangInterpreter interpreter = new DtLangInterpreter(scope, nativeProcedures, trees, nativeFunctions, breakpoints);
    Node treeNode = trees.get(treeName);
    if (treeNode == null) throw new IllegalArgumentException("No tree with name " + treeName);
    interpreter.eval(treeNode);
    return new DtExecuteResult(interpreter.messages(), interpreter.reached());
  }

  public DtExecuteResult testTree(String treeName, SortedMap<String, DtType> scope, List<Integer> breakpoints) {
    if (scope == null) throw new IllegalArgumentException("No scope");
    this.scope = scope;
    DtLangInterpreter interpreter = new DtLangInterpreter(scope, nativeProcedures, trees, nativeFunctions, breakpoints);
    Node treeNode = trees.get(treeName);
    if (treeNode == null) throw new IllegalArgumentException("No tree with name " + treeName);
    try {
      interpreter.eval(treeNode);
    } catch (ErrorException e) {
      interpreter.messages().add(e.msg());
    }
    return new DtExecuteResult(interpreter.messages(), interpreter.reached());
  }

  @Override
  public SortedMap<String, DtType> getScope() {
    return scope;
  }

  @Override
  public Map<String, Long> getMetrics() {
    return new HashMap<>();
  }

  @Override
  public void cleanMetrics() {
  }
}
