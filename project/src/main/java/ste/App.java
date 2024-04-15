package ste;

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.MessageRevFilter;

import ste.git.GitRepository;
import ste.jirarest.JiraProject;
import ste.jirarest.JiraTicket;
import ste.jirarest.util.Http.RequestException;

public class App {
    public static void main(String[] args) throws RequestException, InvalidRemoteException, TransportException, GitAPIException, IOException {
        JiraProject.getProjectByName(args[0]);
        JiraTicket.getAllTicketsByName(args[0]);
        GitRepository repo = new GitRepository("https://github.com/StefanoBelli/Whork", "main", "Whork");
        for(String s : repo.getRepoFilePaths()) {
            System.out.println(s);
        }

        for(RevCommit r : repo.getFilteredCommits(MessageRevFilter.create("SeleniumEditAccount"))) {
            System.out.println(r.getFullMessage());
        }
 for(String s : repo.getRepoFilePaths()) {
            System.out.println(s);
        }

        for(RevCommit r : repo.getFilteredCommits(MessageRevFilter.create("SeleniumEditAccount"))) {
            System.out.println(r.getFullMessage());
        }

        repo.close();
    }
}
