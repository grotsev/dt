package kz.greetgo.dt

/**
  * Created by den on 06.08.16.
  */
sealed abstract class DtRef {}

case class ObjRef(objRef: Obj, field: String) extends DtRef
case class ArrRef(arrRef: Arr, index: Integer) extends DtRef
