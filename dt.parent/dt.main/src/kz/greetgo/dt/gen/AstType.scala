package kz.greetgo.dt.gen

import java.time.{LocalDate, Period}

/**
  * Created by den on 05.10.16.
  */
sealed abstract class AstType {}

case class AstBool() extends AstType

case class AstNum() extends AstType

case class AstStr() extends AstType

case class AstDat() extends AstType

case class AstPer() extends AstType

case class AstObj(obj: java.util.HashMap[String, AstType]) extends AstType

case class AstArr(elem: AstType) extends AstType

case class AstNull() extends AstType
