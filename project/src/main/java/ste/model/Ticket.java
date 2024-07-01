package ste.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;

public final class Ticket {
    private final String key;
    private boolean injectedVersionAvail;
    private boolean artificialInjectedVersion;
    private int injectedVersionIdx;
    private final int openingVersionIdx;
    private final int fixedVersionIdx;
    private List<RevCommit> commits;

    public Ticket(String key, int openingVersionIdx, int fixedVersionIdx) {
        this.key = key;
        this.openingVersionIdx = openingVersionIdx;
        this.fixedVersionIdx = fixedVersionIdx;
        this.injectedVersionAvail = false;
        this.artificialInjectedVersion = false;
    }

    //non-deepcopy-enabled constructor
    private Ticket(Ticket old) {
        this.key = String.valueOf(old.key);
        this.injectedVersionAvail = old.injectedVersionAvail;
        this.artificialInjectedVersion = old.artificialInjectedVersion;
        this.injectedVersionIdx = old.injectedVersionIdx;
        this.openingVersionIdx = old.openingVersionIdx;
        this.fixedVersionIdx = old.fixedVersionIdx;
        this.commits = old.commits;
    }

    public int getOpeningVersionIdx() {
        return this.openingVersionIdx;
    }

    public int getInjectedVersionIdx() {
        return this.injectedVersionIdx;
    }

    public boolean isInjectedVersionAvail() {
        return this.injectedVersionAvail;
    }

    public boolean isArtificialInjectedVersion() {
        return artificialInjectedVersion;
    }

    public List<RevCommit> getCommits() {
        return commits;
    }

    public int getFixedVersionIdx() {
        return fixedVersionIdx;
    }

    public String getKey() {
        return key;
    }

    public void setCommits(List<RevCommit> commits) {
        this.commits = commits;
    }

    public void setInjectedVersionIdx(int injectedVersionIdx) {
        this.injectedVersionIdx = injectedVersionIdx;
        this.injectedVersionAvail = true;
    }

    public void setArtificialInjectedVersion(boolean calcInjectedVersion) {
        this.artificialInjectedVersion = calcInjectedVersion;
    }

    public void unsetInjectedVersionAvail() {
        this.injectedVersionAvail = false;
        this.artificialInjectedVersion = false;
    }

    public static void ensureResetArtificialIv(List<Ticket> tkts) {
        for(Ticket tkt : tkts) {
            if(tkt.isInjectedVersionAvail() && tkt.isArtificialInjectedVersion()) {
                tkt.artificialInjectedVersion = false;
                tkt.injectedVersionAvail = false;
            }
        }
    }

    public static List<Ticket> copyTickets(List<Ticket> old) {
        List<Ticket> newTkt = new ArrayList<>();
        for(Ticket t : old) {
            newTkt.add(new Ticket(t));
        }

        return newTkt;
    }
}