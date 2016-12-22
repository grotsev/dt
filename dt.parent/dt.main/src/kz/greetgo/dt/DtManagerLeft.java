package kz.greetgo.dt;

import name.lakhin.eliah.projects.papacarlo.Lexer;
import name.lakhin.eliah.projects.papacarlo.Syntax;
import name.lakhin.eliah.projects.papacarlo.syntax.Error;
import name.lakhin.eliah.projects.papacarlo.syntax.Node;

import java.util.*;
import java.util.function.Consumer;

public class DtManagerLeft {
  private Node root;
  private SortedMap<String, DtType> scope;
  private List<String> messages;
  private final Map<String, Consumer<SortedMap<String, DtType>>> nativeProcedures = new HashMap<>();
  private final Map<String, Node> userProcedures = new HashMap<>();
  private NativeFunctions nativeFunctions;

  public DtManagerLeft(NativeFunctions nativeFunctions) {
    this.nativeFunctions = nativeFunctions;
  }

  public void registerNativeProcedure(String name, Consumer<SortedMap<String, DtType>> body) {
    nativeProcedures.put(name, body);
  }

  private Node parse(String code) {
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
    userProcedures.put(name, parse(code));
  }

  public void setScope(SortedMap<String, DtType> scope) {
    this.scope = scope;
  }

  public SortedMap<String, DtType> getScope() {
    return scope;
  }

  public void setCode(String code) {
    root = parse(code);
  }

  public List<String> getMessages() {
    return messages;
  }

  public void execute() {
    if (root == null) throw new IllegalArgumentException("No root");
    if (scope == null) throw new IllegalArgumentException("No scope");
    DtLangInterpreter interpreter = new DtLangInterpreter(scope, nativeProcedures, userProcedures, nativeFunctions, null);
    interpreter.eval(root);
    messages = interpreter.messages();
  }
}
