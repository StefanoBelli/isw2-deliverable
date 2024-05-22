package ste;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.tongfei.progressbar.ProgressBar;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.revwalk.filter.MessageRevFilter;

import ste.csv.CsvWriterException;
import ste.evaluation.WalkForward;
import ste.analyzer.BugAnalyzer;
import ste.analyzer.BugAnalyzerException;
import ste.analyzer.metrics.MetricsException;
import ste.csv.CsvWriter;
import ste.git.GitRepository;
import ste.jirarest.JiraProject;
import ste.jirarest.JiraTicket;
import ste.jirarest.http.Http.RequestException;
import ste.model.JavaSourceFile;
import ste.model.Release;
import ste.model.Result;
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
    
    private static String getDatasetCsvFilename(String proj) {
        return String.format("csv_output/%s-Dataset.csv", proj);
    }
    
    private static String getResultCsvFilename(String proj) {
        return String.format("csv_output/%s-Result.csv", proj);
    }

    private static GitRepository stormGitRepo;
    private static GitRepository bookKeeperGitRepo;
    private static List<Release> stormReleases;
    private static List<Release> bookKeeperReleases;
    private static List<Ticket> stormTickets;
    private static List<Ticket> bookKeeperTickets;

    private static final String INFO_WROTE_FMT = "project {} - wrote csv @ {}";
    private static final String INFO_ANALYSIS_FMT = "project {} - running analysis, this may take some time...";

    public static void main(String[] args) 
            throws Exception {

        boolean skipDsCreat = false;

        if(args.length > 0) {
            for(String arg : args) {
                if(arg.equals("--skip-ds-creat") || arg.equals("-s")) {
                    skipDsCreat = true;
                } else if(arg.equals("--no-skip-ds-creat") || arg.equals("-d")) {
                    skipDsCreat = false;
                }
            }
        }

        logger.info("Setup phase...");

        logger.info("Fetching projects for {} and {}...", STORM, BOOKKEEPER);

        JiraProject jiraStormProject = JiraProject.getProjectByName(STORM);
        JiraProject jiraBookKeeperProject = JiraProject.getProjectByName(BOOKKEEPER);

        if(!skipDsCreat) {
            dsCreat(jiraStormProject, jiraBookKeeperProject);
        } else {
            logger.warn("SKIPPING dataset creation, as requested by user");
        }

        evaluate(jiraStormProject, jiraBookKeeperProject);

        logger.info("Graceful termination. Exiting...");
    }

    private static void evaluate(JiraProject jiraStormProject, JiraProject jiraBookKeeperProject) 
            throws Exception {

        logger.info("Evaluating projects {} and {}...", STORM, BOOKKEEPER);

        String stormName = jiraStormProject.getName();
        String bookKeeperName = jiraBookKeeperProject.getName();

        WalkForward stormWf = new WalkForward(
            stormName, 
            Util.readAllFile(getDatasetCsvFilename(stormName)));
        
        stormWf.start();

        WalkForward bookKeeperWf = new WalkForward(
            bookKeeperName, 
            Util.readAllFile(getDatasetCsvFilename(bookKeeperName)));

        bookKeeperWf.start();

        logger.info("Writing results...");

        String resultCsvStorm = getResultCsvFilename(stormName);

        CsvWriter.writeAll(
            resultCsvStorm, 
            Result.class, 
            stormWf.getResults());
        
        logger.info(INFO_WROTE_FMT, STORM, resultCsvStorm);

        String resultCsvBookKeeper = getResultCsvFilename(bookKeeperName);

        CsvWriter.writeAll(
            resultCsvBookKeeper, 
            Result.class, 
            bookKeeperWf.getResults());

        logger.info(INFO_WROTE_FMT, BOOKKEEPER, resultCsvBookKeeper);
    }

    private static void dsCreat(JiraProject jiraStormProject, JiraProject jiraBookKeeperProject) 
            throws RequestException, CsvWriterException, IOException, 
                    GitAPIException, BugAnalyzerException, MetricsException {
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
            
        logger.info("Project analysis phase");

        BugAnalyzer stormAnalyzer = 
            new BugAnalyzer(STORM, stormReleases, stormTickets, stormGitRepo);
        BugAnalyzer bookKeeperAnalyzer = 
            new BugAnalyzer(BOOKKEEPER, bookKeeperReleases, bookKeeperTickets, bookKeeperGitRepo);
            
        logReleasesWithNoCommit(stormReleases, bookKeeperReleases);

        logger.info(INFO_ANALYSIS_FMT, STORM);
        stormAnalyzer.startAnalysis();
        
        logger.info("Done.");
        logger.info(INFO_ANALYSIS_FMT, BOOKKEEPER);
        bookKeeperAnalyzer.startAnalysis();

        logger.info("Done.");
        logger.info("Writing results...");

        String stormDatasetCsv = getDatasetCsvFilename(jiraStormProject.getName());

        CsvWriter.writeAll(
            stormDatasetCsv, 
            JavaSourceFile.class, 
            stormAnalyzer.getResults());

        logger.info(INFO_WROTE_FMT, STORM, stormDatasetCsv);

        String bookKeeperDatasetCsv = getDatasetCsvFilename(jiraBookKeeperProject.getName());

        CsvWriter.writeAll(
            bookKeeperDatasetCsv, 
            JavaSourceFile.class, 
            bookKeeperAnalyzer.getResults());

        logger.info(INFO_WROTE_FMT, BOOKKEEPER, bookKeeperDatasetCsv);

        stormGitRepo.close();
        bookKeeperGitRepo.close();
    }

    private static final String EMPTY_COMMIT_REL_FMT = "project {}: rel {} - has no commit";

    private static void logReleasesWithNoCommit(
            List<Release> storm, List<Release> bookKeeper) {
        
        for(Release rel : storm) {
            if(rel.getCommits().isEmpty()) {
                logger.warn(EMPTY_COMMIT_REL_FMT, STORM, rel.getVersion());
            }
        }

        for(Release rel : bookKeeper) {
            if(rel.getCommits().isEmpty()) {
                logger.warn(EMPTY_COMMIT_REL_FMT, BOOKKEEPER, rel.getVersion());
            }
        }
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
        
        logger.info("Linking releases to commits. This should be fast...");

        linkReleasesToCommits(stormReleases, stormGitRepo, STORM);
        linkReleasesToCommits(bookKeeperReleases, bookKeeperGitRepo, BOOKKEEPER);

        logger.info("After linking releases to commits:");
        
        logger.info(STAT_INFO_ONLYREL_FMT, STORM, stormReleases.size());
        logger.info(STAT_INFO_ONLYREL_FMT, BOOKKEEPER, bookKeeperReleases.size());

        logger.info("Getting relevant infos about tickets OVs, FVs and AVs...");

        stormTickets = initProjectTickets(stormReleases, jst);
        bookKeeperTickets = initProjectTickets(bookKeeperReleases, jbkt);

        bookKeeperReleases.remove(bookKeeperReleases.size() - 1);
        stormReleases.remove(stormReleases.size() - 1);

        removeTicketsIfInconsistent(stormTickets);
        removeTicketsIfInconsistent(bookKeeperTickets);

        logger.info("After ticket inconsistency fixup (and extra rel removal):");

        logger.info(STAT_INFO_FMT, STORM, stormTickets.size(), stormReleases.size());
        logger.info(STAT_INFO_FMT, BOOKKEEPER, bookKeeperTickets.size(), bookKeeperReleases.size());

        logger.info("Linking tickets to commits. This may take a while, please wait...");
        
        linkTicketsToCommits(bookKeeperTickets, bookKeeperGitRepo, BOOKKEEPER);
        linkTicketsToCommits(stormTickets, stormGitRepo, STORM);
        
        removeTicketsIfNoCommits(stormTickets);
        removeTicketsIfNoCommits(bookKeeperTickets);

        logger.info("After removing tickets if no matching commit could be found:");
        
        logger.info(STAT_INFO_FMT, STORM, stormTickets.size(), stormReleases.size());
        logger.info(STAT_INFO_FMT, BOOKKEEPER, bookKeeperTickets.size(), bookKeeperReleases.size());

        reverseTicketsOrder(stormTickets);
        reverseTicketsOrder(bookKeeperTickets);

        int stormTicketsWithIv = statTicketsWithIv(stormTickets);
        int bookKeeperTicketsWithIv = statTicketsWithIv(bookKeeperTickets);

        logger.info(STAT_IVINFO_FMT, STORM, stormTicketsWithIv);
        logger.info(STAT_IVINFO_FMT, BOOKKEEPER, bookKeeperTicketsWithIv);

        logger.info("Applying proportion ({} strategy)...", Proportion.STRATEGY_NAME);

        Proportion.apply(stormTickets, stormReleases.size() + 1);
        Proportion.apply(bookKeeperTickets, bookKeeperReleases.size() + 1);

        removeTicketsIfInconsistent(stormTickets);
        removeTicketsIfInconsistent(bookKeeperTickets);
        
        logger.info("After proportion and inconistency fixup:");
        
        logger.info(STAT_INFO_FMT, STORM, stormTickets.size(), stormReleases.size());
        logger.info(STAT_INFO_FMT, BOOKKEEPER, bookKeeperTickets.size(), bookKeeperReleases.size());

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
                for(JiraTicket.Fields.Version jfv : affVer) {
                    int relIdx = Util.getReleaseIndexByTicketVersionField(rels, jfv);
                    affRelIdx.add(relIdx);
                }

                affRelIdx.removeIf(e -> e == -1);
                affRelIdx.sort((o1, o2) -> o1 - o2);

                if(!affRelIdx.isEmpty()) {
                    realTkt.setInjectedVersionIdx(affRelIdx.get(0));
                }
            }

            tickets.add(realTkt);
        }

        return tickets;
    }

    private static void removeTicketsIfInconsistent(List<Ticket> tkts) {
        tkts.removeIf(t -> {
            int iv = t.getInjectedVersionIdx();
            int ov = t.getOpeningVersionIdx();
            int fv = t.getFixedVersionIdx();

            return !(iv < fv && ov >= iv && fv >= ov);
        });
    }

    private static void linkTicketsToCommits(
            List<Ticket> tkts, GitRepository repo, String projName) throws IOException {
        
        String pbMsg = String.format("Linking tickets to commits for project %s", projName);
        try(ProgressBar pb = Util.buildProgressBar(pbMsg, tkts.size())) {
            for(Ticket tkt : tkts) {
                List<RevCommit> tktCommits = 
                    repo.getFilteredCommits(
                        MessageRevFilter.create(
                            tkt.getKey()));

                tkt.setCommits(tktCommits);
                pb.step();
            }
        }
    }

    private static void removeTicketsIfNoCommits(List<Ticket> tkts) {
        tkts.removeIf(t -> t.getCommits().isEmpty());
    }

    private static void reverseTicketsOrder(List<Ticket> tkts) {
        Collections.reverse(tkts);
    }

    private static void linkReleasesToCommits(List<Release> rels, GitRepository repo, String projName) throws IOException {
        Release firstRel = rels.get(0);

        Date firstStart = firstRel.getReleaseDate();

        List<RevCommit> firstRelCommits = repo.getFilteredCommits(
            CommitTimeRevFilter.before(firstStart));

        firstRel.setCommits(firstRelCommits);
        
        int relsSize = rels.size();
        String pbMsg = String.format("Linking releases to commits for project %s", projName);
        try(ProgressBar pb = Util.buildProgressBar(pbMsg, relsSize - 1)) {
            for(int i = 0; i < relsSize - 1; ++i) {
                Release leftBoundaryRel = rels.get(i);
                Release rightBoundaryRel = rels.get(i + 1);

                Date start = leftBoundaryRel.getReleaseDate();
                Date end = rightBoundaryRel.getReleaseDate();
                List<RevCommit> relCommits = repo.getFilteredCommits(
                    CommitTimeRevFilter.between(start, end));

                rightBoundaryRel.setCommits(relCommits);

                pb.step();
            }
        }
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

