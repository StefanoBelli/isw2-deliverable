package ste.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;

public final class GitRepository {
    private final Git git; //potential resource leakage

    public GitRepository(String remote, String localPath) 
            throws InvalidRemoteException, TransportException, GitAPIException, IOException {
        File localPathDir = new File(localPath);
        if(localPathDir.exists() == false) {
            git = Git
                .cloneRepository()
                .setURI(remote)
                .setDirectory(localPathDir)
                .call();
        } else {
            git = Git.open(localPathDir);
        }
    }

    public List<String> getRepoFilePaths() {
        List<String> filePaths = new ArrayList<>();

        try(Repository repo = git.getRepository()) {

            Ref refHead = repo.findRef(Constants.HEAD);
            try(RevWalk walk = new RevWalk(repo)) {

                RevCommit commit = walk.parseCommit(refHead.getObjectId());
                RevTree tree = commit.getTree();
            
                try(TreeWalk treeWalk = new TreeWalk(repo)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
            
                    while (treeWalk.next()) {
                        filePaths.add(treeWalk.getPathString());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return filePaths;
    }

    public List<RevCommit> getFilteredCommits(RevFilter filter) {
        List<RevCommit> commits = new ArrayList<>();

        try(Repository repo = git.getRepository()) {

            try(RevWalk walk = new RevWalk(repo)) {

                Ref refHead = repo.findRef(Constants.HEAD);
                walk.markStart(walk.parseCommit(refHead.getObjectId()));
                walk.setRevFilter(filter);
                for(RevCommit commit : walk) {
                    commits.add(commit);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return commits;
    }
}
