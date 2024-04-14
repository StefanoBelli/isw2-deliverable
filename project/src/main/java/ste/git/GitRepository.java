package ste.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

public final class GitRepository {
    private final Git git; //resource leakage

    public GitRepository(String remote, String localPath) 
            throws InvalidRemoteException, TransportException, GitAPIException, IOException {
        StringBuilder gitLocalPathBuilder = new StringBuilder();
        gitLocalPathBuilder.append(localPath).append("/.git");
        String gitLocalPath = gitLocalPathBuilder.toString();

        File gitLocalPathDir = new File(gitLocalPath);
        if(gitLocalPathDir.exists() == false) {
            git = Git
                .cloneRepository()
                .setURI(remote)
                .setDirectory(gitLocalPathDir)
                .call();
        } else {
            git = Git.open(gitLocalPathDir);
        }
    }

    public List<String> getRepoFilePaths() {
        List<String> filePaths = new ArrayList<>();

        try(Repository repo = git.getRepository()) {
            Ref head = repo.findRef("HEAD");
            try(RevWalk walk = new RevWalk(repo)) {
            
                RevCommit commit = walk.parseCommit(head.getObjectId());
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
}
