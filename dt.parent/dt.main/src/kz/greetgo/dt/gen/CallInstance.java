package kz.greetgo.dt.gen;

/**
 * Created by den on 10.11.16.
 */
public class CallInstance {
  public final String methodName;
  public int nodeId;
  public String nodeSrc;
  public String nodeLoc;

  public CallInstance(String methodName) {
    this.methodName = methodName;
  }

  @Override
  public String toString() {
    return "(" + methodName + ") " + nodeLoc + ": " + nodeSrc;
  }
}
