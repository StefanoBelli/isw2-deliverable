package ste.model;

import java.util.Date;

import ste.Util;
import ste.jirarest.JiraProject;

public final class Release {
    private final String id;
    private final String version;
    private final Date releaseDate;
    private int releaseIndex;
 
    private Release(String id, String version, Date releaseDate) {
        this.version = version;
        this.releaseDate = releaseDate;
        this.id = id;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public String getVersion() {
        return version;
    }

    public String getId() {
        return id;
    }

    public int getReleaseIndex() {
        return releaseIndex;
    }

    public void setReleaseIndex(int releaseIndex) {
        this.releaseIndex = releaseIndex;
    }

    public static Release fromJiraVersion(JiraProject.Version version) {
        String rd = version.getReleaseDate();
        return new Release(
            version.getId(),
            version.getName(), 
            rd != null ? Util.dateFromString(rd) : null);
    }
}
