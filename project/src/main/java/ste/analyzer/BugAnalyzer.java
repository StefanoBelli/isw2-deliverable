package ste.analyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

import me.tongfei.progressbar.ProgressBar;
import ste.Util;
import ste.Util.Pair;
import ste.analyzer.metrics.Metrics;
import ste.analyzer.metrics.MetricsException;
import ste.git.GitRepository;
import ste.model.JavaSourceFile;
import ste.model.Release;
import ste.model.Ticket;

public final class BugAnalyzer {
    private final List<Release> rels;
    private final List<Ticket> tkts;
    private final GitRepository repo;
    private final String projName;

    private List<JavaSourceFile> results;
    
    public BugAnalyzer(String projName, List<Release> rels, List<Ticket> tkts, GitRepository repo) {
        this.rels = rels;
        this.tkts = tkts;
        this.repo = repo;
        this.projName = projName;
        this.results = null;
    }

    public BugAnalyzer(String projName, List<Release> rels, List<Ticket> tkts, GitRepository repo, List<JavaSourceFile> results) {
        this(projName, rels, tkts, repo);
        this.results = results;
    }

    public List<JavaSourceFile> getResults() {
        return results;
    }
    
    public void startAnalysis() 
            throws IOException, MetricsException {

        boolean doMetrics = results == null;

        if(doMetrics) {
            initResults();
        }

        determineBuggyness();

        if(doMetrics) {
            Metrics metrics = new Metrics(projName, results, rels, repo);
            metrics.calculate();
        }
    }
    
    private void initResults() throws IOException {
        results = new ArrayList<>();

        for(int i = 0; i < rels.size(); ++i) {
            Release rel = rels.get(i);
            List<RevCommit> commits = rel.getCommits();
            if(!commits.isEmpty()) {
                populateResults(commits, rel);
            }
        }
    }

    private void populateResults(List<RevCommit> commits, Release rel) throws IOException {
        List<Pair<String, ObjectId>> objs = 
            repo.getObjsForCommit(commits.get(commits.size() - 1));

        for(Pair<String, ObjectId> obj : objs) {
            String relPath = obj.getFirst();
            if(relPath.endsWith(".java") && !relPath.contains("src/test")) {
                JavaSourceFile jsf = JavaSourceFile.build(relPath, rel);
                if(!results.contains(jsf)) {
                    String javaSourceCode = new String(repo.readObjContent(obj.getSecond()));
                    jsf.setLoc(Util.countLines(javaSourceCode));
                    results.add(jsf);
                }
            }
        }
    } 

    private void determineBuggyness() throws IOException {
        JavaSourceFile.resetBuggy(results);

        String pbMsg = String.format("Determining buggyness for project: %s", projName);
        try(ProgressBar pb = Util.buildProgressBar(pbMsg, tkts.size())) {
            for(Ticket tkt : tkts) {
                List<RevCommit> fixCommits = tkt.getCommits();

                List<String> buggyFiles = new ArrayList<>();

                for(RevCommit commit : fixCommits) {
                    List<DiffEntry> diffEntries = repo.getCommitDiffEntries(commit);

                    for(DiffEntry diffEntry : diffEntries) {
                        if(diffEntry.toString().endsWith(".java]")) {
                            addToBuggyFilesByChangeType(buggyFiles, diffEntry);
                        }
                    }
                }

                buggyFilesForVerRange(
                    buggyFiles, 
                    tkt.getInjectedVersionIdx(), 
                    tkt.getFixedVersionIdx());

                pb.step();
            }
        }
    }

    private void buggyFilesForVerRange(List<String> files, int ivIncl, int fvExcl) {
        for(String file : files) {
            for(JavaSourceFile jsf : results) {
                if(
                    jsf.getFilename().equals(file) && 
                    isBuggyRelease(jsf.getRelease(), ivIncl, fvExcl)
                ) {
                    jsf.setBuggy(true);
                }
            }
        }
    }

    private boolean isBuggyRelease(Release rel, int ivIncl, int fvExcl) {
        for(int i = ivIncl; i < fvExcl; ++i) {
            if(rel.getVersion().equals(rels.get(i).getVersion())) {
                return true;
            }
        }

        return false;
    }

    private void addToBuggyFilesByChangeType(List<String> buggyFiles, DiffEntry diffEntry) {
        ChangeType changeType = diffEntry.getChangeType();

        if(changeType == ChangeType.DELETE) {
            buggyFiles.add(diffEntry.getOldPath());
        } else if(changeType == ChangeType.RENAME) {
            buggyFiles.add(diffEntry.getNewPath());
            buggyFiles.add(diffEntry.getOldPath());  
        } else if(changeType == ChangeType.MODIFY || 
                changeType == ChangeType.COPY || 
                changeType == ChangeType.ADD) {
            buggyFiles.add(diffEntry.getNewPath());
        }
    }
}
