package ste;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.MessageRevFilter;

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

    static {
        logger = Logger.getLogger("App");
    }

    private static String getCloneDir(String rel) {
        return String.format("%s/.isw2repos/%s", System.getProperty("user.home"), rel);
    }

    private static String getCloneInfoLine(String proj, String github, String branch, String local) {
        return String.format("(git) project %s: url=%s (branch=%s, local=%s)", 
            proj, github, branch, local);
    }

    public static void main(String[] args) 
            throws RequestException, InvalidRemoteException, 
                    TransportException, GitAPIException, IOException {

        logger.info("Setup phase...");

        logger.info(String.format("Fetching projects for %s and %s...", STORM, BOOKKEEPER));

        JiraProject jiraStormProject = JiraProject.getProjectByName(STORM);
        JiraProject jiraBookKeeperProject = JiraProject.getProjectByName(BOOKKEEPER);

        logger.info(String.format("Fetching tickets for %s and %s...", STORM, BOOKKEEPER));

        JiraTicket[] jiraStormTickets = JiraTicket.getAllTicketsByName(STORM);
        JiraTicket[] jiraBookKeeperTickets = JiraTicket.getAllTicketsByName(BOOKKEEPER);

        String stormLocal = getCloneDir(STORM_LOCAL);
        String bookKeeperLocal = getCloneDir(BOOKKEEPER_LOCAL);

        logger.info("Checking git repositories...");

        logger.info(getCloneInfoLine(STORM, STORM_GITHUB, STORM_BRANCH, stormLocal));
        GitRepository stormGitRepo = new GitRepository(STORM_GITHUB, STORM_BRANCH, stormLocal);

        logger.info(getCloneInfoLine(BOOKKEEPER, BOOKKEEPER_GITHUB, BOOKKEEPER_BRANCH, bookKeeperLocal));
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
        }
        */
    }

    private static List<Release> sortReleasesByDate(JiraProject project) {
        List<Release> rel = new ArrayList<>();

        JiraProject.Version[] vers = project.getVersions();
        for(JiraProject.Version ver : vers) {
            rel.add(Release.fromJiraVersion(ver));
        }

        rel.removeIf(new Predicate<Release>() {
            @Override
            public boolean test(Release release) {
                return release.getReleaseDate() == null;
            }
        });

        rel.sort(new Comparator<Release>() {
            @Override
            public int compare(Release o1, Release o2) {
                return o1.getReleaseDate().compareTo(o2.getReleaseDate());
            }
        });

        int halfSize = Math.round(rel.size() / 2);
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
        tkts.removeIf(new Predicate<Ticket>() {

            @Override
            public boolean test(Ticket t) {
                return 
                    t.getFixedVersionIdx() == -1 || 
                    t.getOpeningVersionIdx() == -1 || 
                    (t.isInjectedVersionAvail() && t.getInjectedVersionIdx() == -1);
            } 
        });
    }

    private static void removeTicketsIfInconsistent(List<Ticket> tkts) {
        tkts.removeIf(new Predicate<Ticket>() {

            @Override
            public boolean test(Ticket t) {
                int iv = t.getInjectedVersionIdx();
                int ov = t.getOpeningVersionIdx();
                int fv = t.getFixedVersionIdx();

                return iv >= fv || iv > ov /*|| ov > fv*/;
            } 
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
        tkts.removeIf(new Predicate<Ticket>() {
            @Override
            public boolean test(Ticket t) {
                return t.getCommits().isEmpty();
            }       
        });
    }

    private static void reverseTicketsOrder(List<Ticket> tkts) {
        Collections.reverse(tkts);
    }
}

