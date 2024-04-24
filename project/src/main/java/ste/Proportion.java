package ste;

import java.util.List;

import ste.model.Ticket;

public final class Proportion {
    private Proportion() {} 

    public static final String STRATEGY_NAME = "increment";

    private static int proportion(Ticket t, int numRels) {
        int iv = t.getInjectedVersionIdx() + 1;
        int ov = t.getOpeningVersionIdx() + 1;
        int fv = t.getFixedVersionIdx() + 1;

        if(fv == ov) {
            return 0;
        }

        if(iv >= numRels || ov >= numRels || fv >= numRels) {
            return 0;
        }

        return (int) Math.floor((float) (fv - iv) / (fv - ov));
    }

    private static int increment(List<Ticket> subTickets, int numRels) {
        int realProportion = 0;
        int effectiveTickets = 0;

        for(Ticket ticket : subTickets) {
            if(!ticket.isArtificialInjectedVersion()) {
                int tmpProportion = proportion(ticket, numRels);
                if(tmpProportion >= 1) {
                    realProportion += tmpProportion;
                    ++effectiveTickets;
                }
            }
        }

        if(effectiveTickets == 0) {
            return 0;
        }

        return (int) Math.floor((float) realProportion / effectiveTickets);
    }

    public static void apply(List<Ticket> allTickets, int numRels) {
        for(int i = 0; i < allTickets.size(); ++i) {
            Ticket ticket = allTickets.get(i);

            int ov = ticket.getOpeningVersionIdx() + 1;
            int fv = ticket.getFixedVersionIdx() + 1;

            if(!ticket.isInjectedVersionAvail()) {
                int newIv;
                if(ov == fv) {
                    newIv = ov;
                } else {
                    int pIncrement = increment(allTickets.subList(0, i), numRels);
                    newIv = fv - ((fv - ov) * pIncrement);
                }

                ticket.setInjectedVersionIdx(newIv - 1);
                ticket.setArtificialInjectedVersion(true);
            }
        }        
    }
}
