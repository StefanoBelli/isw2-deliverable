package ste;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.revwalk.filter.MessageRevFilter;

import ste.csv.CsvWriterException;
import ste.csv.CsvWriter;
import ste.git.GitRepository;
import ste.jirarest.JiraProject;
import ste.jirarest.JiraTicket;
import ste.jirarest.http.Http.RequestException;
import ste.model.Release;
import ste.model.Ticket;

public final class App {
    private static final Logger logger;
    
    private static final String STORM = "STORM";
    private static final String STORM_GITHUB = "https://github.com/apache/storm";
    private static final String STORM_LOCAL = "storm";
    private static final String STORM_BRANCH = "master";
    private static final String BOOKKEEPER = "BOOKKEEPER";
    private static final String BOOKKEEPER_GITHUB = "https://github.com/apache/bookkeeper";
    private static final String BOOKKEEPER_LOCAL = "bookkeeper";
    private static final String BOOKKEEPER_BRANCH = "master";

    private static final String CLONE_INFO_FMT = "(git) project {}: url={} (branch={}, local={})";

    static {
        logger = LoggerFactory.getLogger(App.class.getName());
    }

    private static String getCloneDir(String rel) {
        return String.format("%s/.isw2repos/%s", System.getProperty("user.home"), rel);
    }

    private static String getVersionInfoCsvFilename(String proj) {
        return String.format("csv_output/%s-VersionInfo.csv", proj);
    }

    private static GitRepository stormGitRepo;
    private static GitRepository bookKeeperGitRepo;
    private static List<Release> stormReleases;
    private static List<Release> bookKeeperReleases;
    private static List<Ticket> stormTickets;
    private static List<Ticket> bookKeeperTickets;

    public static void main(String[] args) 
            throws RequestException, GitAPIException, 
                    IOException, CsvWriterException {

        logger.info("Setup phase...");

        logger.info("Fetching projects for {} and {}...", STORM, BOOKKEEPER);

        JiraProject jiraStormProject = JiraProject.getProjectByName(STORM);
        JiraProject jiraBookKeeperProject = JiraProject.getProjectByName(BOOKKEEPER);

        logger.info("Fetching tickets for {} and {}...", STORM, BOOKKEEPER);

        JiraTicket[] jiraStormTickets = JiraTicket.getAllTicketsByName(STORM);
        JiraTicket[] jiraBookKeeperTickets = JiraTicket.getAllTicketsByName(BOOKKEEPER);

        String stormLocal = getCloneDir(STORM_LOCAL);
        String bookKeeperLocal = getCloneDir(BOOKKEEPER_LOCAL);

        logger.info("Checking git repositories...");

        logger.info(CLONE_INFO_FMT, STORM, STORM_GITHUB, STORM_BRANCH, stormLocal);
        stormGitRepo = new GitRepository(STORM_GITHUB, STORM_BRANCH, stormLocal);

        logger.info(CLONE_INFO_FMT, BOOKKEEPER, BOOKKEEPER_GITHUB, BOOKKEEPER_BRANCH, bookKeeperLocal);
        bookKeeperGitRepo = new GitRepository(BOOKKEEPER_GITHUB, BOOKKEEPER_BRANCH, bookKeeperLocal);

        logger.info("Setup phase done");

        filteringSequence(
            jiraStormProject, 
            jiraBookKeeperProject, 
            jiraStormTickets, 
            jiraBookKeeperTickets);
        
        logger.info("Terminating...");

        stormGitRepo.close();
        bookKeeperGitRepo.close();

        logger.info("Graceful termination. Exiting...");
    }

    private static final String STAT_INFO_FMT = "project {} - rem. tickets: {} - rem. releases: {}";
    private static final String STAT_INFO_ONLYREL_FMT = "project {} - rem. releases: {}";
    private static final String STAT_IVINFO_FMT = "project {} - tickets with IV: {}";

    private static void filteringSequence(
                JiraProject jsp, JiraProject jbkp, JiraTicket[] jst, JiraTicket[] jbkt)
            throws CsvWriterException, IOException {
                
        logger.info("Starting filtering sequence...");

        logger.info(STAT_INFO_FMT, STORM, jst.length, jsp.getVersions().length);
        logger.info(STAT_INFO_FMT, BOOKKEEPER, jbkt.length, jbkp.getVersions().length);

        logger.info(
            "Removing releases that have no release date" +
            ", sorting them and then cutting them in half (but includes one extra release" + 
            ", which will be removed later)...");

        stormReleases = sortReleasesByDate(jsp);
        bookKeeperReleases = sortReleasesByDate(jbkp);
        
        logger.info("Linking releases to commits, then removing" + 
                        " the last extra release. This should be fast...");

        linkReleasesToCommits(stormReleases, stormGitRepo);
        linkReleasesToCommits(bookKeeperReleases, bookKeeperGitRepo);

        logger.info("After linking releases to commits" + 
                    " and *removing* the last extra release:");
        
        logger.info(STAT_INFO_ONLYREL_FMT, STORM, stormReleases.size());
        logger.info(STAT_INFO_ONLYREL_FMT, BOOKKEEPER, bookKeeperReleases.size());


        logger.info("Getting relevant infos about tickets OVs, FVs and AVs...");

        stormTickets = initProjectTickets(stormReleases, jst);
        bookKeeperTickets = initProjectTickets(bookKeeperReleases, jbkt);

        removeTicketsIfInvlRelease(stormTickets);
        removeTicketsIfInvlRelease(bookKeeperTickets);

        removeTicketsIfInconsistent(stormTickets);
        removeTicketsIfInconsistent(bookKeeperTickets);

        logger.info("After ticket inconsistency fixup:");

        logger.info(STAT_INFO_FMT, STORM, stormTickets.size(), stormReleases.size());
        logger.info(STAT_INFO_FMT, BOOKKEEPER, bookKeeperTickets.size(), bookKeeperReleases.size());

        logger.info("Linking tickets to commits. This may take a while, please wait...");
        
        linkTicketsToCommits(bookKeeperTickets, bookKeeperGitRepo);
        linkTicketsToCommits(stormTickets, stormGitRepo);
        
        removeTicketsIfNoCommits(stormTickets);
        removeTicketsIfNoCommits(bookKeeperTickets);

        logger.info("After removing tickets if no matching commit could be found:");
        
        logger.info(STAT_INFO_FMT, STORM, stormTickets.size(), stormReleases.size());
        logger.info(STAT_INFO_FMT, BOOKKEEPER, bookKeeperTickets.size(), bookKeeperReleases.size());

        reverseTicketsOrder(stormTickets);
        reverseTicketsOrder(bookKeeperTickets);

        /*
        int ivs = 0;

        for(Ticket t : stormTickets) {

            if(t.isInjectedVersionAvail()) {
                ++ivs;
                System.out.println(String.format("IV = %d, OV = %d, FV = %d", 
                    t.getInjectedVersionIdx(), t.getOpeningVersionIdx(), t.getFixedVersionIdx()));
            }
        }
        System.out.println(stormTickets.size() + ", with IV = " + ivs); 
        System.out.println("BOOKKEEPER-----");

        ivs = 0;
        
        for(Ticket t : bookKeeperTickets) {

            if(t.isInjectedVersionAvail()) {
                ++ivs;
                System.out.println(String.format("IV = %d, OV = %d, FV = %d", 
                    t.getInjectedVersionIdx(), t.getOpeningVersionIdx(), t.getFixedVersionIdx()));
            }
        }

        System.out.println(bookKeeperTickets.size() + ", with IV = " + ivs);
        */

        int stormTicketsWithIv = statTicketsWithIv(stormTickets);
        int bookKeeperTicketsWithIv = statTicketsWithIv(bookKeeperTickets);

        logger.info(STAT_IVINFO_FMT, STORM, stormTicketsWithIv);
        logger.info(STAT_IVINFO_FMT, BOOKKEEPER, bookKeeperTicketsWithIv);

        logger.info("Applying proportion ({} strategy)...", Proportion.STRATEGY_NAME);

        Proportion.apply(stormTickets);
        Proportion.apply(bookKeeperTickets);

        removeTicketsIfInconsistent(stormTickets);
        removeTicketsIfInconsistent(bookKeeperTickets);
        
        logger.info("After proportion and inconistency fixup:");
        
        logger.info(STAT_INFO_FMT, STORM, stormTickets.size(), stormReleases.size());
        logger.info(STAT_INFO_FMT, BOOKKEEPER, bookKeeperTickets.size(), bookKeeperReleases.size());

        /*
        System.out.println("POST-PROPORTION========================");

        ivs = 0;

        for(Ticket t : stormTickets) {

            if(t.isInjectedVersionAvail()) {
                ++ivs;
                System.out.println(String.format("IV = %d, OV = %d, FV = %d", 
                    t.getInjectedVersionIdx(), t.getOpeningVersionIdx(), t.getFixedVersionIdx()));
            }
        }
        System.out.println(stormTickets.size() + ", with IV = " + ivs); 
        System.out.println("BOOKKEEPER-----");

        ivs = 0;
        
        for(Ticket t : bookKeeperTickets) {

            if(t.isInjectedVersionAvail()) {
                ++ivs;
                System.out.println(String.format("IV = %d, OV = %d, FV = %d", 
                    t.getInjectedVersionIdx(), t.getOpeningVersionIdx(), t.getFixedVersionIdx()));
            }
        }
        
        System.out.println(bookKeeperTickets.size() + ", with IV = " + ivs);
        */
        
        /*
        for(Release rel : stormReleases) {
            if(rel.getCommits() != null) {
                System.out.println(rel.getCommits().size());
            } else {
                System.out.println("null");
            }
        }
        
        System.out.println("BOOKKEEPER============");

        for(Release rel : bookKeeperReleases) {
             if(rel.getCommits() != null) {
                System.out.println(rel.getCommits().size());
            } else {
                System.out.println("null");
            }
        }*/

        logger.info("Filtering sequence done");
    }

    private static List<Release> sortReleasesByDate(JiraProject project) 
            throws CsvWriterException, IOException {
        List<Release> rel = new ArrayList<>();

        JiraProject.Version[] vers = project.getVersions();
        for(JiraProject.Version ver : vers) {
            rel.add(Release.fromJiraVersion(ver));
        }

        rel.removeIf(release -> release.getReleaseDate() == null);
        
        rel.sort((o1, o2) -> o1.getReleaseDate().compareTo(o2.getReleaseDate()));

        for(int i = 0; i < rel.size(); ++i) {
            rel.get(i).setIndex(i + 1);
        }

        String csvFilename = getVersionInfoCsvFilename(project.getName());
        CsvWriter.writeAll(csvFilename, Release.class, rel);

        int halfSize = (int) Math.floor(rel.size() / 2f);

        //extra release will be cut out when linking rel -> commits
        //we need the extra rel to get end date of last release
        return rel.subList(0, halfSize + 1);
    }

    private static List<Ticket> initProjectTickets(
            List<Release> rels, JiraTicket[] tkts) {

        List<Ticket> tickets = new ArrayList<>();

        for(JiraTicket tkt : tkts) {
            String key = tkt.getKey();
            
            JiraTicket.Fields tktFields = tkt.getFields();

            String rds = tktFields.getResolutionDate();
            String cds = tktFields.getCreated();
            
            int fixRelIdx = 
                Util.getReleaseIndexByDate(rels, Util.dateFromString(rds.substring(0,10)));
            int openRelIdx = 
                Util.getReleaseIndexByDate(rels, Util.dateFromString(cds.substring(0,10)));
                
            Ticket realTkt = new Ticket(key, openRelIdx, fixRelIdx);

            JiraTicket.Fields.Version[] affVer = tktFields.getVersions();
            if(affVer.length > 0) {
                List<Integer> affRelIdx = new ArrayList<>();
                //System.out.println("===============");
                for(JiraTicket.Fields.Version jfv : affVer) {
                    int relIdx = Util.getReleaseIndexByTicketVersionField(rels, jfv);
                    affRelIdx.add(relIdx);
                    //System.out.println(String.format("release %s, index %d", jfv.getReleaseDate(), relIdx));
                }

                affRelIdx.removeIf(e -> e == -1);
                affRelIdx.sort((o1, o2) -> o1 - o2);

                /*for(int i : affRelIdx) {
                    System.out.println(i);
                }*/

                if(!affRelIdx.isEmpty()) {
                    realTkt.setInjectedVersionIdx(affRelIdx.get(0));
                }
            }

            tickets.add(realTkt);
        }

        return tickets;
    }

    private static void removeTicketsIfInvlRelease(List<Ticket> tkts) {
        tkts.removeIf(t ->
            t.getFixedVersionIdx() == -1 || 
            (t.isInjectedVersionAvail() && t.getInjectedVersionIdx() == -1)
        );
    }

    private static void removeTicketsIfInconsistent(List<Ticket> tkts) {
        tkts.removeIf(t -> {
            int iv = t.getInjectedVersionIdx();
            int ov = t.getOpeningVersionIdx();
            int fv = t.getFixedVersionIdx();

            //alt-cond iv >= fv || iv > ov /*|| ov > fv*/;
            return !(iv < fv && ov >= iv);
            //alt-cond iv == fv || ov > fv;
        });
    }

    private static void linkTicketsToCommits(
            List<Ticket> tkts, GitRepository repo) throws IOException {

        for(Ticket tkt : tkts) {
            List<RevCommit> tktCommits = 
                repo.getFilteredCommits(
                    MessageRevFilter.create(
                        tkt.getKey()));

            tkt.setCommits(tktCommits);
        }
    }

    private static void removeTicketsIfNoCommits(List<Ticket> tkts) {
        tkts.removeIf(t -> t.getCommits().isEmpty());
    }

    private static void reverseTicketsOrder(List<Ticket> tkts) {
        Collections.reverse(tkts);
    }

    private static void linkReleasesToCommits(List<Release> rels, GitRepository repo) throws IOException {
        Calendar cal = Calendar.getInstance();

        for(int i = 1; i < rels.size(); ++i) {
            Release leftBoundaryRel = rels.get(i - 1);
            Release rightBoundaryRel = rels.get(i);

            Date tmpStart = leftBoundaryRel.getReleaseDate();
            Date tmpEnd = rightBoundaryRel.getReleaseDate();

            cal.setTime(tmpStart);
            cal.add(Calendar.DAY_OF_MONTH, 1);

            Date relStartDate = cal.getTime();

            cal.setTime(tmpEnd);
            cal.add(Calendar.DAY_OF_MONTH, -1);

            Date relEndDate = cal.getTime();

            List<RevCommit> relCommits = repo.getFilteredCommits(
                CommitTimeRevFilter.between(relStartDate, relEndDate));

            /*
            System.out.println("===================");
            System.out.println(relStartDate);
            System.out.println(relEndDate);
            */

            leftBoundaryRel.setCommits(relCommits);
        }

        rels.removeLast();
    }

    private static int statTicketsWithIv(List<Ticket> tkts) {
        int ticketsWithIv = 0;

        for(Ticket t : tkts) {
            if(t.isInjectedVersionAvail()) {
                ++ticketsWithIv;
            }
        }

        return ticketsWithIv;
    }
}

