package ste.git;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

import ste.Util.Pair;

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

    public List<Edit> getEditsByDiffEntry(DiffEntry diffEntry) 
            throws IOException {
        
        try (DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            df.setRepository(repo);
            df.setContext(0);
            df.setDiffComparator(RawTextComparator.DEFAULT);
            df.setDetectRenames(true);

            return df.toFileHeader(diffEntry).toEditList();
        }
    }

    public List<DiffEntry> getCommitDiffEntries(RevCommit commit) 
            throws IOException {

        try (DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            df.setRepository(repo);
            df.setContext(0);
            df.setDiffComparator(RawTextComparator.DEFAULT);
            df.setDetectRenames(true);

            if(commit.getParentCount() != 0) {
                RevCommit parent = (RevCommit) commit.getParent(0).getId();
                return df.scan(parent.getTree(), commit.getTree());
            } else {
                try(RevWalk rw = new RevWalk(repo)) {
                    return df.scan(
                        new EmptyTreeIterator(), 
                        new CanonicalTreeParser(null, rw.getObjectReader(), commit.getTree()));
                }
            }
        }
    }

    public byte[] readObjContent(ObjectId obj) 
            throws IOException {
        ObjectLoader objLoader = repo.open(obj);
        if(objLoader.isLarge()) {
            ByteArrayOutputStream outStream = 
                new ByteArrayOutputStream();

            objLoader.copyTo(outStream);
            return outStream.toByteArray();
        }

        return objLoader.getBytes();
    }
    
    public List<Pair<String, ObjectId>> getObjsForCommit(RevCommit commit) 
            throws IOException {
        List<RevCommit> singleCommit = new ArrayList<>();
        singleCommit.add(commit);

        return getObjsForCommits(singleCommit);
    }

    public List<Pair<String, ObjectId>> getObjsForCommits(List<RevCommit> commits) 
            throws IOException {
        List<Pair<String, ObjectId>> objs = new ArrayList<>();
        
        for (RevCommit commit : commits) {
            ObjectId treeId = commit.getTree();
            
            try (TreeWalk treeWalk = new TreeWalk(repo)) {
                treeWalk.reset(treeId);
                treeWalk.setRecursive(true);

                while (treeWalk.next()) {
                    objs.add(new Pair<>(treeWalk.getPathString(), treeWalk.getObjectId(0)));
                }

            }
        }

        return objs;
    }

    public List<RevCommit> getFilteredCommits(RevFilter filter) throws IOException {
        List<RevCommit> commits = new ArrayList<>();

        try(RevWalk walk = new RevWalk(repo)) {
            Ref refHead = repo.findRef(Constants.HEAD);

            if(refHead != null) {
                walk.markStart(walk.parseCommit(refHead.getObjectId()));
                walk.sort(RevSort.REVERSE);
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
