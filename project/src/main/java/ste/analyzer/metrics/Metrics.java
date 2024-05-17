package ste.analyzer.metrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.revwalk.RevCommit;

import me.tongfei.progressbar.ProgressBar;
import ste.Util;
import ste.git.GitRepository;
import ste.model.JavaSourceFile;
import ste.model.Release;

public final class Metrics {
    private final GitRepository repo;
    private final List<Release> rels;
    private final List<JavaSourceFile> jsfs;
    private final String projName;

    public Metrics(String projName, List<JavaSourceFile> jsfs, List<Release> rels, GitRepository repo) {
        this.repo = repo;    
        this.rels = rels;
        this.jsfs = jsfs;
        this.projName = projName;
    }

    public void calculate() 
            throws MetricsException, IOException {
        
        String pbMsg = String.format("Calculating %s metrics...", projName);

        try(ProgressBar pb = Util.buildProgressBar(pbMsg, jsfs.size())) {
            for(JavaSourceFile jsf : jsfs) {
                oneshot(jsf);       
                pb.step();
            }   
        }

        removeAllEmptyCommitReleases();
    }

    private void oneshot(JavaSourceFile jsf) 
            throws MetricsException, IOException {

        String filename = jsf.getFilename();
        
        List<RevCommit> relCommits = getJsfCommitsForRelease(jsf);

        List<Integer> locAdded = new ArrayList<>();
        List<Integer> locDeleted = new ArrayList<>();

        Set<String> authorsEmails = new HashSet<>();

        List<Integer> chgSet = new ArrayList<>();

        int numRevs = 0;

        for(RevCommit relCommit : relCommits) {
            List<DiffEntry> diffs = repo.getCommitDiffEntries(relCommit);

            int locAddedPerRevision = 0;
            int locDeletedPerRevision = 0;

            int hasMyFile = 0;

            for(DiffEntry diff : diffs) {
                if(getPathByChangeType(diff).equals(filename)) {
                    hasMyFile = 1;

                    authorsEmails.add(relCommit.getAuthorIdent().getEmailAddress());

                    List<Edit> edits = repo.getEditsByDiffEntry(diff);
                    
                    for(Edit edit : edits) {
                        locAddedPerRevision += edit.getEndB() - edit.getBeginB();
                        locDeletedPerRevision += edit.getEndA() - edit.getBeginA();
                    }
                }
            }

            numRevs += hasMyFile;

            if(hasMyFile == 1) {
                locAdded.add(locAddedPerRevision);
                locDeleted.add(locDeletedPerRevision);
                chgSet.add(calculateChangeSet(diffs));
            }
        }

        List<Integer> churn = Util.IntListWide.eachSub(locAdded, locDeleted);

        aggregateAndSetProps(
            jsf, locAdded, numRevs, 
            authorsEmails.size(), chgSet, churn);
    }

    private void removeAllEmptyCommitReleases() {
        String pbMsg = String.format("Removing empty-commit releases for project %s...", projName);
        try(ProgressBar pb = Util.buildProgressBar(pbMsg, rels.size())) {
            for(Release rel : rels) {
                if(rel.getCommits().isEmpty()) {
                    String noCommitRelVer = rel.getVersion();
                    jsfs.removeIf(jsf -> jsf.getRelease().getVersion().equals(noCommitRelVer));
                }
                pb.step();
            }
        }
    }

    private int calculateChangeSet(List<DiffEntry> diffEntries) {
        Set<String> chgFilenames = new HashSet<>();

        for(DiffEntry diff : diffEntries) {
            String path = getPathByChangeType(diff);
            if(path.endsWith(".java")) {
                chgFilenames.add(path);
            }
        }

        return chgFilenames.size();
    }
    
    private void aggregateAndSetProps(
            JavaSourceFile jsf, 
            List<Integer> locAdd, 
            int numRevs, 
            int numAuthors, 
            List<Integer> chgSet, 
            List<Integer> churn) {

        jsf.setMaxLocAdded(locAdd.isEmpty() ? 0 : Collections.max(locAdd));
        jsf.setAvgLocAdded(Util.IntListWide.avg(locAdd));
        jsf.setLocAdded(Util.IntListWide.sum(locAdd));
        jsf.setMaxChurn(churn.isEmpty() ? 0 : Collections.max(churn));
        jsf.setAvgChurn(Util.IntListWide.avg(churn));
        jsf.setChurn(Util.IntListWide.sum(churn));
        jsf.setNumRev(numRevs);
        jsf.setNumAuthors(numAuthors);
        jsf.setMaxChgSet(chgSet.isEmpty() ? 0 : Collections.max(chgSet));
        jsf.setAvgChgSet(Util.IntListWide.avg(chgSet));
    }

    private List<RevCommit> getJsfCommitsForRelease(JavaSourceFile jsf) 
            throws MetricsException {

        //low-impact loop on performance

        for(Release rel : rels) {
            if(rel.equals(jsf.getRelease())) {
                return rel.getCommits();
            }
        }

        throw new MetricsException("Unable to find matching release");
    }

    private String getPathByChangeType(DiffEntry diffEntry) {
        ChangeType changeType = diffEntry.getChangeType();

        if(changeType == ChangeType.DELETE || changeType == ChangeType.RENAME) {
            return diffEntry.getOldPath();
        } 

        return diffEntry.getNewPath();
    }
}
