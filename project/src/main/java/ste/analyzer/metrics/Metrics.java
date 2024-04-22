package ste.analyzer.metrics;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.revwalk.RevCommit;

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

    public void calculateLocAdded(JavaSourceFile jsf) {

    }

    public void calculateLocTouched(JavaSourceFile jsf) {

    }
    
    public void calculateAvgLocAdded(JavaSourceFile jsf) {

    }
    
    public void calculateMaxLocAdded(JavaSourceFile jsf) {

    }

    public void calculateChurn(JavaSourceFile jsf) {

    }
    
    public void calculateAvgChurn(JavaSourceFile jsf) {

    }
    
    public void calculateMaxChurn(JavaSourceFile jsf) {

    }

    public void calculateNumOfAuthors(JavaSourceFile jsf) 
            throws MetricsException, IOException {

        Release rel = getJsfRelease(jsf);
        List<RevCommit> relCommits = rel.getCommits();
        Set<String> authorsEmails = new HashSet<>(10);
            
        for(RevCommit relCommit : relCommits) {
            List<DiffEntry> diffs = repo.getCommitDiffEntries(relCommit);
            
            for(DiffEntry diff : diffs) {
                if(diff.getNewPath().equals(jsf.getFilename())) {
                    authorsEmails.add(relCommit.getAuthorIdent().getEmailAddress());
                }
            }
        }

        jsf.setNumAuthors(authorsEmails.size());
    }
    
    public void calculateNumOfRevs(JavaSourceFile jsf) {

    }

    private Release getJsfRelease(JavaSourceFile jsf) 
            throws MetricsException {

        for(Release rel : rels) {
            if(rel.equals(jsf.getRelease())) {
                return rel;
            }
        }

        throw new MetricsException("Unable to find matching release");
    }
}
