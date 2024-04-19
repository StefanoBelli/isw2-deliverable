package ste;

import java.util.List;

import ste.model.Ticket;

public final class Proportion {
    private Proportion() {} 

    private static int proportion(Ticket t) {
        int iv = t.getInjectedVersionIdx() + 1;
        int ov = t.getOpeningVersionIdx() + 1;
        int fv = t.getFixedVersionIdx() + 1;

        if(fv == ov) {
            return 0;
        }

        return (fv - iv) / (fv - ov);
    }

    private static int increment(List<Ticket> subTickets) {
        int realProportion = 0;
        int effectiveTickets = 0;

        for(Ticket ticket : subTickets) {
            if(!ticket.isArtificialInjectedVersion()) {
                int tmpProportion = proportion(ticket);
                if(tmpProportion >= 1) {
                    realProportion += tmpProportion;
                    ++effectiveTickets;
                } /*else {
                    System.err.println("ERRORED WARNING: PROPORTION neg");
                }*/
            }
        }

        if(effectiveTickets == 0) {
            return 0;
        }

        return (int) Math.floor((float) realProportion / effectiveTickets);
    }

    public static void apply(List<Ticket> allTickets) {
        for(int i = 0; i < allTickets.size(); ++i) {
            Ticket ticket = allTickets.get(i);

            int ov = ticket.getOpeningVersionIdx() + 1;
            int fv = ticket.getFixedVersionIdx() + 1;

            if(!ticket.isInjectedVersionAvail()) {
                int newIv;
                if(ov == fv) {
                    newIv = ov;
                } else {
                    int pIncrement = increment(allTickets.subList(0, i));
                    newIv = fv - ((fv - ov) * pIncrement);
                    newIv = Math.min(newIv, ov);
                    /*if(newIv - 1 < 0) {
                        System.err.println("FIXUP WARNING");
                    }*/
                }

                ticket.setInjectedVersionIdx(newIv - 1);
                ticket.setArtificialInjectedVersion(true);
            }
        }        
    }
}
