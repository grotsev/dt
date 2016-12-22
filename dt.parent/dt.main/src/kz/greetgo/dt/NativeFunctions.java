package kz.greetgo.dt;

import scala.math.BigDecimal;

import java.time.LocalDate;
import java.util.SortedMap;

public interface NativeFunctions {
  LocalDate today();

  LocalDate businessDay();

  String generateId();

  long nextNumber();

  void echo(DtType value);

  void echoScope(SortedMap<String, DtType> scope); // TODO [den] implement in DtLangTranslator

  boolean dictInc(String dictCode, String fieldCode, String rowCode, BigDecimal incrementValue);

  BigDecimal dictValueNumber(String dictCode, String fieldCode, String rowCode);

  String dictValueText(String dictCode, String fieldCode, String rowCode);
}
