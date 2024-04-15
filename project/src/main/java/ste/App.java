package ste;

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import ste.git.GitRepository;
import ste.jirarest.JiraProject;
import ste.jirarest.JiraTicket;
import ste.jirarest.util.Http.RequestException;

public class App {
    public static void main(String[] args) throws RequestException, InvalidRemoteException, TransportException, GitAPIException, IOException {
        JiraProject.getProjectByName(args[0]);
        JiraTicket.getAllTicketsByName(args[0]);
        GitRepository repo = new GitRepository("https://github.com/StefanoBelli/Whork.git", "Whork");
    }
}
