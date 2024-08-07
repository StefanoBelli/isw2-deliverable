package ste;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import me.tongfei.progressbar.ProgressBar;

import org.slf4j.Logger;

import ste.csv.CsvWriterException;
import ste.jirarest.JiraProject;
import ste.jirarest.JiraTicket;
import ste.jirarest.http.Http.RequestException;
import ste.model.Release;
import ste.model.Ticket;

public final class Proportion {
    private static final class ColdStart {
        private static Logger log;
        static {
            log = LoggerFactory.getLogger(ColdStart.class.getName());
        }

        private ColdStart() {}

        private static final String[] APACHE_PROJECTS = {
            "AVRO",
            "OPENJPA",
            "SYNCOPE",
            "TAJO",
            "ZOOKEEPER"
        };

        //values may be static... reused shortly after
        private static boolean alreadyComputed = false;
        private static double p;

        private static double median(List<Double> l) {
            Collections.sort(l);
            int sz = l.size();
            
            if (sz % 2 == 0)
                return (l.get(sz / 2) + l.get(sz / 2 - 1)) / 2;
            else
                return l.get(sz / 2);
        }

        private static double averagedProportion(List<Ticket> tkts) {
            double avgP = 0;
            int numP = 0;

            for (Ticket projectTicket : tkts) {
                if (projectTicket.isInjectedVersionAvail()) {
                    double p = proportion(projectTicket);
                    if (p > 0) {
                        numP++;
                        avgP += p;
                    }
                }
            }

            if(numP == 0) {
                return 0;
            }

            return avgP / numP;
        }

        public static double computeProportion() 
                throws RequestException, CsvWriterException, IOException {
            if(alreadyComputed) {
                return p;
            }

            List<Double> avgPs = new ArrayList<>();
            
            String msg = "Fetching projects for ColdStart...";
            try (ProgressBar pb = Util.buildProgressBar(msg, APACHE_PROJECTS.length)) {
                for (String apacheProject : APACHE_PROJECTS) {
                    JiraProject jiraProject = JiraProject.getProjectByName(apacheProject);
                    JiraTicket[] jiraTickets = JiraTicket.getAllTicketsByName(apacheProject);
                    List<Release> releases = Util.sortReleasesByDate(jiraProject, nTotRels -> nTotRels);
                    List<Ticket> tickets = Util.initProjectTickets(releases, jiraTickets);
                    tickets.removeIf(tkt -> !tkt.isInjectedVersionAvail());
                    Util.removeTicketsIfInconsistent(tickets);

                    avgPs.add(averagedProportion(tickets));

                    pb.step();
                }
            }

            p = median(avgPs);

            log.info("done, ColdStart p value ({}) is cached", p);
            alreadyComputed = true;
            return p;
        }
    }

    private Proportion() {} 

    public static final String STRATEGY_NAME = "increment with cold start";
    public static final int NO_COLDSTART_THRESHOLD = 5;

    private static double proportion(Ticket t) {
        int iv = t.getInjectedVersionIdx() + 1;
        int ov = t.getOpeningVersionIdx() + 1;
        int fv = t.getFixedVersionIdx() + 1;

        if(fv == ov) {
            return (double) fv - iv;
        }

        return (double) (fv - iv) / (fv - ov);
    }

    private static double increment(List<Ticket> subTickets) {
        double realProportion = 0;
        int effectiveTickets = 0;

        for(Ticket ticket : subTickets) {
            if(
                ticket.isInjectedVersionAvail() && 
                !ticket.isArtificialInjectedVersion()
            ) {
                ++effectiveTickets;
                realProportion += proportion(ticket);
            }
        }

        //needed to avoid division by zero exceptions
        if(effectiveTickets == 0) {
            return 0;
        }

        return realProportion / effectiveTickets;
    }

    private static Logger log;

    static {
        log = LoggerFactory.getLogger(Proportion.class.getName());
    }

    /*
     * allTickets must be sorted (by resolution date) before they get passed to this method!
     */
    public static void apply(List<Ticket> allTickets) 
            throws RequestException, CsvWriterException, IOException {
        for(int i = 0; i < allTickets.size(); ++i) {
            Ticket ticket = allTickets.get(i);

            int ov = ticket.getOpeningVersionIdx() + 1;
            int fv = ticket.getFixedVersionIdx() + 1;

            if(!ticket.isInjectedVersionAvail()) {
                double pIncrement = 0;
                List<Ticket> usedTicketsForIncrement = filterUsedTickets(allTickets, i);

                if (hasEnoughValidTickets(usedTicketsForIncrement)) {
                    pIncrement = increment(usedTicketsForIncrement);
                } else {
                    pIncrement = ColdStart.computeProportion();
                }

                int newIv = Math.max(
                    1, (int) Math.round(
                        fv == ov ? fv - pIncrement : fv - ((fv - ov) * pIncrement)));

                ticket.setInjectedVersionIdx(newIv - 1);
                ticket.setArtificialInjectedVersion(true);
            }
        }
    }

    private static boolean hasEnoughValidTickets(List<Ticket> tickets) {
        int validTickets = 0;
        
        for(Ticket ticket : tickets) {
            if(ticket.isInjectedVersionAvail() && 
                !ticket.isArtificialInjectedVersion()) {
                ++validTickets;
            }
        }

        boolean mustUseIncrement = validTickets > NO_COLDSTART_THRESHOLD;
        if(mustUseIncrement) {
            log.info("using increment "+
                    " ({}/{} valid tickets)", validTickets, tickets.size());
        } else {
            log.info("using cold start ({}/{} valid tickets)", 
                validTickets, tickets.size());
        }

        return mustUseIncrement;
    }

    private static List<Ticket> filterUsedTickets(List<Ticket> allTickets, int targetTicketIdx) {
        List<Ticket> filtered = new ArrayList<>();

        int tktMatchingVerIdx = allTickets.get(targetTicketIdx).getOpeningVersionIdx();

        for(int i = 0; i < targetTicketIdx; ++i) {
            Ticket currentTicket = allTickets.get(i);
            if(currentTicket.getFixedVersionIdx() < tktMatchingVerIdx) {
                filtered.add(currentTicket);
            }
        }

        return filtered;
    }
}
