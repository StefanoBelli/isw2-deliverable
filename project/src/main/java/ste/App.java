package ste;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.MessageRevFilter;

import ste.csv.CsvWriterException;
import ste.csv.CsvWriter;
import ste.git.GitRepository;
import ste.jirarest.JiraProject;
import ste.jirarest.JiraTicket;
import ste.jirarest.util.Http.RequestException;
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
        logger = LoggerFactory.getLogger("App");
    }

    private static String getCloneDir(String rel) {
        return String.format("%s/.isw2repos/%s", System.getProperty("user.home"), rel);
    }

    private static String getVersionInfoCsvFilename(String proj) {
        return String.format("csv_output/%s-VersionInfo.csv", proj);
    }

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
        GitRepository stormGitRepo = new GitRepository(STORM_GITHUB, STORM_BRANCH, stormLocal);

        logger.info(CLONE_INFO_FMT, BOOKKEEPER, BOOKKEEPER_GITHUB, BOOKKEEPER_BRANCH, bookKeeperLocal);
        GitRepository bookKeeperGitRepo = new GitRepository(BOOKKEEPER_GITHUB, BOOKKEEPER_BRANCH, bookKeeperLocal);

        logger.info("Setup phase done");

        List<Release> stormReleases = sortReleasesByDate(jiraStormProject);
        List<Release> bookKeeperReleases = sortReleasesByDate(jiraBookKeeperProject);

        List<Ticket> stormTickets = initProjectTickets(stormReleases, jiraStormTickets);
        List<Ticket> bookKeeperTickets = initProjectTickets(bookKeeperReleases, jiraBookKeeperTickets);

        removeTicketsIfInvlRelease(stormTickets);
        removeTicketsIfInvlRelease(bookKeeperTickets);

        removeTicketsIfInconsistent(stormTickets);
        removeTicketsIfInconsistent(bookKeeperTickets);
        
        linkTicketsToCommits(bookKeeperTickets, bookKeeperGitRepo);
        linkTicketsToCommits(stormTickets, stormGitRepo);
        
        removeTicketsIfNoCommits(stormTickets);
        removeTicketsIfNoCommits(bookKeeperTickets);

        reverseTicketsOrder(stormTickets);
        reverseTicketsOrder(bookKeeperTickets);

        /* 
        for(Ticket t : stormTickets) {

            if(t.isInjectedVersionAvail()) {
                System.out.println(String.format("IV = %d, OV = %d, FV = %d", 
                    t.getInjectedVersionIdx(), t.getOpeningVersionIdx(), t.getFixedVersionIdx()));
            }
        }
        
        System.out.println("BOOKKEEPER-----");
        
        for(Ticket t : bookKeeperTickets) {

            if(t.isInjectedVersionAvail()) {
                System.out.println(String.format("IV = %d, OV = %d, FV = %d", 
                    t.getInjectedVersionIdx(), t.getOpeningVersionIdx(), t.getFixedVersionIdx()));
            }
        }*/

        stormGitRepo.close();
        bookKeeperGitRepo.close();
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
        for(int i = 0; i < halfSize; ++i) {
            rel.remove(rel.size() - 1);
        }

        return rel;
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
                for(JiraTicket.Fields.Version jfv : affVer) {
                    int relIdx = Util.getReleaseIndexByTicketVersionField(rels, jfv);
                    affRelIdx.add(relIdx);
                }

                realTkt.setInjectedVersionIdx(affRelIdx.get(0));
                realTkt.setAffectedVersionsIdxs(affRelIdx);
            }

            tickets.add(realTkt);
        }

        return tickets;
    }

    private static void removeTicketsIfInvlRelease(List<Ticket> tkts) {
        tkts.removeIf(t ->
            t.getFixedVersionIdx() == -1 || 
            t.getOpeningVersionIdx() == -1 || 
            (t.isInjectedVersionAvail() && t.getInjectedVersionIdx() == -1)
        );
    }

    private static void removeTicketsIfInconsistent(List<Ticket> tkts) {
        tkts.removeIf(t -> {
            int iv = t.getInjectedVersionIdx();
            int ov = t.getOpeningVersionIdx();
            int fv = t.getFixedVersionIdx();

            return iv >= fv || iv > ov /*|| ov > fv*/;
        });
    }

    private static void linkTicketsToCommits(
            List<Ticket> tkts, GitRepository repo) {

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
}

