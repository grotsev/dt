package kz.greetgo.dt;

import java.util.*;
import java.util.stream.Collectors;

public class DtManagerUtil {
  public static Obj scopeToObj(Map<String, DtType> scope) {
    Map<String, DtType> index = new HashMap<>();
    for (Map.Entry<String, DtType> entry : scope.entrySet()) {
      List<String> path = Arrays.asList(entry.getKey().split("\\."));
      DtType value = entry.getValue();
      index.put(entry.getKey(), value);
      for (int i = path.size(); i > 0; i--) {
        List<String> childPath = path.subList(0, i);
        //String childStr = childPath.stream().collect(Collectors.joining("."));
        List<String> parentPath = path.subList(0, i - 1);
        String parentStr = parentPath.stream().collect(Collectors.joining("."));

        String strIndex = childPath.get(childPath.size() - 1);
        try {
          int intIndex = Integer.parseInt(strIndex);
          Arr parent = (Arr) index.get(parentStr);
          if (parent == null) {
            parent = new Arr(new ArrayList<>());
            index.put(parentStr, parent);
          }
          arrPut(parent.arr(), intIndex, value);
          value = parent;
        } catch (NumberFormatException e) {
          Obj parent = (Obj) index.get(parentStr);
          if (parent == null) {
            parent = new Obj(new HashMap<>());
            index.put(parentStr, parent);
          }
          parent.obj().put(strIndex, value);
          value = parent;
        }
      }
    }
    Obj obj = (Obj) index.get("");
    return obj != null ? obj : new Obj(new HashMap<>());
  }

  public static SortedMap<String, DtType> objToScope(Obj obj) {
    SortedMap<String, DtType> index = new TreeMap<>();
    ObjToScope$.MODULE$.doIt(obj, index, "");
    return index;
  }


  public static <T> void arrPut(ArrayList<T> arr, int index, T value) {
    while (arr.size() <= index) arr.add(null);
    arr.set(index, value);
  }

  public static <T> T arrGet(ArrayList<T> arr, int index) {
    if (index < 0) return null;
    if (index >= arr.size()) return null;
    return arr.get(index);
  }
}
