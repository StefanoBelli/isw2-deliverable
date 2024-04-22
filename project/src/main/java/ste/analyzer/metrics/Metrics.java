package ste.analyzer.metrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.revwalk.RevCommit;

import ste.Util;
import ste.git.GitRepository;
import ste.model.JavaSourceFile;
import ste.model.Release;

public final class Metrics {
    private final GitRepository repo;
    private final List<Release> rels;

    public Metrics(List<Release> rels, GitRepository repo) {
        this.repo = repo;    
        this.rels = rels;
    }

    public void oneshotCalculate(JavaSourceFile jsf) 
            throws MetricsException, IOException {
        
        List<RevCommit> relCommits = getJsfCommitsForRelease(jsf);

        List<Integer> locAdded = new ArrayList<>();
        List<Integer> locDeleted = new ArrayList<>();

        Set<String> authorsEmails = new HashSet<>(10);

        int numRevs = 0;

        for(RevCommit relCommit : relCommits) {
            List<DiffEntry> diffs = repo.getCommitDiffEntries(relCommit);

            int locAddedPerRevision = 0;
            int locDeletedPerRevision = 0;

            int hasMyFile = 0;

            for(DiffEntry diff : diffs) {
                
                if(diff.getNewPath().equals(jsf.getFilename())) {
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
            }
        }

        List<Integer> churn = Util.IntListWide.eachSub(locAdded, locDeleted);

        jsf.setMaxLocAdded(Collections.max(locAdded));
        jsf.setAvgLocAdded(Util.IntListWide.avg(locAdded));
        jsf.setLocAdded(Util.IntListWide.sum(locAdded));
        jsf.setMaxChurn(Collections.max(churn));
        jsf.setAvgChurn(Util.IntListWide.avg(churn));
        jsf.setChurn(Util.IntListWide.sum(churn));
        jsf.setNumRev(numRevs);
        jsf.setNumAuthors(authorsEmails.size());
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
}
