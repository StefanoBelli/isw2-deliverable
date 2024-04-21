package ste;

import java.io.IOException;
import java.util.ArrayList;
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

    public void startAnalysis() throws IOException {
        for(int iRel = 0; iRel < rels.size(); ++iRel) {
            List<JavaSourceFile> relSrcs = new ArrayList<>();
            List<RevCommit> commits = rels.get(iRel).getCommits();
            if(!commits.isEmpty()) {
                System.out.println("==================================COMMIT");
                List<Pair<String, ObjectId>> objs = repo.getObjsForCommit(commits.getLast());

                for(Pair<String, ObjectId> obj : objs) {
                    System.out.println(obj.getFirst());
                }
            }

        }
    }
}
