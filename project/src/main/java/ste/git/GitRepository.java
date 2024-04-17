package ste.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;

public final class GitRepository {
    private final Repository repo;
    private final Git git;

    public GitRepository(String remote, String branch, String localPath) 
            throws GitAPIException, IOException {
        File localPathDir = new File(localPath);
        if(!localPathDir.exists()) {
            git = Git
                .cloneRepository()
                .setURI(remote)
                .setDirectory(localPathDir)
                .call();
        } else {
            git = Git.open(localPathDir);
        }

        git
            .checkout()
            .setCreateBranch(false)
            .setName(branch)
            .call();
        
        repo = git.getRepository();
    }

    public List<String> getRepoFilesByCommit(RevCommit commit) throws IOException {
        List<String> filePaths = new ArrayList<>();
        
        try(RevWalk walk = new RevWalk(repo)) {

            RevTree tree = commit.getTree();
            
            try(TreeWalk treeWalk = new TreeWalk(repo)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
            
                while (treeWalk.next()) {
                    filePaths.add(treeWalk.getPathString());
                }
            }
        }

        return filePaths;
    }

    public List<RevCommit> getFilteredCommits(RevFilter filter) throws IOException {
        List<RevCommit> commits = new ArrayList<>();

        try(RevWalk walk = new RevWalk(repo)) {
            Ref refHead = repo.findRef(Constants.HEAD);

            if(refHead != null) {
                walk.markStart(walk.parseCommit(refHead.getObjectId()));
                walk.setRevFilter(filter);
                
                for(RevCommit commit : walk) {
                    commits.add(commit);
                }
            }
        }

        return commits;
    }

    public void close() {
        //Git#close() calls Repository#close() by itself
        git.close();
    }
}
