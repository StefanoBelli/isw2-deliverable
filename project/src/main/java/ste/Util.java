package ste;

import java.util.Date;
import java.util.List;

import ste.jirarest.JiraTicket;
import ste.model.Release;

import java.time.LocalDate;
import java.time.ZoneId;

public final class Util {
   public static Date dateFromString(String dstr) {
      return Date.from(
               LocalDate.parse(dstr)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant());
   }

   public static int getReleaseIndexByDate(List<Release> r, Date d) {
      int i = 1;
      for (; i < r.size(); ++i) {
         Date rd = r.get(i).getReleaseDate();
         if (rd.compareTo(d) > 0) {
            return i - 1;
         }
      }

      return i;
   }

   public static int getReleaseIndexByTicketVersionField(
         List<Release> rels, JiraTicket.Fields.Version v) {
      
      for(int i = 0; i < rels.size(); ++i) {
         if(rels.get(i).getVersion().equals(v.getName())) {
            return i;
         }
      }

      return -1;
   }
}
