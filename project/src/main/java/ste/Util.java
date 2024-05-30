package ste;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;
import java.io.InputStream;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import ste.csv.CsvWriter;
import ste.csv.CsvWriterException;
import ste.jirarest.JiraProject;
import ste.jirarest.JiraTicket;
import ste.model.Release;
import ste.model.Ticket;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

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

   public static String readAllFile(String path) 
         throws IOException {
      return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
   }

   public static void csv2Arff(String csvContent, String outArff) throws IOException {
      InputStream csvContentIstream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

      CSVLoader csvLoader = new CSVLoader();
      csvLoader.setSource(csvContentIstream);
      Instances entries = csvLoader.getDataSet();

      csvContentIstream.close();

      ArffSaver arffSaver = new ArffSaver();
      arffSaver.setInstances(entries);
      arffSaver.setFile(new File(outArff));
      arffSaver.writeBatch();
   }
   
   public static int numOfPositives(Instances insts) {
      return numOf(insts, "yes");
   }

   public static int numOfNegatives(Instances insts) {
      return numOf(insts, "no");
   }

   private static int numOf(Instances insts, String label) {
      int numPosInsts = 0;
      int attrIdx = insts.numAttributes() - 1;

      for(int i = 0; i < insts.size(); ++i) {
         if(insts.get(i).toString(attrIdx).equals(label)) {
            ++numPosInsts;
         }
      }

      return numPosInsts;
   }

   public static ProgressBar buildProgressBar(String msg, int max) {
      ProgressBarBuilder builder = new ProgressBarBuilder();
      return builder
         .setTaskName(msg)
         .setStyle(ProgressBarStyle.ASCII)
         .setInitialMax(max)
         .continuousUpdate()
         .setMaxRenderedLength(150)
            .build();
   }

   private static String getVersionInfoCsvFilename(String proj) {
      return String.format("csv_output/%s-VersionInfo.csv", proj);
   }

   public interface ReleaseCutter {
      int rightBorder(int nRels);
   }

   public static List<Release> sortReleasesByDate(JiraProject project, ReleaseCutter cutter)
         throws CsvWriterException, IOException {
      List<Release> rel = new ArrayList<>();

      JiraProject.Version[] vers = project.getVersions();
      for (JiraProject.Version ver : vers) {
         rel.add(Release.fromJiraVersion(ver));
      }

      rel.removeIf(release -> release.getReleaseDate() == null);

      rel.sort((o1, o2) -> o1.getReleaseDate().compareTo(o2.getReleaseDate()));

      for (int i = 0; i < rel.size(); ++i) {
         rel.get(i).setIndex(i + 1);
      }

      String csvFilename = getVersionInfoCsvFilename(project.getName());
      CsvWriter.writeAll(csvFilename, Release.class, rel);

      return rel.subList(0, cutter.rightBorder(rel.size()));
   }

   public static List<Ticket> initProjectTickets(
         List<Release> rels, JiraTicket[] tkts) {

      List<Ticket> tickets = new ArrayList<>();

      for (JiraTicket tkt : tkts) {
         String key = tkt.getKey();

         JiraTicket.Fields tktFields = tkt.getFields();

         String rds = tktFields.getResolutionDate();
         String cds = tktFields.getCreated();

         int fixRelIdx = Util.getReleaseIndexByDate(rels, Util.dateFromString(rds.substring(0, 10)));
         int openRelIdx = Util.getReleaseIndexByDate(rels, Util.dateFromString(cds.substring(0, 10)));

         Ticket realTkt = new Ticket(key, openRelIdx, fixRelIdx);

         JiraTicket.Fields.Version[] affVer = tktFields.getVersions();
         if (affVer.length > 0) {
            List<Integer> affRelIdx = new ArrayList<>();
            for (JiraTicket.Fields.Version jfv : affVer) {
               int relIdx = Util.getReleaseIndexByTicketVersionField(rels, jfv);
               affRelIdx.add(relIdx);
            }

            affRelIdx.removeIf(e -> e == -1);
            affRelIdx.sort((o1, o2) -> o1 - o2);

            if (!affRelIdx.isEmpty()) {
               realTkt.setInjectedVersionIdx(affRelIdx.get(0));
            }
         }

         tickets.add(realTkt);
      }

      return tickets;
   }

   public static void removeTicketsIfInconsistent(List<Ticket> tkts) {
      tkts.removeIf(t -> {
         int iv = t.getInjectedVersionIdx();
         int ov = t.getOpeningVersionIdx();
         int fv = t.getFixedVersionIdx();

         return !(iv < fv && ov >= iv && fv >= ov);
      });
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
