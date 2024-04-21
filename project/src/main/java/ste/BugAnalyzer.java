package ste;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

import ste.Util.Pair;
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

    private void initResults() throws IOException {
        results = new ArrayList<>();

        for(Release rel : rels) {
            List<RevCommit> commits = rel.getCommits();
            if(!commits.isEmpty()) {
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
    }

    public void startAnalysis() throws IOException {
        initResults();
    }
}
