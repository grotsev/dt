package kz.greetgo.dt

import java.time.{LocalDate, Period}

/**
  * All DtTypes
  * Created by den on 12.07.16.
  */
sealed abstract class DtType {}

case class Bool(bool: Boolean) extends DtType

case class Num(num: BigDecimal) extends DtType

case class Str(str: String) extends DtType

case class Dat(dat: LocalDate) extends DtType

case class Per(per: Period) extends DtType

case class Obj(obj: java.util.HashMap[String, DtType]) extends DtType

case class Arr(arr: java.util.ArrayList[DtType]) extends DtType
