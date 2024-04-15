package ste.model;

import java.util.Date;

import ste.Util;
import ste.jirarest.JiraProject;

public final class Release {
    private final String version;
    private final Date releaseDate;
    
    private Release(String version, Date releaseDate) {
        this.version = version;
        this.releaseDate = releaseDate;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public String getVersion() {
        return version;
    }

    public static Release fromJiraVersion(JiraProject.Version version) {
        String rd = version.getReleaseDate();
        return new Release(
            version.getName(), 
            rd != null ? Util.dateFromString(rd) : null);
    }
}
