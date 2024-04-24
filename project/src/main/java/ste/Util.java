package ste;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import ste.jirarest.JiraTicket;
import ste.model.Release;

public final class Util {
   private Util() {}
   
   public static Date dateFromString(String dstr) {
      return Date.from(
               LocalDate.parse(dstr)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant());
   }

   public static int getReleaseIndexByDate(List<Release> r, Date d) {
      for (int i = 0; i < r.size(); ++i) {
         Date rd = r.get(i).getReleaseDate();
         if (rd.compareTo(d) > 0) {
            return i;
         }
      }

      return -1;
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

   public static int countLines(String s) {
      return s.split("\r\n|\r|\n").length;
   }

   public static ProgressBar buildProgressBar(String msg, int max) {
      ProgressBarBuilder builder = new ProgressBarBuilder();
      return builder
         .setTaskName(msg)
         .setStyle(ProgressBarStyle.ASCII)
         .setInitialMax(max)
         .continuousUpdate()
         .build();
   }

   public static final class IntListWide {

      private IntListWide() {}

      private static IntStream asStream(List<Integer> lst) {
         return lst.stream().mapToInt(e -> e);
      }
      
      public static int avg(List<Integer> lst) {
         return lst.isEmpty() ? 0 : (int) Math.round(
               asStream(lst).average().orElse(0f)
         );
      }

      public static int sum(List<Integer> lst) {
         return lst.isEmpty() ? 0 : asStream(lst).sum();
      }

      public static List<Integer> eachSub(List<Integer> l1, List<Integer> l2) {
         List<Integer> res = new ArrayList<>();

         for(int i1 = 0; i1 < l1.size(); ++i1) {
            res.add(l1.get(i1) - l2.get(i1));
         }

         return res;
      }
   }

   public static final class Threeple<I,J,K> {
      private final I i;
      private final J j;
      private final K k;

      public Threeple(I i, J j, K k) {
         this.i = i;
         this.j = j;
         this.k = k;
      }

      public I getFirst() {
          return i;
      }

      public J getSecond() {
          return j;
      }
      
      public K getThird() {
          return k;
      }
   }
   public static final class Pair<I,J> {
      private final I i;
      private final J j;

      public Pair(I i, J j) {
         this.i = i;
         this.j = j;
      }

      public I getFirst() {
          return i;
      }

      public J getSecond() {
          return j;
      }
   }
}
