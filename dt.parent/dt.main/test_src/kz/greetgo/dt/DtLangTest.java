package kz.greetgo.dt;

import name.lakhin.eliah.projects.papacarlo.Lexer;
import name.lakhin.eliah.projects.papacarlo.Syntax;
import name.lakhin.eliah.projects.papacarlo.lexis.FragmentController;
import name.lakhin.eliah.projects.papacarlo.syntax.Error;
import name.lakhin.eliah.projects.papacarlo.syntax.Node;
import org.testng.annotations.Test;
import scala.Some;
import scala.collection.immutable.List;
import scala.collection.immutable.Map;
import scala.collection.immutable.Set;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;

public class DtLangTest {
  @Test
  public void execute() throws Exception {
    Lexer lexer = DtLang$.MODULE$.lexer();
    Syntax syntax = DtLang$.MODULE$.syntax(lexer);

    lexer.input("group(empty(),empty(),empty())");
    List<Error> errors = syntax.getErrors();
    System.out.println(errors);
    FragmentController fragments = lexer.fragments();
    System.out.println(fragments);

    System.out.println(lexer);

    scala.Some<Node> root = (Some<Node>) syntax.getRootNode();
    Node node = root.get();

    Map<String, List<Node>> branches = node.getBranches();
    Set<String> keySet = branches.keySet();
    
    System.out.println(node.getBranches());

  }

  @Test(enabled = false)
  public void testDate(){
    LocalDate d = LocalDate.of(2016,9,29);
    System.out.println(d);
    d = d.withDayOfMonth(31);
    Period p = Period.ofMonths(1);
    d = d.plus(p);
    System.out.println(d);
    d = d.plus(p);
    System.out.println(d);
    d = d.plus(p);
    System.out.println(d);
  }
}
