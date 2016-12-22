package kz.greetgo.dt

import java.util

import scala.collection.JavaConversions._

object ObjToScope {
  def doIt(scopeObj: DtType, scope: util.SortedMap[String, DtType], path: String): Unit = {
    scopeObj match {
      case o@Obj(obj) => obj.entrySet().foreach(e =>
        doIt(e.getValue, scope, if (path.isEmpty) e.getKey else path + "." + e.getKey)
      )
      case a@Arr(arr) => arr.indices.foreach(i =>
        doIt(arr(i), scope, if (path.isEmpty) "" + i else path + "." + i)
      )
      case x => scope.put(path, x)
    }
  }
}
