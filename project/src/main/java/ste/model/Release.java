package ste.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;

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
    private List<RevCommit> commits;
 
    private Release(String id, String version, Date releaseDate) {
        this.version = version;
        this.releaseDate = releaseDate;
        this.id = id;
    }

    //non-deepcopy-enabled constructor
    public Release(Release old) {
        this.id = String.valueOf(old.id);
        this.version = String.valueOf(old.version);
        this.releaseDate = new Date(old.releaseDate.getTime());
        this.commits = old.commits; //copyrefs
        this.index = old.index;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }
    
    @CsvColumn(order = 1, name = "Index")
    public int getIndex() {
        return index;
    }

    @CsvColumn(order = 2, name = "Version ID")
    public String getId() {
        return id;
    }

    @CsvColumn(order = 3, name = "Version Name")
    public String getVersion() {
        return version;
    }

    @CsvColumn(order = 4, name = "Date")
    public String getFormattedReleaseDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(releaseDate);
    }

    public List<RevCommit> getCommits() {
        return commits;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setCommits(List<RevCommit> commits) {
        this.commits = commits;
    }

    public static Release fromJiraVersion(JiraProject.Version version) {
        String rd = version.getReleaseDate();
        return new Release(
            version.getId(),
            version.getName(), 
            rd != null ? Util.dateFromString(rd) : null);
    }

    public static List<Release> copyReleases(List<Release> old) {
        List<Release> newRels = new ArrayList<>();
        for(Release rel : old) {
            newRels.add(new Release(rel));
        }
        
        return newRels;
    }
}
