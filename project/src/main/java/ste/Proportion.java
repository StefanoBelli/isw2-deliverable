package ste;

import java.util.List;

import ste.model.Ticket;

public final class Proportion {
    private Proportion() {} 

    private static int proportion(Ticket t) {
        int iv = t.getInjectedVersionIdx();
        int ov = t.getOpeningVersionIdx();
        int fv = t.getFixedVersionIdx();

        return (fv - iv) / (fv - ov);
    }

    private static int increment(List<Ticket> subTickets) {
        int realProportion = 0;
        int effectiveTickets = 0;

        for(Ticket ticket : subTickets) {
            if(!ticket.isCalcInjectedVersion()) {
                realProportion += ticket.getInjectedVersionIdx();
                ++effectiveTickets;
            }
        }

        return Math.round(realProportion / effectiveTickets);
    }

    public static void apply(List<Ticket> allTickets) {
        for(int i = 0; i < allTickets.size(); ++i) {
            Ticket ticket = allTickets.get(i);

            int ov = ticket.getOpeningVersionIdx();
            int fv = ticket.getFixedVersionIdx();

            if(!ticket.isInjectedVersionAvail()) {
                if(ov == fv) {
                    ticket.setInjectedVersionIdx(ov);
                    ticket.setCalcInjectedVersion(true);
                } else {

                }
            }
        }        
    }
}
