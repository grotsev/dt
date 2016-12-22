package kz.greetgo.dt;

import name.lakhin.eliah.projects.papacarlo.Lexer;
import name.lakhin.eliah.projects.papacarlo.Syntax;
import name.lakhin.eliah.projects.papacarlo.syntax.Error;
import name.lakhin.eliah.projects.papacarlo.syntax.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by den on 05.08.16.
 */
public class DtManager2 {
  private final Map<String, Consumer<Obj>> nativeProcedures = new HashMap<>();
  private final Map<String, Node> trees = new HashMap<>();
  private final NativeFunctions nativeFunctions;

  public DtManager2(NativeFunctions nativeFunctions) {
    this.nativeFunctions = nativeFunctions;
  }

  private DtManager2(DtManager2 dtManager) {
    nativeFunctions = dtManager.nativeFunctions;
    nativeProcedures.putAll(dtManager.nativeProcedures);
    trees.putAll(dtManager.trees);
  }

  public DtManager2 copy() {
    return new DtManager2(this);
  }

  public void registerNativeProcedure(String name, Consumer<Obj> body) {
    nativeProcedures.put(name, body);
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

  public DtExecuteResult executeTree(String treeName, Obj scope) {
    return executeTree(treeName, scope, null);
  }

  public DtExecuteResult executeTree(String treeName, Obj scope, List<Integer> breakpoints) {
    if (scope == null) throw new IllegalArgumentException("No scope");
    DtLangInterpreter2 interpreter = new DtLangInterpreter2(scope, nativeProcedures, trees, nativeFunctions, breakpoints);
    Node treeNode = trees.get(treeName);
    if (treeNode == null) throw new IllegalArgumentException("No tree with name " + treeName);
    interpreter.eval(treeNode);
    return new DtExecuteResult(interpreter.messages(), interpreter.reached());
  }
}
