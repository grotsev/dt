package kz.greetgo.dt.gen;

import kz.greetgo.dt.ErrorException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by den on 13.10.16.
 */
public class U {

  private static BigDecimal nvl(BigDecimal l) {
    return l == null ? BigDecimal.ZERO : l;
  }

  private static Period nvl(Period l) {
    return l == null ? Period.ZERO : l;
  }

  public static Boolean nvl(Boolean l) {
    return l == null ? false : l;
  }

  public static boolean nvl(boolean l) { // override for optimization
    return l;
  }

  private static String nvl(String l) {
    return l == null ? "" : l;
  }

  // num

  public static BigDecimal num(String val) {
    return new BigDecimal(val);
  }

  // plus

  public static BigDecimal plus(BigDecimal l, BigDecimal r) {
    return nvl(l).add(nvl(r));
  }

  public static LocalDate plus(LocalDate dat, Period per) {
    return dat == null ? null : dat.plus(nvl(per));
  }

  public static Period plus(Period l, Period r) {
    return nvl(l).plus(nvl(r));
  }

  // minus

  public static BigDecimal minus(BigDecimal l) {
    return l == null ? BigDecimal.ZERO : l.negate();
  }

  public static Period minus(Period l) {
    return l == null ? Period.ZERO : l.negated();
  }

  public static Boolean minus(Boolean l) {
    return l == null ? true : !l;
  }

  public static BigDecimal minus(BigDecimal l, BigDecimal r) {
    return nvl(l).subtract(nvl(r));
  }

  public static LocalDate minus(LocalDate dat, Period per) {
    return dat == null ? null : dat.minus(nvl(per));
  }

  public static Period minus(Period l, Period r) {
    return nvl(l).minus(nvl(r));
  }

  public static Period minus(LocalDate l, LocalDate r) {
    return r == null || l == null ? null : l.until(r);
  }

  // mult

  public static BigDecimal mult(BigDecimal l, BigDecimal r) {
    return l == null || r == null ? BigDecimal.ZERO : l.multiply(r);
  }

  public static Period mult(Period per, BigDecimal num) {
    return per == null || num == null ? Period.ZERO : per.multipliedBy(num.intValue());
  }

  // div, rem

  public static BigDecimal div(BigDecimal l, BigDecimal r) {
    return nvl(l).divide(nvl(r), MathContext.DECIMAL128);
  }

  public static BigDecimal rem(BigDecimal l, BigDecimal r) {
    return nvl(l).remainder(nvl(r));
  }

  // cmp

  public static int cmp(BigDecimal l, BigDecimal r) {
    return nvl(l).compareTo(nvl(r));
  }

  public static int cmp(String l, String r) {
    return l == r ? 0 : (l == null ? -1 : l.compareTo(r));
  }

  public static int cmp(LocalDate l, LocalDate r) {
    return l == null ? (r == null ? 0 : -1) : (r == null ? 1 : l.compareTo(r));
  }

  public static int cmp(Boolean l, Boolean r) {
    return nvl(l).compareTo(nvl(r));
  }

  // and, or

  public static Boolean and(Boolean l, Boolean r) {
    return nvl(l) && nvl(r);
  }

  public static Boolean or(Boolean l, Boolean r) {
    return nvl(l) || nvl(r);
  }

  // subs

  public static String subs(String s, BigDecimal from) {
    return s.substring(crop(from, s));
  }

  public static String subs(String s, BigDecimal from, BigDecimal to) {
    return s.substring(crop(from, s), crop(to, s));
  }

  private static int crop(BigDecimal nb, String s) {
    int l = s.length();
    int n = nb.intValue();
    return n < 0 ? 0 : (n > l ? l : n);
  }

  // dateParse

  public static LocalDate dateParse(String s) {
    try {
      return LocalDate.parse(nvl(s).substring(0, 10));
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public static LocalDate dateParse(BigDecimal year, BigDecimal month, BigDecimal day) {
    try {
      return LocalDate.of(nvl(year).intValue(), nvl(month).intValue(), nvl(day).intValue());
    } catch (DateTimeException e) {
      return null;
    }
  }

  // indexOf, lasIndexOf

  public static BigDecimal indexOf(String str, String substr) {
    return new BigDecimal(nvl(str).indexOf(nvl(substr)));
  }

  public static BigDecimal indexOf(String str, String substr, BigDecimal num) {
    return new BigDecimal(nvl(str).indexOf(nvl(substr), nvl(num).intValue()));
  }

  public static BigDecimal lastIndexOf(String str, String substr) {
    return new BigDecimal(nvl(str).lastIndexOf(nvl(substr)));
  }

  public static BigDecimal lastIndexOf(String str, String substr, BigDecimal num) {
    return new BigDecimal(nvl(str).lastIndexOf(nvl(substr), nvl(num).intValue()));
  }

  // startsWith, endsWith

  public static Boolean startsWith(String str, String substr) {
    return nvl(str).startsWith(nvl(substr));
  }

  public static Boolean endsWith(String str, String substr) {
    return nvl(str).endsWith(nvl(substr));
  }

  // round, floor, power, log

  public static BigDecimal round(BigDecimal num) {
    return nvl(num).setScale(0, BigDecimal.ROUND_HALF_UP);
  }

  public static BigDecimal round(BigDecimal num, BigDecimal scale) {
    return nvl(num).setScale(nvl(scale).intValue(), BigDecimal.ROUND_HALF_UP);
  }

  public static BigDecimal floor(BigDecimal num) {
    return nvl(num).setScale(0, BigDecimal.ROUND_FLOOR);
  }

  public static BigDecimal power(BigDecimal num, BigDecimal pow) {
    return new BigDecimal(Math.pow(nvl(num).doubleValue(), nvl(pow).doubleValue()));
  }

  public static BigDecimal log(BigDecimal base, BigDecimal num) {
    return new BigDecimal(Math.log(nvl(num).doubleValue()) / Math.log(nvl(base).doubleValue()));
  }

  // day, month, year

  public static BigDecimal day(LocalDate dat) {
    return dat == null ? null : new BigDecimal(dat.getDayOfMonth());
  }

  public static BigDecimal month(LocalDate dat) {
    return dat == null ? null : new BigDecimal(dat.getMonthValue());
  }

  public static BigDecimal year(LocalDate dat) {
    return dat == null ? null : new BigDecimal(dat.getYear());
  }

  // daysBetween, daysInMonth, setDay

  public static BigDecimal daysBetween(LocalDate l, LocalDate r) {
    return l == null || r == null ? null : new BigDecimal(ChronoUnit.DAYS.between(l, r));
  }

  public static BigDecimal daysInMonth(LocalDate dat) {
    return dat == null ? null : new BigDecimal(dat.lengthOfMonth());
  }

  public static LocalDate setDay(LocalDate dat, BigDecimal day) {
    if (dat == null) throw new NullPointerException();
    if (day == null) throw new NullPointerException();
    try {
      return dat.withDayOfMonth(day.intValue());
    } catch (DateTimeException e) {
      return null;
    }
  }

  // toNum

  public static BigDecimal toNum(String str) {
    try {
      return new BigDecimal(nvl(str).replaceAll("[_ ]", "").replace(',', '.'));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public static void error(String str) {
    throw new ErrorException(str);
  }

  public static BigDecimal len(ArrayList a) {
    return a == null ? BigDecimal.ZERO : new BigDecimal(a.size());
  }

  public static <T extends Object & Comparable<? super T>> T min(ArrayList<T> a) {
    return a == null ? null : Collections.min(a);
  }

  public static <T extends Object & Comparable<? super T>> T max(ArrayList<T> a) {
    return a == null ? null : Collections.max(a);
  }

  public static <T> void ensureSize(final ArrayList<T> list, final int size) {
    if (size <= list.size()) return;

    list.addAll(new AbstractList<T>() {
      @Override
      public Object[] toArray() {
        return new Object[size - list.size()];
      }

      @Override
      public T get(int index) {
        throw new UnsupportedOperationException("Not supported yet.");
      }

      @Override
      public int size() {
        throw new UnsupportedOperationException("Not supported yet.");
      }
    });
  }

  public static <T> String toStr(T t) {
    return t == null ? "" : t.toString();
  }

  public static String format(LocalDate dat, String fmt) {
    try {
      return dat.format(DateTimeFormatter.ofPattern(fmt));
    } catch (Exception e) {
      return null;
    }
  }

}
