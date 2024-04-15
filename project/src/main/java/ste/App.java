package ste;

import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import ste.git.GitRepository;
import ste.jirarest.JiraProject;
import ste.jirarest.JiraTicket;
import ste.jirarest.util.Http.RequestException;

public final class App {
    private static final Logger logger;
    
    private static final String STORM = "STORM";
    private static final String STORM_GITHUB = "https://github.com/apache/storm";
    private static final String STORM_LOCAL = "storm";
    private static final String STORM_BRANCH = "master";
    private static final String BOOKKEEPER = "BOOKKEEPER";
    private static final String BOOKKEEPER_GITHUB = "https://github.com/apache/bookkeeper";
    private static final String BOOKKEEPER_LOCAL = "bookkeeper";
    private static final String BOOKKEEPER_BRANCH = "master";

    static {
        logger = Logger.getLogger("App");
    }

    private static String getCloneDir(String rel) {
        return String.format("%s/%s", System.getProperty("user.home"), rel);
    }

    private static String getCloneInfoLine(String proj, String github, String branch, String local) {
        return String.format("(git) project %s: url=%s (branch=%s, local=%s)", 
            proj, github, branch, local);
    }

    public static void main(String[] args) 
            throws RequestException, InvalidRemoteException, 
                    TransportException, GitAPIException, IOException {

        logger.info("Setup phase...");

        logger.info(String.format("Fetching projects for %s and %s...", STORM, BOOKKEEPER));

        JiraProject jiraStormProject = JiraProject.getProjectByName(STORM);
        JiraProject jiraBookKeeperProject = JiraProject.getProjectByName(BOOKKEEPER);

        logger.info(String.format("Fetching tickets for %s and %s...", STORM, BOOKKEEPER));

        JiraTicket[] jiraStormTickets = JiraTicket.getAllTicketsByName(STORM);
        JiraTicket[] jiraBookKeeperTickets = JiraTicket.getAllTicketsByName(BOOKKEEPER);

        String stormLocal = getCloneDir(STORM_LOCAL);
        String bookKeeperLocal = getCloneDir(BOOKKEEPER_LOCAL);

        logger.info("Checking git repositories...");

        logger.info(getCloneInfoLine(STORM, STORM_GITHUB, STORM_BRANCH, stormLocal));
        GitRepository stormGitRepo = new GitRepository(STORM_GITHUB, STORM_BRANCH, stormLocal);

        logger.info(getCloneInfoLine(BOOKKEEPER, BOOKKEEPER_GITHUB, BOOKKEEPER_BRANCH, bookKeeperLocal));
        GitRepository bookKeeperGitRepo = new GitRepository(BOOKKEEPER_GITHUB, BOOKKEEPER_BRANCH, bookKeeperLocal);

        logger.info("Setup phase done");
    }
}

