package ste;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.tongfei.progressbar.ProgressBar;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.revwalk.filter.MessageRevFilter;

import ste.csv.CsvWriterException;
import ste.evaluation.NonMatchingSetsSizeException;
import ste.evaluation.WalkForward;
import ste.evaluation.WalkForwardSplit;
import ste.evaluation.WalkForwardSplitIterator;
import ste.analyzer.BugAnalyzer;
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
import weka.core.converters.ConverterUtils.DataSource;

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

    private static String getDatasetCsvFilename(String proj) {
        return String.format("csv_output/%s-Dataset.csv", proj);
    }
    
    private static String getResultCsvFilename(String proj) {
        return String.format("csv_output/%s-Result.csv", proj);
    }

    private static String getTrainingSetDir(String proj) {
        return String.format("csv_output/%s/training-set", proj);
    }
    
    private static String getTestingSetDir(String proj) {
        return String.format("csv_output/%s/testing-set", proj);
    }
        
    private static String getTrainingSetFilename(String proj, int wfIter) {
        return String.format("%s/%s-TrainingSet-%d.csv", getTrainingSetDir(proj), proj, wfIter);
    }

    private static String getTestingSetFilename(String proj, int wfIter) {
        return String.format("%s/%s-TestingSet-%d.csv", getTestingSetDir(proj), proj, wfIter);
    }

    private static final String LOOKUP_PY_SCRIPT_FILENAME = "csv_output/lookup-npofb20.py";

    private static GitRepository stormGitRepo;
    private static GitRepository bookKeeperGitRepo;
    private static List<Release> stormReleases;
    private static List<Release> bookKeeperReleases;

    private static final String INFO_WROTE_FMT = "project {} - wrote csv @ {}";
    private static final String INFO_ANALYSIS_FMT = "project {} - running analysis, this may take some time...";

    public static void main(String[] args) 
            throws Exception {

        boolean skipDsCreat = false;

        for(String arg : args) {
            if(arg.equals("--skip-ds-creat") || arg.equals("-s")) {
                skipDsCreat = true;
            } else if(arg.equals("--no-skip-ds-creat") || arg.equals("-d")) {
                skipDsCreat = false;
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

        logger.info("writing python lookup script for NPofB20...");

        writeLookupPythonScript(LOOKUP_PY_SCRIPT_FILENAME);

        logger.info("Graceful termination. Exiting...");
    }

    private static void writeLookupPythonScript(String scriptPath) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder
            .append("#!/usr/bin/python3\n")
            .append("import csv\n")
            .append("import sys\n")
            .append("with open(f\"{sys.argv[1]}-Result.csv\") as csvf:\n")
            .append("\treader = csv.reader(csvf)\n")
            .append("\tfor row in reader:\n")
            .append("\t\tif str.lower(row[5]) == str.lower(sys.argv[2]) and \\\n")
            .append("\t\t\tstr.lower(row[6]) == str.lower(sys.argv[3]) and \\\n")
            .append("\t\t\tstr.lower(row[7]) == str.lower(sys.argv[4]) and \\\n")
            .append("\t\t\tstr.lower(row[8]) == str.lower(sys.argv[5]) and \\\n")
            .append("\t\t\tstr(int(row[1])) == sys.argv[6]:\n")
            .append("\t\t\tprint(f\"{row[18]}\")\n")
            .append("\t\t\tbreak\n");

        try(FileOutputStream fos = new FileOutputStream(scriptPath)) {
            fos.write(builder.toString().getBytes());
        }

        logger.info("wrote python lookup script for NPofB20 @ {}", scriptPath);
        logger.info("usage: python {} <Dataset> <Classifier> <FeatureSelection> <Balancing> <Sensitivity> <WalkForwardIter>", 
            scriptPath);
    }

    private static void evaluate(JiraProject jiraStormProject, JiraProject jiraBookKeeperProject) 
            throws Exception {

        logger.info("Evaluating projects {} and {}...", STORM, BOOKKEEPER);

        String stormName = jiraStormProject.getName();
        String bookKeeperName = jiraBookKeeperProject.getName();
        
        WalkForward bookKeeperWf = buildWalkForward(bookKeeperName);
        bookKeeperWf.start();

        String resultCsvBookKeeper = getResultCsvFilename(bookKeeperName);

        CsvWriter.writeAll(
            resultCsvBookKeeper, 
            Result.class, 
            bookKeeperWf.getResults());

        logger.info(INFO_WROTE_FMT, BOOKKEEPER, resultCsvBookKeeper);

        WalkForward stormWf = buildWalkForward(stormName);
        stormWf.start();

        String resultCsvStorm = getResultCsvFilename(stormName);

        CsvWriter.writeAll(
            resultCsvStorm, 
            Result.class, 
            stormWf.getResults());
        
        logger.info(INFO_WROTE_FMT, STORM, resultCsvStorm);
    }

    private static void dsCreat(JiraProject jiraStormProject, JiraProject jiraBookKeeperProject) 
            throws RequestException, CsvWriterException, IOException, 
                    GitAPIException, MetricsException {
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

        var allTkts = filteringSequence(
            jiraStormProject, 
            jiraBookKeeperProject, 
            jiraStormTickets, 
            jiraBookKeeperTickets);
            
        logger.info("Project analysis phase");

        List<Ticket> stormTickets = allTkts.getFirst();
        List<Ticket> bookKeeperTickets = allTkts.getSecond();

        List<Ticket> stormTicketsCpy = Ticket.copyTickets(stormTickets);
        List<Ticket> bookKeeperTicketsCpy = Ticket.copyTickets(bookKeeperTickets);

        doProportion(BOOKKEEPER, bookKeeperTickets, bookKeeperReleases);
        doProportion(STORM, stormTickets, stormReleases);

        BugAnalyzer stormAnalyzer = 
            new BugAnalyzer(STORM, stormReleases, stormTickets, stormGitRepo);
        BugAnalyzer bookKeeperAnalyzer = 
            new BugAnalyzer(BOOKKEEPER, bookKeeperReleases, bookKeeperTickets, bookKeeperGitRepo);
            
        logReleasesWithNoCommit(stormReleases, bookKeeperReleases);

        logger.info(INFO_ANALYSIS_FMT, BOOKKEEPER);
        bookKeeperAnalyzer.startAnalysis();
        
        cutReleasesInHalf(bookKeeperReleases, bookKeeperAnalyzer.getResults());
        String bookKeeperDatasetCsv = getDatasetCsvFilename(jiraBookKeeperProject.getName());

        CsvWriter.writeAll(
            bookKeeperDatasetCsv, 
            JavaSourceFile.class, 
            bookKeeperAnalyzer.getResults());
            
        logger.info(INFO_WROTE_FMT, BOOKKEEPER, bookKeeperDatasetCsv);

        createTrainingSetsWithTestingSets(
            jiraBookKeeperProject.getName(), bookKeeperReleases, bookKeeperTicketsCpy, 
            bookKeeperGitRepo, bookKeeperAnalyzer.getResults());

        logger.info(INFO_ANALYSIS_FMT, STORM);
        stormAnalyzer.startAnalysis();

        cutReleasesInHalf(stormReleases, stormAnalyzer.getResults());
        String stormDatasetCsv = getDatasetCsvFilename(jiraStormProject.getName());

        CsvWriter.writeAll(
            stormDatasetCsv, 
            JavaSourceFile.class, 
            stormAnalyzer.getResults());

        logger.info(INFO_WROTE_FMT, STORM, stormDatasetCsv);

        createTrainingSetsWithTestingSets(
            jiraStormProject.getName(), stormReleases, stormTicketsCpy, 
            stormGitRepo, stormAnalyzer.getResults());

        stormGitRepo.close();
        bookKeeperGitRepo.close();
    }

    private static List<String> getSortedSetsCsv(String setDir) throws IOException {
        List<Util.Pair<String, Integer>> sets = new ArrayList<>();
        File directory = new File(setDir);
        for(File file : directory.listFiles()) {
            String setCsv = Util.readAllFile(setDir + "/" + file.getName());
            int setOrder;
            try(Scanner in = new Scanner(file.getName()).useDelimiter("[^0-9]+")) {
                setOrder = in.nextInt();
            }
            sets.add(new Util.Pair<>(setCsv, setOrder));
        }

        sets.sort((e1, e2) -> e1.getSecond() - e2.getSecond());

        List<String> newedSets = new ArrayList<>();
        for(Util.Pair<String, Integer> set : sets) {
            newedSets.add(set.getFirst());
        }

        return newedSets;
    }

    private static WalkForward buildWalkForward(String projName) throws Exception {
        Util.csv2Arff(Util.readAllFile(getDatasetCsvFilename(projName)), "dataset-tmp.arff");
        int datasetSize = new DataSource("dataset-tmp.arff").getDataSet().size();
        var trainingSets = getSortedSetsCsv(getTrainingSetDir(projName));
        var testingSets = getSortedSetsCsv(getTestingSetDir(projName));
        if(trainingSets.size() != testingSets.size()) {
            throw new NonMatchingSetsSizeException();
        }
        List<WalkForwardSplit> splits = new ArrayList<>();
        for(int i = 0; i < trainingSets.size(); ++i) {
            splits.add(new WalkForwardSplit(trainingSets.get(i), testingSets.get(i), i));
        }
        return new WalkForward(projName, datasetSize, new WalkForwardSplitIterator(splits));
    }

    private static void cutReleasesInHalf(List<Release> rels, List<JavaSourceFile> jsfs) {
        List<Release> halfRels = rels.subList(0, Math.round(rels.size() / 2f));

        jsfs.removeIf(elem -> {
            for(Release r : halfRels) {
                if(elem.getRelease().getVersion().equals(r.getVersion())) {
                    return false;
                }
            }

            return true;
        });
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

    private static Util.Pair<List<Ticket>, List<Ticket>> filteringSequence(
                JiraProject jsp, JiraProject jbkp, JiraTicket[] jst, JiraTicket[] jbkt)
            throws CsvWriterException, IOException {
                
        logger.info("Starting filtering sequence...");

        logger.info(STAT_INFO_FMT, STORM, jst.length, jsp.getVersions().length);
        logger.info(STAT_INFO_FMT, BOOKKEEPER, jbkt.length, jbkp.getVersions().length);

        logger.info(
            "Removing releases that have no release date" +
            ", sorting them and then cutting them in half (but includes one extra release" + 
            ", which will be removed later)...");

        stormReleases = Util.sortReleasesByDate(jsp, nrels -> nrels);
        bookKeeperReleases = Util.sortReleasesByDate(jbkp, nrels -> nrels);
        
        logger.info("Linking releases to commits. This should be fast...");

        linkReleasesToCommits(stormReleases, stormGitRepo, STORM);
        linkReleasesToCommits(bookKeeperReleases, bookKeeperGitRepo, BOOKKEEPER);

        logger.info("After linking releases to commits:");
        
        logger.info(STAT_INFO_ONLYREL_FMT, STORM, stormReleases.size());
        logger.info(STAT_INFO_ONLYREL_FMT, BOOKKEEPER, bookKeeperReleases.size());

        logger.info("Getting relevant infos about tickets OVs, FVs and AVs...");

        List<Ticket> stormTickets = Util.initProjectTickets(stormReleases, jst);
        List<Ticket> bookKeeperTickets = Util.initProjectTickets(bookKeeperReleases, jbkt);
        
        Util.enableIvComputationIfMinorTicketInconsistency(stormTickets);
        Util.enableIvComputationIfMinorTicketInconsistency(bookKeeperTickets);

        Util.removeTicketsIfInconsistent(stormTickets);
        Util.removeTicketsIfInconsistent(bookKeeperTickets);

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

        int stormTicketsWithIv = statTicketsWithIv(stormTickets);
        int bookKeeperTicketsWithIv = statTicketsWithIv(bookKeeperTickets);

        logger.info(STAT_IVINFO_FMT, STORM, stormTicketsWithIv);
        logger.info(STAT_IVINFO_FMT, BOOKKEEPER, bookKeeperTicketsWithIv);

        return new Util.Pair<>(stormTickets, bookKeeperTickets);
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

    //releases formal parameter is needed for log purposes only
    private static void doProportion(String projName, List<Ticket> tickets, List<Release> releases) 
            throws RequestException, CsvWriterException, IOException {
        logger.info("Applying proportion ({} strategy)...", Proportion.STRATEGY_NAME);
        logger.info("for project {}...", projName);
        Proportion.apply(tickets);
        Util.removeTicketsIfInconsistent(tickets);
        logger.info("After proportion and inconistency fixup:");
        logger.info(STAT_INFO_FMT, projName, tickets.size(), releases.size());
        logger.info("Filtering sequence done");
    }

    private static void createTrainingSetsWithTestingSets(
        String projName, List<Release> rels, List<Ticket> tkts, GitRepository repo, List<JavaSourceFile> jsfs) 
            throws IOException, MetricsException, RequestException, CsvWriterException {
                
        List<Release> halfRels = rels.subList(0, Math.round(rels.size() / 2f));

        int realSplitNum = 1;

        for(int i = 2; i <= halfRels.size(); ++i) {
            if(halfRels.get(i - 1).getCommits().isEmpty()) {
                continue;
            }

            final int curRelIdx = i;

            List<JavaSourceFile> testingSet = JavaSourceFile.copyJavaSourceFiles(jsfs);
            testingSet.removeIf(e -> e.getRelease().getIndex() != curRelIdx);
            CsvWriter.writeAll(
                getTestingSetFilename(projName, realSplitNum), 
                JavaSourceFile.class,
                testingSet);

            ++realSplitNum;
        }

        realSplitNum = 1;

        for(int i = 1; i <= halfRels.size() - 1; ++i) {
            if(halfRels.get(i).getCommits().isEmpty()) {
                continue;
            }

            final int curRelIdx = i;

            List<Release> trainingSetRels = Release.copyReleases(halfRels);
            trainingSetRels.removeIf(e -> e.getIndex() > curRelIdx);

            List<Ticket> trainingSetTkts = Ticket.copyTickets(tkts);
            trainingSetTkts.removeIf(e -> e.getFixedVersionIdx() + 1 > curRelIdx);
            Ticket.ensureResetArtificialIv(trainingSetTkts);

            List<JavaSourceFile> trainingSet = JavaSourceFile.copyJavaSourceFiles(jsfs);
            trainingSet.removeIf(e -> e.getRelease().getIndex() > curRelIdx);

            doProportion(projName, trainingSetTkts, trainingSetRels);

            BugAnalyzer bugAnalyzer = 
                new BugAnalyzer(projName, trainingSetRels, trainingSetTkts, repo, trainingSet);
            
            bugAnalyzer.startAnalysis();

            CsvWriter.writeAll(
                getTrainingSetFilename(projName, realSplitNum), 
                JavaSourceFile.class, 
                bugAnalyzer.getResults());

            ++realSplitNum;
        }
    }
}

