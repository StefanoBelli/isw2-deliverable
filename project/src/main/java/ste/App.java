package ste;

import ste.jirarest.JiraProject;
import ste.jirarest.JiraTicket;
import ste.jirarest.util.Http.RequestException;

public class App {
    public static void main(String[] args) throws RequestException {
        JiraProject.getProjectByName(args[0]);
        JiraTicket.getAllTicketsByName(args[0]);
    }
}
