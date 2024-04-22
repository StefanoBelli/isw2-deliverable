package ste.analyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

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

    private List<JavaSourceFile> results;
    
    public BugAnalyzer(List<Release> rels, List<Ticket> tkts, GitRepository repo) {
        this.rels = rels;
        this.tkts = tkts;
        this.repo = repo;
    }

    public List<JavaSourceFile> getResults() {
        return results;
    }
    
    public void startAnalysis() 
            throws IOException, BugAnalyzerException, MetricsException {

        initResults();
        Metrics m = new Metrics(rels, repo);
        m.calculateNumOfAuthors(results.get(0));
    }
    
    private void initResults() throws IOException, BugAnalyzerException {
        results = new ArrayList<>();

        for(int i = 0; i < rels.size(); ++i) {
            Release rel = rels.get(i);
            List<RevCommit> commits = rel.getCommits();
            if(!commits.isEmpty()) {
                populateResults(commits, rel);
            } else {
                //System.out.println("========================EMPTY");
                List<RevCommit> nextCommits = rels.get(i + 1).getCommits(); 
                if(nextCommits.isEmpty()) {
                    throw new BugAnalyzerException("Empty commit release near another one");
                }
                populateResults(nextCommits, rel);
            }
        }
    }

    private void populateResults(List<RevCommit> commits, Release rel) throws IOException {
        //System.out.println("==================================COMMIT");
        List<Pair<String, ObjectId>> objs = repo.getObjsForCommit(commits.getLast());

        for(Pair<String, ObjectId> obj : objs) {
            //System.out.println(obj.getFirst());
            String relPath = obj.getFirst();
            if(relPath.endsWith(".java")) {
                JavaSourceFile jsf = JavaSourceFile.build(relPath, rel);
                //System.out.println(obj.getFirst());
                if(!results.contains(jsf)) {
                    String javaSourceCode = new String(repo.readObjContent(obj.getSecond()));
                    jsf.setLoc(javaSourceCode.lines().count());
                    results.add(jsf);
                }
            }
        }
    }
}
