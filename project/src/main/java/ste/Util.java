package ste;

import java.util.Date;
import java.time.LocalDate;
import java.time.ZoneId;

public final class Util {
   public static Date dateFromString(String dstr) {
    return Date.from(
                LocalDate.parse(dstr)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant());
   }
}
