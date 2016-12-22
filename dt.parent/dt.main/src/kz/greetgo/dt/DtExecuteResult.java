package kz.greetgo.dt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DtExecuteResult {
  public final List<String> messages;
  public final Set<Integer> reached;

  private final Map<String, Object> attributes = new HashMap<>();

  public void setAttribute(String name, Object value) {
    attributes.put(name, value);
  }

  @SuppressWarnings("unchecked")
  public <T> T getAttribute(String name) {
    return (T) attributes.get(name);
  }

  public DtExecuteResult(List<String> messages, Set<Integer> reached) {
    this.messages = messages;
    this.reached = reached;
  }
}
