package ste.model;

import java.text.SimpleDateFormat;
import java.util.Date;

import ste.Util;
import ste.csv.annotations.CsvColumn;
import ste.csv.annotations.CsvDescriptor;
import ste.jirarest.JiraProject;

@CsvDescriptor
public final class Release {
    private final String id;
    private final String version;
    private final Date releaseDate;
    
    private int index;
 
    private Release(String id, String version, Date releaseDate) {
        this.version = version;
        this.releaseDate = releaseDate;
        this.id = id;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }
    
    @CsvColumn(order = 4, name = "Date")
    public String getFormattedReleaseDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(releaseDate);
    }

    @CsvColumn(order = 3, name = "Version Name")
    public String getVersion() {
        return version;
    }

    @CsvColumn(order = 2, name = "Version ID")
    public String getId() {
        return id;
    }

    @CsvColumn(order = 1, name = "Index")
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public static Release fromJiraVersion(JiraProject.Version version) {
        String rd = version.getReleaseDate();
        return new Release(
            version.getId(),
            version.getName(), 
            rd != null ? Util.dateFromString(rd) : null);
    }
}
